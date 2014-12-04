package net.teamlixo.eggcrack.timer;

public interface Timer {

    /**
     * Finds if the timer is ready.
     * @return true if the timer is ready.
     */
    public boolean isReady();

    /**
     * Advances the timer.
     */
    public void next();

}
