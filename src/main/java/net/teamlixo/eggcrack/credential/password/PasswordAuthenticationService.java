package net.teamlixo.eggcrack.credential.password;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.authentication.AbstractAuthenticationService;
import net.teamlixo.eggcrack.authentication.AuthenticationException;
import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.credential.Credential;

import java.net.Proxy;

/**
 * Extends the AuthenticationService class and provides password authentication to the system.
 */
public abstract class PasswordAuthenticationService extends AbstractAuthenticationService {
    public PasswordAuthenticationService(String name) {
        super(name);
    }

    @Override
    public AuthenticatedAccount authenticate(Account account, Credential credential, Proxy proxy) throws AuthenticationException {
        if (!(credential instanceof PasswordCredential))
            throw new AuthenticationException(AuthenticationException.AuthenticationFailure.INVALID_CREDENTIAL, "credential not properly instantiated");

        return authenticate(account, ((PasswordCredential)credential).getPassword(), proxy);
    }

    protected abstract AuthenticatedAccount authenticate(Account account, String password, Proxy proxy) throws AuthenticationException;
}
