package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.credential.Credential;

import java.net.Proxy;

public interface AuthenticationService {
    /**
     * Authenticates an account with a password.
     * @param account Account to authenticate.
     * @param credential Credential to authenticate with.
     * @param proxy Proxy to authenticate with.
     * @return true if authentication was successful, false otherwise.
     */
    public boolean authenticate(Account account, Credential credential, Proxy proxy) throws AuthenticationException;
}
