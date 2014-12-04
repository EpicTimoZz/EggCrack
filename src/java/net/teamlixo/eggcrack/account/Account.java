package net.teamlixo.eggcrack.account;

public abstract class Account {
    private final String username;

    public Account(String username) {
        this.username = username;
    }

    /**
     * Gets the account's username.
     * @return Username of the account.
     */
    public String getUsername() {
        return username;
    }
}
