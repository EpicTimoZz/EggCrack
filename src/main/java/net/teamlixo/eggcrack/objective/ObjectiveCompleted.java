package net.teamlixo.eggcrack.objective;

import net.teamlixo.eggcrack.session.Tracker;

public class ObjectiveCompleted extends Objective {
    private int target = 0;

    public ObjectiveCompleted(int target) {
        this.target = target;
    }

    @Override
    public boolean check(Tracker tracker) {
        return tracker.getCompleted() >= target;
    }
}
