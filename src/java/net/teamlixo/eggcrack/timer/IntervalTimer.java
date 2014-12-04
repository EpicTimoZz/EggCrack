package net.teamlixo.eggcrack.timer;

public final class IntervalTimer implements Timer {
    private final long interval;
    private long nextTick;

    public IntervalTimer(long seconds) {
        this(seconds, RateWindow.SECOND);
    }

    public IntervalTimer(float interval, RateWindow window) {
        this.interval = (long) Math.floor((float)window.getNanoseconds() / interval);
    }

    @Override
    public boolean isReady() {
        return System.nanoTime() >= nextTick;
    }

    @Override
    public void next() {
        nextTick = System.nanoTime() + interval;
    }

    public enum RateWindow {
        HOUR(3600000000000L),
        MINUTE(60000000000L),
        SECOND(1000000000L),
        MILLISECOND(1000000L),
        NANOSECOND(1L);

        private long nanoseconds;
        RateWindow(long nanoseconds) {
            this.nanoseconds = nanoseconds;
        }

        public long getNanoseconds() {
            return nanoseconds;
        }
    }
}
