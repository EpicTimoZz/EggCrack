package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.credential.Credential;

/**
 * A wide-response callback listener interface. Used internally.
 */
public interface AuthenticationCallback {

    /**
     * Called when authentication is successful.
     * @param account Account authenticated.
     * @param credential Credential used to authenticate the account.
     */
    public void onAuthenticationCompleted(Account account, Credential credential);

    /**
     * Called when authentication was unsuccessful.
     * @param account Account that failed authentication.
     */
    public void onAuthenticationFailed(Account account);

}
