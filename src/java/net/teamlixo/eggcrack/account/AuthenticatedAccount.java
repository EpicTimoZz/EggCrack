package net.teamlixo.eggcrack.account;

import net.teamlixo.eggcrack.credential.Credential;

import java.util.UUID;

public class AuthenticatedAccount extends Account {
    private final UUID uuid;
    private final String accountName;
    private final Credential credential;

    public AuthenticatedAccount(String username, String accountName, UUID uuid, Credential credential) {
        super(username);

        this.uuid = uuid;
        this.accountName = accountName;
        this.credential = credential;
    }

    public String getAccountName() {
        return accountName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Credential getCredential() {
        return credential;
    }
}
