package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.credential.Credential;

import java.util.UUID;

/**
 * A wide-response callback listener interface. Used internally.
 */
public interface AuthenticationCallback {

    /**
     * Called when authentication is successful.
     * @param account Account authenticated.
     */
    public void onAuthenticationCompleted(AuthenticatedAccount account);

    /**
     * Called when authentication was unsuccessful.
     * @param account Account that failed authentication.
     */
    public void onAuthenticationFailed(Account account);

}
