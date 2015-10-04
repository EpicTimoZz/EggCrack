package net.teamlixo.eggcrack.session;

import net.teamlixo.eggcrack.session.Tracker;

public interface SessionListener {
    public void started();
    public void update(float progress, Tracker tracker);
    public void completed();
}
