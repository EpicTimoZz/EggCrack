package net.teamlixo.eggcrack.account;

import net.teamlixo.eggcrack.EggCrack;

public abstract class Account {
    private final String username;
    private String uncheckedPassword;
    private AccountListener accountListener;
    private volatile State state = State.WAITING;
    private volatile float progress = 0f;

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

    @Override
    public int hashCode() {
        return getUsername().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o instanceof Account) return ((Account) o).getUsername().equalsIgnoreCase(this.getUsername());
        return false;
    }

    public String getUncheckedPassword() {
        return uncheckedPassword;
    }

    public void setUncheckedPassword(String uncheckedPassword) {
        this.uncheckedPassword = uncheckedPassword;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public float getProgress() {
        return progress;
    }

    public void setState(State state) {
        if (this.state != state) EggCrack.LOGGER.fine("Account " + getUsername() + " => " + state);
        if (state == State.STARTED) setProgress(0F);
        else if (state == State.FINISHED) setProgress(1F);

        this.state = state;
    }

    public State getState() {
        return state;
    }

    public enum State {
        WAITING,
        STARTED,
        FINISHED
    }
}
