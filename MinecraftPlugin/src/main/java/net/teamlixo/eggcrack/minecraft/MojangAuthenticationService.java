package net.teamlixo.eggcrack.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.account.output.AttemptedAccount;
import net.teamlixo.eggcrack.authentication.AuthenticationException;
import net.teamlixo.eggcrack.authentication.configuration.ServiceConfiguration;
import net.teamlixo.eggcrack.credential.password.PasswordAuthenticationService;
import net.teamlixo.eggcrack.credential.password.PasswordCredential;
import net.teamlixo.eggcrack.timer.IntervalTimer;
import net.teamlixo.eggcrack.timer.Timer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;

/**
 * Lowest-level functionality of EggCrack 2.0; all requests are created and managed here.
 *
 * Deprecated because of long timeouts.
 */
@Deprecated
public class MojangAuthenticationService extends PasswordAuthenticationService {
    private static final int MINIMUM_PASSWORD_LEGTH = 6; //Mojang specification
    private static final InetAddress LOCAL_ADDRESS = InetAddress.getLoopbackAddress();

    /**
     * Authentication factory to use when creating new Mojang authentication instances.
     */
    private AuthenticationFactory authenticationFactory;

    /**
     * Interval each proxy may send requests at, in seconds.
     */
    private float interval = 0.05F;
    private final Map<InetAddress, Timer> intervalMap = new HashMap<InetAddress, Timer>();
    private final List<Proxy> unavailableProxies = new ArrayList<Proxy>();

    private final ServiceConfiguration.Option<Boolean> checkCape;
    private final ServiceConfiguration.Option<Boolean> disbleProxies;

    public MojangAuthenticationService(String name, AuthenticationFactory authenticationFactory) {
        super(name);
        this.authenticationFactory = authenticationFactory;

        this.checkCape = getConfiguration().register(
                new ServiceConfiguration.Option<Boolean>("Check for cape", Boolean.FALSE)
        );

        this.disbleProxies = getConfiguration().register(
                new ServiceConfiguration.Option<Boolean>("Proxy timeout", Boolean.TRUE)
        );
    }

    @Override
    protected AuthenticatedAccount authenticate(Account account, String password, Proxy proxy) throws AuthenticationException {
        if (!(account instanceof AttemptedAccount))
            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INVALID_ACCOUNT, "account not properly instantiated");

        return authenticateMinecraft(account.getUsername(), password, proxy);
    }

    private AuthenticatedAccount authenticateMinecraft(String username, String password, Proxy proxy) throws AuthenticationException {
        //Ensure username and password are not null.
        if (username == null)
            return null;

        if (password == null)
            return null;

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
        EggCrack.LOGGER.finer("[Authentication] " + username + ": using proxy [type=" + proxy.type().name() + ",address=" + proxyAddress + "].");

        //Step 3: Attempt to authenticate the user using the username and password.

        UserAuthentication userAuthentication = authenticationFactory.createUserAuthentication(proxy);
        userAuthentication.setUsername(username);
        userAuthentication.setPassword(password);

        try {
            EggCrack.LOGGER.fine("[Authentication] Trying [username=" + username + ", password=" + password + "].");

            userAuthentication.logIn();
            if (timer != null) timer.next();

            GameProfile[] profiles = userAuthentication.getAvailableProfiles();
            if (profiles.length <= 0) //Account has no profiles, we logged in but cannot use it.
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.NO_PROFILES, "Account has no profiles");
            GameProfile profile = userAuthentication.getSelectedProfile();
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
                    profile.getId(), //Account UUID in-game
                    new PasswordCredential(password) //Account password.
            );
        } catch (com.mojang.authlib.exceptions.AuthenticationException e) {
            if (timer != null) timer.next();

            String errorMessage = e.getMessage();

            EggCrack.LOGGER.finer("[Authentication] Attempt [username=" + username + ", password=" + password + "] failed: " + e.getMessage());

            if (errorMessage.equals("Invalid credentials. Invalid username or password.")) {
                //Username or password is not correct.
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INCORRECT_CREDENTIAL, errorMessage);
            } else if (errorMessage.equals("Invalid credentials.")) {
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.REJECTED, errorMessage);
            } else if (errorMessage.equals("Cannot contact authentication server")) {
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.TIMEOUT, errorMessage);
            } else if (errorMessage.equals("Invalid credentials. Account migrated, use e-mail as username.") ||
                    errorMessage.equals("Invalid credentials. Account migrated, use email as username.") ||
                    errorMessage.equals("Invalid username")) {
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INVALID_ACCOUNT, errorMessage);
            } else {
                EggCrack.LOGGER.warning("[Authentication] Unexpected response: " + e.getMessage());
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.REJECTED, errorMessage);
            }
        } catch (NoSuchElementException exception) {
            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.BAD_PROXY, "Bad proxy");
        }
    }

    public int unavailableProxies() {
        return unavailableProxies.size();
    }
}
