package net.teamlixo.eggcrack;

public final class Tracker {
    private int completed = 0;
    private int failed = 0;
    private int requests = 0;
    private int attempts = 0;
    private long start = System.currentTimeMillis();

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public long elapsedMilliseconds() {
        return System.currentTimeMillis() - start;
    }
}
