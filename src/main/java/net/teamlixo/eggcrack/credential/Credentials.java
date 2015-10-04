package net.teamlixo.eggcrack.credential;

import net.teamlixo.eggcrack.credential.password.PasswordCredential;

/**
 * Helper class used when creating passwords.
 */
public final class Credentials {

    /**
     * Creates a new password credential.
     * @param password Password to use in the credential.
     * @return Credential instance.
     */
    public static final Credential createPassword(String password) {
        return new PasswordCredential(password);
    }

}
