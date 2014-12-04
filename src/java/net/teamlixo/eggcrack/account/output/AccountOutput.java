package net.teamlixo.eggcrack.account.output;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.credential.Credential;

import java.io.IOException;

public abstract class AccountOutput {
    /**
     * Saves an account to the output.
     * @param account Account to save.
     * @param credential Credential to save.
     * @throws java.io.IOException
     */
    public abstract void save(Account account, Credential credential) throws IOException;
}
