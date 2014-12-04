package net.teamlixo.eggcrack.credential.password;

import net.teamlixo.eggcrack.credential.Credential;

/**
 * Provides password functionality to the Credential class. See @toString()
 */
public class PasswordCredential implements Credential {
    private final String password;

    public PasswordCredential(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return getPassword();
    }
}
