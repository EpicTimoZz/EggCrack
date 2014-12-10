package net.teamlixo.eggcrack.account;

public abstract class Account {
    private final String username;
    private AccountListener accountListener;

    public Account(String username) {
        this.username = username;
    }

    /**
     * Sets the listener responsible for listening to this account's status.
     * @param accountListener Account listener.
     */
    public final void setListener(AccountListener accountListener) {
        this.accountListener = accountListener;
    }

    /**
     * Gets the account listener responsible for listening to this account's status.
     * @return Account listener.
     */
    public final AccountListener getListener() {
        return accountListener;
    }

    /**
     * Gets the account's username.
     * @return Username of the account.
     */
    public final String getUsername() {
        return username;
    }
}
