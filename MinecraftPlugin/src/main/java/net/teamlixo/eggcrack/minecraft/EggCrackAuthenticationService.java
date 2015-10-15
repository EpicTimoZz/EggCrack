package net.teamlixo.eggcrack.minecraft;

import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.authentication.AuthenticationException;
import net.teamlixo.eggcrack.authentication.configuration.ServiceConfiguration;
import net.teamlixo.eggcrack.credential.password.PasswordAuthenticationService;
import net.teamlixo.eggcrack.credential.password.PasswordCredential;
import net.teamlixo.eggcrack.timer.IntervalTimer;
import net.teamlixo.eggcrack.timer.Timer;
import org.mcupdater.Yggdrasil.AuthManager;
import org.mcupdater.Yggdrasil.ErrorResponse;
import org.mcupdater.Yggdrasil.Profile;
import org.mcupdater.Yggdrasil.SessionResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;

public class EggCrackAuthenticationService extends PasswordAuthenticationService {
    private static final int MINIMUM_PASSWORD_LEGTH = 6; //Mojang specification
    private static final InetAddress LOCAL_ADDRESS = InetAddress.getLoopbackAddress();

    private AuthManager authManager = new AuthManager();

    private final ServiceConfiguration.Option<Boolean> checkCape;
    private final ServiceConfiguration.Option<Boolean> disbleProxies;

    /**
     * Interval each proxy may send requests at, in seconds.
     */
    private float interval = 0.05F;
    private final Map<InetAddress, Timer> intervalMap = new HashMap<InetAddress, Timer>();
    private final List<Proxy> unavailableProxies = new ArrayList<Proxy>();

    public EggCrackAuthenticationService(String name) {
        super(name);

        this.checkCape = getConfiguration().register(
                new ServiceConfiguration.Option<Boolean>("Check for cape", Boolean.FALSE)
        );

        this.disbleProxies = getConfiguration().register(
                new ServiceConfiguration.Option<Boolean>("Proxy timeout", Boolean.TRUE)
        );
    }

