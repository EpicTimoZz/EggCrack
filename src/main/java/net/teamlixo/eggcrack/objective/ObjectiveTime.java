package net.teamlixo.eggcrack.objective;

import net.teamlixo.eggcrack.session.Tracker;

public class ObjectiveTime extends Objective {
    private long maximumSeconds;

    public ObjectiveTime(long maximumSeconds) {
        this.maximumSeconds = maximumSeconds;
    }

    @Override
    public boolean check(Tracker tracker) {
        return tracker.elapsedMilliseconds() >= maximumSeconds * 1000;
    }
}
