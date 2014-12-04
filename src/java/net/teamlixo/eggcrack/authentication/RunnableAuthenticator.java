package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.credential.Credential;

import java.net.Proxy;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
        //Startup
        Credential credential = credentialIterator.next();
        Thread.currentThread().setName("Authenticator-" + account.getUsername());

        while (proxyIterator.hasNext()) {
            try {
                if (credential == null)
                    credential = credentialIterator.next();

                EggCrack.LOGGER.finest("[Account: " + account.getUsername() +
                        "] Sending authentication request [password=" + credential.toString() + "]...");

                try {
                    if (authenticationService.authenticate(account, credential, proxyIterator.next())) {
                        authenticationCallback.onAuthenticationCompleted(account, credential);
                        break;
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

        //Cleanup
        authenticationCallback.onAuthenticationFailed(account);
    }
}
