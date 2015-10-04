package net.teamlixo.eggcrack.session;

import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.account.output.AccountOutput;
import net.teamlixo.eggcrack.authentication.AuthenticationCallback;
import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.authentication.RunnableAuthenticator;
import net.teamlixo.eggcrack.credential.Credential;
import net.teamlixo.eggcrack.list.ExtendedList;
import net.teamlixo.eggcrack.list.array.ExtendedArrayList;
import net.teamlixo.eggcrack.objective.Objective;
import net.teamlixo.eggcrack.proxy.ProxyCallback;
import net.teamlixo.eggcrack.proxy.RunnableProxyChecker;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class Session implements Runnable, AuthenticationCallback, ProxyCallback {
    private final ExecutorService executorService;
    private final AuthenticationService authenticationService;

    private final ExtendedList<Account> accountList;
    private final ExtendedList<Credential> credentialList;
    private final ExtendedList<Proxy> proxyList;
    private final ExtendedList<Objective> objectiveList;
    private final ExtendedList<AccountOutput> outputList;

    private final Tracker tracker;

    private SessionListener sessionListener;

    private final URL checkUrl;

    public Session(ExecutorService executorService,
                   AuthenticationService authenticationService,
                   ExtendedList<Account> accountList,
                   ExtendedList<Credential> credentialList,
                   ExtendedList<Proxy> proxyList,
                   ExtendedList<Objective> objectiveList,
                   ExtendedList<AccountOutput> outputList,
                   Tracker tracker,
                   URL checkUrl) {
        this.executorService = executorService;
        this.authenticationService = authenticationService;

        this.accountList = accountList;
        this.credentialList = credentialList;
        this.proxyList = proxyList;
        this.objectiveList = objectiveList;
        this.outputList = outputList;

        this.tracker = tracker;
        this.tracker.setTotal(accountList.size());

        this.checkUrl = checkUrl;
    }

    public SessionListener getListener() {
        return sessionListener;
    }

    public void setListener(SessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    @Override
    public void run() {
        ExtendedList<Future> futureList = new ExtendedArrayList<Future>();

        Iterator<Proxy> proxyIterator = proxyList.iterator(false);
        if (checkUrl != null) {
            EggCrack.LOGGER.info("Checking proxies with URL \"" + checkUrl.toString() + "\"...");
            long start = System.currentTimeMillis();

            synchronized (proxyList) {
                while (proxyIterator.hasNext())
                    futureList.add(
                            executorService.submit(
                                    new RunnableProxyChecker(
                                            proxyIterator.next(),
                                            checkUrl,
                                            this
                                    )
                            )
                    );
            }

            waitFutures(futureList, new FutureRunnable() {
                long lastSecond = System.currentTimeMillis();

                @Override
                public boolean run(float progress) {
                    if (System.currentTimeMillis() > lastSecond + 1000) {
                        lastSecond = System.currentTimeMillis();
                        EggCrack.LOGGER.info((int) Math.floor(progress * 100f) + "% complete.");
                    }

                    return true;
                }
            });

            EggCrack.LOGGER.info("Proxy check completed successfully in " +
                    (System.currentTimeMillis() - start) + "ms. Proxies available: " + proxyList.size() + ".");
        }

        EggCrack.LOGGER.info("Startup complete; initiating session...");

        if (sessionListener != null) sessionListener.started();

        futureList.clear();
        proxyIterator = proxyList.iterator(true);
        Iterator<Account> accountIterator = accountList.iterator(false);
        while (accountIterator.hasNext())
            futureList.add(
                    executorService.submit(
                            new RunnableAuthenticator(
                                    authenticationService,
                                    accountIterator.next(),
                                    tracker,
                                    credentialList.iterator(false),
                                    proxyIterator,
                                    this
                            )
                    )
            );

        final long totalAttempts = Math.max(accountList.size(), accountList.size() * credentialList.size());
        waitFutures(futureList, new FutureRunnable() {
            long lastSecond = System.currentTimeMillis();
            long requestsLastSecond = 0;
            long attemptsLastSecond = 0;

            @Override
            public boolean run(float progress) {
                if (System.currentTimeMillis() > lastSecond + 1000) {
                    lastSecond = System.currentTimeMillis();

                    if (sessionListener != null) sessionListener.update(progress, tracker);

                    EggCrack.LOGGER.info((Math.floor(Math.max(progress * 1000f, ((float)tracker.getAttempts() / (float)totalAttempts) * 1000f)) / 10f) + "% complete (" +
                            tracker.getCompleted() + "/" + (accountList.size() - tracker.getFailed()) + ") | Attempts: " +
                            tracker.getAttempts() + " (" + (tracker.getAttempts() - attemptsLastSecond) +  " of " +
                            (tracker.getRequests() - requestsLastSecond) + " requests)");

                    requestsLastSecond = tracker.getRequests();
                    attemptsLastSecond = tracker.getAttempts();
                }

                Iterator<Objective> objectiveIterator = objectiveList.iterator(false);
                while (objectiveIterator.hasNext()) {
                    Objective objective = objectiveIterator.next();
                    if (objective.check(tracker)) {
                        //Shutdown
                        EggCrack.LOGGER.info(objective.getClass().getSimpleName() + " was met; ending session.");
                        return false; //Break.
                    }
                }

                return true;
            }
        });

        Iterator<Future> futureIterator = futureList.iterator(false);
        while (futureIterator.hasNext()) {
            futureIterator.next().cancel(true);
            futureIterator.remove();
        }

        if (sessionListener != null) sessionListener.completed();

        EggCrack.LOGGER.info("Session complete. Runtime: " + (tracker.elapsedMilliseconds() / 1000f) + " seconds.");
        EggCrack.LOGGER.info(" Total requests: " + tracker.getRequests());
        EggCrack.LOGGER.info(" Attempts: " + tracker.getAttempts() + " (" + (Math.floor(((float) tracker.getAttempts() / (float) tracker.getRequests()) * 1000f) / 10f) + "%)");
        EggCrack.LOGGER.info(" Accounts completed: " + tracker.getCompleted());
        EggCrack.LOGGER.info(" Accounts failed: " + tracker.getFailed());
    }

    @Override
    public void onAuthenticationCompleted(AuthenticatedAccount account) {
        EggCrack.LOGGER.info("Account successfully recovered: " + account.getUsername());

        Iterator<AccountOutput> accountOutputIterator = outputList.iterator(false);
        while (accountOutputIterator.hasNext()) {
            AccountOutput accountOutput = accountOutputIterator.next();
            try {
                accountOutput.save(account);
            } catch (IOException e) {
                EggCrack.LOGGER.severe("Failed to save credentials for " + account.getUsername() +
                        " (" + accountOutput.getClass().getSimpleName() + "): " + e.getMessage());
            }
        }

        synchronized (tracker) {
            tracker.setCompleted(tracker.getCompleted() + 1);
        }
    }

    @Override
    public void onAuthenticationFailed(Account account) {
        synchronized (tracker) {
            tracker.setFailed(tracker.getFailed() + 1);
        }
    }

    @Override
    public void onProxyFailed(Proxy proxy) {
        synchronized (proxyList) {
            proxyList.remove(proxy);
        }
    }

    private static void waitFutures(ExtendedList<Future> futureList, FutureRunnable update) {
        Iterator<Future> futureIterator = futureList.iterator(true);
        int original = futureList.size();
        while (futureIterator.hasNext()) {
            Future future = futureIterator.next();
            if (future.isDone() || future.isCancelled()) {
                futureIterator.remove();
            }

            if (!update.run(1f - ((float)futureList.size() / (float)original)))
                break;
        }
    }

    private interface FutureRunnable {
        public boolean run(float progress);
    }
}
