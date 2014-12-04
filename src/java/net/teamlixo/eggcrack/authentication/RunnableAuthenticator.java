package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AccountListener;
import net.teamlixo.eggcrack.credential.Credential;

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

    public RunnableAuthenticator(AuthenticationService authenticationService,
                                 Account account,
                                 Iterator<Credential> credentialIterator,
                                 Iterator<Proxy> proxyIterator,
                                 AuthenticationCallback authenticationCallback) {
        this.authenticationService = authenticationService;
        this.account = account;
        this.credentialIterator = credentialIterator;
        this.proxyIterator = proxyIterator;
        this.authenticationCallback = authenticationCallback;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Authenticator-" + account.getUsername());

        Credential credential = credentialIterator.next();
        AccountListener accountListener = account.getListener();
        if (accountListener != null) accountListener.onAccountStarted();

        while (proxyIterator.hasNext()) {
            try {
                if (credential == null)
                    credential = credentialIterator.next();

                EggCrack.LOGGER.finest("[Account: " + account.getUsername() +
                        "] Sending authentication request [password=" + credential.toString() + "]...");

                if (accountListener != null) accountListener.onAccountAttempting(credential);

                try {
                    if (authenticationService.authenticate(account, credential, proxyIterator.next())) {
                        authenticationCallback.onAuthenticationCompleted(account, credential);
                        if (accountListener != null) accountListener.onAccountCompleted(credential);
                        return;
                    } else {
                        credential = credentialIterator.next();
                    }
                } catch (AuthenticationException exception) {
                    if (exception.getFailure().getAction() == AuthenticationException.AuthenticationAction.STOP) {
                        EggCrack.LOGGER.warning("Stopping session for " + account.getUsername() + ": " + exception.getMessage());
                        break;
                    } else if (exception.getFailure().getAction() == AuthenticationException.AuthenticationAction.NEXT_CREDENTIALS) {
                        credential = credentialIterator.next();
                    }
                }
            } catch (NoSuchElementException exception) {
                break;
            } catch (Throwable ex) {
                //Just ignore it.
            }
        }

        if (accountListener != null) accountListener.onAccountFailed();
        authenticationCallback.onAuthenticationFailed(account);
    }
}
