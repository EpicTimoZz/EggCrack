package net.teamlixo.eggcrack;

import java.util.concurrent.ThreadFactory;

public class AuthenticatorThreadFactory implements ThreadFactory {
    private final int priority;
    public AuthenticatorThreadFactory(int priority) {
        this.priority = priority;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread authenticatorThread = new Thread(r);

        //Ensure the thread is deamon, and uses minimum priority.
        authenticatorThread.setDaemon(true);
        authenticatorThread.setPriority(priority);

        return authenticatorThread;
    }
}