    @Override
    protected AuthenticatedAccount authenticate(Account account, String password, Proxy proxy)
            throws AuthenticationException {
        String username = account.getUsername();

        //Step 1: Check username and password for possible corruptions.
        username = username.trim().replace("\n", "").replace("\r", "");

        if (password.equalsIgnoreCase("%user"))
            password = username;

        //Little bit of sanitizing.
        password = password.replace("\n", "").replace("\r", "").trim();

        //Make sure the password isn't too short.
        if (password.length() < MINIMUM_PASSWORD_LEGTH)
            return null;

        //Step 2: Check proxy for rate-limiting.
        InetAddress proxyAddress = proxy.type() == Proxy.Type.DIRECT || proxy.address() == null ?
                LOCAL_ADDRESS :
                ((InetSocketAddress)proxy.address()).getAddress();

        Timer timer = null;
        synchronized (intervalMap) { //Not sure if HashMaps are thread-safe.
            if (disbleProxies.getValue()) {
                if (!intervalMap.containsKey(proxyAddress))
                    intervalMap.put(proxyAddress, new IntervalTimer(interval, IntervalTimer.RateWindow.SECOND));
                timer = intervalMap.get(proxyAddress);

                if (proxy.type() != Proxy.Type.DIRECT && !timer.isReady()) {
                    if (!unavailableProxies.contains(proxy)) unavailableProxies.add(proxy);
                    throw new AuthenticationException(AuthenticationException.AuthenticationFailure.BAD_PROXY, "Bad proxy");
                } else unavailableProxies.remove(proxy);
            }
        }

        String response = null;

        try {
            response = authManager.authenticate(account.getUsername(), password, "", proxy);
            if (timer != null) timer.next();
        } catch (IOException e) {
            throw new net.teamlixo.eggcrack.authentication.AuthenticationException(
                    AuthenticationException.AuthenticationFailure.REJECTED,
                    e.getMessage()
            );
        }

        try {
            ErrorResponse errorResponse = (ErrorResponse) this.authManager.getGson().fromJson(response, ErrorResponse.class);
            String errorMessage = errorResponse.getErrorMessage();

            if (errorMessage == null || errorMessage.length() == 0) throw new JsonSyntaxException("Invalid error.");
            else if (errorMessage.equals("Invalid credentials. Invalid username or password.")) {
                //Username or password is not correct.
                throw new net.teamlixo.eggcrack.authentication.AuthenticationException(
                        net.teamlixo.eggcrack.authentication.AuthenticationException.AuthenticationFailure.INCORRECT_CREDENTIAL,
                        errorMessage
                );
            } else if (errorMessage.equals("Invalid credentials.")) {
                throw new net.teamlixo.eggcrack.authentication.AuthenticationException(
                        net.teamlixo.eggcrack.authentication.AuthenticationException.AuthenticationFailure.REJECTED,
                        errorMessage
                );
            } else if (errorMessage.equals("Cannot contact authentication server")) {
                throw new net.teamlixo.eggcrack.authentication.AuthenticationException(
                        net.teamlixo.eggcrack.authentication.AuthenticationException.AuthenticationFailure.TIMEOUT,
                        errorMessage
                );
            } else if (errorMessage.equals("Invalid credentials. Account migrated, use e-mail as username.") ||
                    errorMessage.equals("Invalid credentials. Account migrated, use email as username.") ||
                    errorMessage.equals("Invalid username")) {
                throw new net.teamlixo.eggcrack.authentication.AuthenticationException(
                        net.teamlixo.eggcrack.authentication.AuthenticationException.AuthenticationFailure.INVALID_ACCOUNT,
                        errorMessage
                );
            } else {
                EggCrack.LOGGER.warning("[Authentication] Unexpected response: " + errorMessage);
                throw new net.teamlixo.eggcrack.authentication.AuthenticationException(
                        net.teamlixo.eggcrack.authentication.AuthenticationException.AuthenticationFailure.REJECTED,
                        errorMessage
                );
            }

        } catch (JsonSyntaxException ignored) {}

        try
        {
            SessionResponse sessionResponse = (SessionResponse) this.authManager.getGson().fromJson(response, SessionResponse.class);
            if (sessionResponse.getAvailableProfiles() != null && sessionResponse.getAvailableProfiles().length > 0) {
                Profile[] profiles = sessionResponse.getAvailableProfiles();
                if (profiles.length <= 0) //Account has no profiles, we logged in but cannot use it.
                    throw new AuthenticationException(AuthenticationException.AuthenticationFailure.NO_PROFILES, "Account has no profiles");
                Profile profile = sessionResponse.getSelectedProfile();
                if (profile == null) profile = profiles[0]; //Select first profile on the account.

                // See: http://www.mpgh.net/forum/showthread.php?t=1036349
                if (checkCape.getValue()) {
                    for (int i = 0; i < 10; i ++) { // Try 10 times to grab a cape.
                        try {
                            String url = String.format(
                                    "http://s3.amazonaws.com/MinecraftCloaks/%s.png",
                                    URLEncoder.encode(profile.getName(), "UTF8")
                            );

                            HttpURLConnection urlConnection = (HttpURLConnection) URI.create(url).toURL().openConnection(proxy);
                            if (urlConnection.getResponseCode() / 100 != 2)
                                throw new AuthenticationException(
                                        AuthenticationException.AuthenticationFailure.INVALID_ACCOUNT, "Missing cape"
                                );

                            break;
                        } catch (UnsupportedEncodingException e) {
                            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INVALID_ACCOUNT, e.getMessage());
                        } catch (IOException e) {
                            // Retry.
                        }
                    }
                }

                return new AuthenticatedAccount(
                        username, //Account username
                        profile.getName(), //Account display name in-game
                        UUID.fromString(profile.getId()), //Account UUID in-game
                        new PasswordCredential(password) //Account password.
                );
            } else throw new AuthenticationException(
                    AuthenticationException.AuthenticationFailure.NO_PROFILES,
                    "Account has no profiles"
            );
        }
        catch (Exception ex)
        {
            ex.printStackTrace();

            throw new net.teamlixo.eggcrack.authentication.AuthenticationException(
                    net.teamlixo.eggcrack.authentication.AuthenticationException.AuthenticationFailure.REJECTED,
                    ex.getMessage()
            );
        }
    }

    @Override
    public int unavailableProxies() {
        return unavailableProxies.size();
    }
}
