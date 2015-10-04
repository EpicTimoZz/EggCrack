package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.credential.Credentials;
import net.teamlixo.eggcrack.session.Session;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AccountListener;
import net.teamlixo.eggcrack.credential.Credential;
import net.teamlixo.eggcrack.session.Tracker;

import java.net.Proxy;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Helper class designed to provide asynchronous authentication functionality to EggCrack. Calls AuthenticationCallback
 * with completed or failed responses. Handles one account, but narrows down to the AuthenticationService class, which
 * should be both thread-safe and support many separate requesting accounts asynchronously.
 */
public class RunnableAuthenticator implements Runnable {
    private AuthenticationService authenticationService;
    private Account account;
    private Iterator<Credential> credentialIterator;
    private Iterator<Proxy> proxyIterator;
    private AuthenticationCallback authenticationCallback;
    private Tracker tracker;

    public RunnableAuthenticator(AuthenticationService authenticationService,
                                 Account account,
                                 Tracker tracker,
                                 Iterator<Credential> credentialIterator,
                                 Iterator<Proxy> proxyIterator,
                                 AuthenticationCallback authenticationCallback) {
        this.authenticationService = authenticationService;
        this.account = account;
        this.credentialIterator = credentialIterator;
        this.proxyIterator = proxyIterator;
        this.authenticationCallback = authenticationCallback;
        this.tracker = tracker;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Authenticator-" + account.getUsername());

        Credential credential = account.getUncheckedPassword() != null ?
                Credentials.createPassword(account.getUncheckedPassword()) : null;

        AccountListener accountListener = account.getListener();
        if (accountListener != null) accountListener.onAccountStarted(account);

        while (proxyIterator.hasNext()) {
            try {
                if (credential == null) {
                    if (accountListener != null) accountListener.onAccountTried(account, credential);
                    credential = credentialIterator.next();
                    synchronized (tracker) {
                        tracker.setAttempts(tracker.getAttempts() + 1);
                    }
                }

                EggCrack.LOGGER.finest("[Account: " + account.getUsername() +
                        "] Sending authentication request [password=" + credential.toString() + "]...");

                if (accountListener != null) accountListener.onAccountAttempting(account, credential);

                try {
                    AuthenticatedAccount authenticatedAccount =
                            authenticationService.authenticate(account, credential, proxyIterator.next());
                    if (authenticatedAccount != null) {
                        authenticationCallback.onAuthenticationCompleted(authenticatedAccount);
                        if (accountListener != null) accountListener.onAccountCompleted(account, credential);
                        return;
                    } else {
                        if (accountListener != null) accountListener.onAccountTried(account, credential);
                        credential = credentialIterator.next();
                        synchronized (tracker) {
                            tracker.setAttempts(tracker.getAttempts() + 1);
                        }
                    }
                } catch (AuthenticationException exception) {
                    if (exception.getFailure().hasRequested())
                        tracker.setRequests(tracker.getRequests() + 1);

                    if (exception.getFailure().getAction() == AuthenticationException.AuthenticationAction.STOP) {
                        EggCrack.LOGGER.warning("Stopping session for " + account.getUsername() + ": " + exception.getMessage());
                        break;
                    } else if (exception.getFailure().getAction() == AuthenticationException.AuthenticationAction.NEXT_CREDENTIALS) {
                        if (accountListener != null) accountListener.onAccountTried(account, credential);
                        synchronized (tracker) {
                            tracker.setAttempts(tracker.getAttempts() + 1);
                        }
                        if (account.getUncheckedPassword() == null)
                            credential = credentialIterator.next();
                        else
                            break; // Checker only tries one password.
                    }
                }
            } catch (NoSuchElementException exception) {
                break;
            } catch (Throwable ex) {
                //Just ignore it.
            }
        }

        if (accountListener != null) accountListener.onAccountFailed(account );
        authenticationCallback.onAuthenticationFailed(account);
    }
}
