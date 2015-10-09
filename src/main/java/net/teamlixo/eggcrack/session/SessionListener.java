package net.teamlixo.eggcrack.session;

public interface SessionListener {
    public void started(Step step);
    public void update(float progress, Tracker tracker, int availableProxies);
    public void completed();

    public static enum Step {
        PROXY_CHECKING,
        CRACKING
    }
}
