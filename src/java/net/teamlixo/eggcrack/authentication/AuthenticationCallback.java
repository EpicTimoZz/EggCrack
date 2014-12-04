package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.credential.Credential;

public interface AuthenticationCallback {
    public void onAuthenticationCompleted(Account account, Credential credential);
    public void onAuthenticationFailed(Account account);
}
