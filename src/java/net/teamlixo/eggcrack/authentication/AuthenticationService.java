package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.credential.Credential;

import java.net.Proxy;

/**
 * Internal authentication service interface.
 */
public interface AuthenticationService {

    /**
     * Gets the friendly name for this authentication service.
     * @return Authentication service name.
     */
    public String getName();

    /**
     * Authenticates an account with a password.
     * @param account Account to authenticate.
     * @param credential Credential to authenticate with.
     * @param proxy Proxy to authenticate with.
     * @return AuthenticatedAccount instance.
     */
    public AuthenticatedAccount authenticate(Account account, Credential credential, Proxy proxy) throws AuthenticationException;

}
