package net.teamlixo.eggcrack.account;

import net.teamlixo.eggcrack.credential.Credential;

public interface AccountListener {

    /**
     * Called when an account is added to the system.
     */
    public void onAccountStarted();

    /**
     * Called when an account is removed from the system.
     */
    public void onAccountFailed();

    /**
     * Called when an account is being attempted.
     * @param credential Credential being attempted.
     */
    public void onAccountAttempting(Credential credential);

    /**
     * Called when an account has been recovered.
     * @param credential Credential.
     */
    public void onAccountCompleted(Credential credential);

}
