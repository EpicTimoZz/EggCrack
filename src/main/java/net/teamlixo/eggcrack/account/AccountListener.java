package net.teamlixo.eggcrack.account;

import net.teamlixo.eggcrack.credential.Credential;

public interface AccountListener {

    /**
     * Called when an account is added to the system.
     */
    public void onAccountStarted(Account account);

    /**
     * Called when an account is removed from the system.
     */
    public void onAccountFailed(Account account);

    /**
     * Called when an account is being attempted.
     * @param credential Credential being attempted.
     */
    public void onAccountAttempting(Account account, Credential credential);

    /**
     * Called when an account has been recovered.
     * @param credential Credential.
     */
    public void onAccountCompleted(Account account, Credential credential);

    /**
     * Called when an account has been attempted credentials.
     * @param account Account attempted.
     * @param credential Credentials attempted.
     */
    public void onAccountTried(Account account, Credential credential);

    public void onAccountRequested(Account account);
}
