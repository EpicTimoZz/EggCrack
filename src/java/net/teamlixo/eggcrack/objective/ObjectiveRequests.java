package net.teamlixo.eggcrack.objective;

import net.teamlixo.eggcrack.Tracker;

public class ObjectiveRequests extends Objective {
    private int target;

    public ObjectiveRequests(int target) {
        this.target = target;
    }

    @Override
    public boolean check(Tracker tracker) {
        return tracker.getRequests() >= target;
    }
}
