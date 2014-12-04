package net.teamlixo.eggcrack.account;

public abstract class Account {
    private final String username;
    private AccountListener accountListener;

    public Account(String username) {
        this.username = username;
    }

    public void setListener(AccountListener accountListener) {
        this.accountListener = accountListener;
    }

    public AccountListener getListener() {
        return accountListener;
    }

    /**
     * Gets the account's username.
     * @return Username of the account.
     */
    public String getUsername() {
        return username;
    }
}
