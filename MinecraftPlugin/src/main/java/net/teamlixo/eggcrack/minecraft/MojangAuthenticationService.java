package net.teamlixo.eggcrack.minecraft;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.account.output.AttemptedAccount;
import net.teamlixo.eggcrack.authentication.AuthenticationException;
import net.teamlixo.eggcrack.credential.password.PasswordAuthenticationService;
import net.teamlixo.eggcrack.credential.password.PasswordCredential;
import net.teamlixo.eggcrack.timer.IntervalTimer;
import net.teamlixo.eggcrack.timer.Timer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Lowest-level functionality of EggCrack; all requests are created and managed here.
 */
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

    public MojangAuthenticationService() {
        super("Yggdrasil Authentication");
        this.authenticationFactory = new MojangAuthenticationFactory(Agent.MINECRAFT);
    }

    public MojangAuthenticationService(String name, AuthenticationFactory authenticationFactory) {
        super(name);
        this.authenticationFactory = authenticationFactory;
    }

    @Override
    protected AuthenticatedAccount authenticate(Account account, String password, Proxy proxy) throws AuthenticationException {
        if (!(account instanceof AttemptedAccount))
            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INVALID_ACCOUNT);

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
            if (!intervalMap.containsKey(proxyAddress))
                intervalMap.put(proxyAddress, new IntervalTimer(interval, IntervalTimer.RateWindow.SECOND));
            timer = intervalMap.get(proxyAddress);
        }

        if (proxy.type() != Proxy.Type.DIRECT && !timer.isReady())
            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.BAD_PROXY);

        EggCrack.LOGGER.finer("[Authentication] " + username + ": using proxy [type=" + proxy.type().name() + ",address=" + proxyAddress + "].");

        //Step 3: Attempt to authenticate the user using the username and password.

        UserAuthentication userAuthentication = authenticationFactory.createUserAuthentication(proxy);
        userAuthentication.setUsername(username);
        userAuthentication.setPassword(password);

        try {
            EggCrack.LOGGER.fine("[Authentication] Trying [username=" + username + ", password=" + password + "].");

            userAuthentication.logIn();
            timer.next();

            GameProfile[] profiles = userAuthentication.getAvailableProfiles();
            if (profiles.length <= 0) //Account has no profiles, we logged in but cannot use it.
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.NO_PROFILES);
            GameProfile profile = userAuthentication.getSelectedProfile();
            if (profile == null) profile = profiles[0]; //Select first profile on the account.

            return new AuthenticatedAccount(
                    username, //Account username
                    profile.getName(), //Account display name in-game
                    profile.getId(), //Account UUID in-game
                    new PasswordCredential(password) //Account password.
            );
        } catch (com.mojang.authlib.exceptions.AuthenticationException e) {
            timer.next();

            String errorMessage = e.getMessage();

            EggCrack.LOGGER.finer("[Authentication] Attempt [username=" + username + ", password=" + password + "] failed: " + e.getMessage());

            if (errorMessage.equals("Invalid credentials. Invalid username or password.")) {
                //Username or password is not correct.
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INCORRECT_CREDENTIAL);
            } else if (errorMessage.equals("Invalid credentials.")) {
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.REJECTED);
            } else if (errorMessage.equals("Cannot contact authentication server")) {
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.TIMEOUT);
            } else if (errorMessage.equals("Invalid credentials. Account migrated, use e-mail as username.")) {
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INVALID_ACCOUNT);
            } else {
                EggCrack.LOGGER.warning("[Authentication] Unexpected response: " + e.getMessage());
                throw new AuthenticationException(AuthenticationException.AuthenticationFailure.REJECTED);
            }
        } catch (NoSuchElementException exception) {
            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.BAD_PROXY);
        }
    }
}
