package net.teamlixo.eggcrack.account.output;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.credential.Credential;

import java.io.IOException;
import java.util.UUID;

public abstract class AccountOutput {
    /**
     * Saves an account to the output.
     * @param account Account to save.
     * @throws java.io.IOException
     */
    public abstract void save(AuthenticatedAccount account) throws IOException;
}
