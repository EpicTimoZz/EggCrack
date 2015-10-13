package net.teamlixo.eggcrack;

import com.google.gson.Gson;
import com.mojang.authlib.Agent;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.output.AccountOutput;
import net.teamlixo.eggcrack.account.output.FileAccountOutput;
import net.teamlixo.eggcrack.account.output.UrlAccountOutput;
import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.config.Configuration;
import net.teamlixo.eggcrack.config.JsonConfiguration;
import net.teamlixo.eggcrack.credential.Credential;
import net.teamlixo.eggcrack.credential.Credentials;
import net.teamlixo.eggcrack.list.ExtendedList;
import net.teamlixo.eggcrack.list.array.ExtendedArrayList;
import net.teamlixo.eggcrack.account.output.AttemptedAccount;
import net.teamlixo.eggcrack.objective.Objective;
import net.teamlixo.eggcrack.objective.ObjectiveCompleted;
import net.teamlixo.eggcrack.objective.ObjectiveRequests;
import net.teamlixo.eggcrack.objective.ObjectiveTime;
import net.teamlixo.eggcrack.plugin.FilePluginManager;
import net.teamlixo.eggcrack.plugin.SystemPluginManager;
import net.teamlixo.eggcrack.plugin.java.json.JsonPluginLoader;
import net.teamlixo.eggcrack.session.Session;
import net.teamlixo.eggcrack.session.Tracker;
import net.teamlixo.eggcrack.ui.UserInterface;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    public static final void main(String[] args) throws
            IOException, ClassNotFoundException, UnsupportedLookAndFeelException,
            InstantiationException, IllegalAccessException {

        //Prefer IPv4 stack. Fixes some issues.
        System.setProperty("java.net.preferIPv4Stack", "true");

        //Configure the logger (It's just going to output to console)
        EggCrack.LOGGER = Logger.getLogger("EggCrack");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new LineLogFormatter());
        EggCrack.LOGGER.addHandler(consoleHandler);
        EggCrack.LOGGER.setUseParentHandlers(false);
        EggCrack.LOGGER.setLevel(Level.ALL);

        //Read in parameters.
        OptionParser optionsParser = new OptionParser();

        ArgumentAcceptingOptionSpec consoleArgument = optionsParser.accepts("console").withOptionalArg();
        ArgumentAcceptingOptionSpec debugArgument = optionsParser.accepts("debug").withOptionalArg();
        ArgumentAcceptingOptionSpec configArgument = optionsParser.accepts("config").withRequiredArg().ofType(String.class).defaultsTo("eggcrack.json");

        ArgumentAcceptingOptionSpec methodArgument = optionsParser.accepts("method").withOptionalArg().ofType(String.class).defaultsTo("Minecraft");

        ArgumentAcceptingOptionSpec proxyArgument = optionsParser.accepts("proxies").withRequiredArg().ofType(String.class).defaultsTo("proxies.txt");
        ArgumentAcceptingOptionSpec usernameArgument = optionsParser.accepts("usernames").withRequiredArg().ofType(String.class).defaultsTo("usernames.txt");
        ArgumentAcceptingOptionSpec passwordArgument = optionsParser.accepts("passwords").withRequiredArg().ofType(String.class).defaultsTo("passwords.txt");

        ArgumentAcceptingOptionSpec threadsArgument = optionsParser.accepts("threads").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(32));
        ArgumentAcceptingOptionSpec intervalArgument = optionsParser.accepts("interval").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(10));
        ArgumentAcceptingOptionSpec checkArgument = optionsParser.accepts("checkUrl").withOptionalArg().ofType(String.class);
        ArgumentAcceptingOptionSpec proxyTimeout = optionsParser.accepts("proxyTimeout").withOptionalArg().ofType(Integer.class);

        ArgumentAcceptingOptionSpec completedObjectiveArgument = optionsParser.accepts("objectiveCompleted").withOptionalArg().ofType(Integer.class);
        ArgumentAcceptingOptionSpec timeObjectiveArgument = optionsParser.accepts("objectiveSeconds").withOptionalArg().ofType(Integer.class);
        ArgumentAcceptingOptionSpec requestObjectiveArgument = optionsParser.accepts("objectiveRequests").withOptionalArg().ofType(Integer.class);

        ArgumentAcceptingOptionSpec fileOutputArgument = optionsParser.accepts("outputFile").withOptionalArg().ofType(String.class);
        ArgumentAcceptingOptionSpec urlOutputArgument = optionsParser.accepts("outputUrl").withOptionalArg().ofType(String.class);

        ArgumentAcceptingOptionSpec logFileArgument = optionsParser.accepts("logFile").withOptionalArg().ofType(String.class);
        ArgumentAcceptingOptionSpec logLevelArgument = optionsParser.accepts("logLevel").withOptionalArg().ofType(String.class).defaultsTo("warn");

        ArgumentAcceptingOptionSpec checkAccountsArgument = optionsParser.accepts("check").withOptionalArg();

        OptionSet optionSet = optionsParser.parse(args);

        if (optionSet.has(debugArgument)) {
            consoleHandler.setLevel(Level.FINEST);
            EggCrack.LOGGER.fine("Console debugging enabled.");
        } else consoleHandler.setLevel(Level.INFO);

        File file = new File((String) optionSet.valueOf(configArgument));
        Configuration configuration = (new Gson()).fromJson(new InputStreamReader(new FileInputStream(file)), JsonConfiguration.class);
        String branch = ((JsonConfiguration)configuration).getUpdateBranch();
        int version = configuration.getVersion();

        System.out.println("EggCrack build " + configuration.getVersion() + ", Copyright (C) Team Lixo");
        System.out.println("EggCrack comes with ABSOLUTELY NO WARRANTY. This is free software,");
        System.out.println("and you are welcome to redistribute it under certain conditions.");
        System.out.println("");

        final boolean guiAvailable = Desktop.isDesktopSupported() && !GraphicsEnvironment.isHeadless();
        boolean guiEnabled = !optionSet.has(consoleArgument);

        if (configuration.isUpdateEnabled()) {
            EggCrack.LOGGER.fine("Checking for updates to build #" + version + " on branch " +  branch + "...");

            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(configuration.getUpdateURL().openStream()));
                int remoteVersion = Integer.parseInt(bufferedReader.readLine());
                if (remoteVersion > 0 && remoteVersion > version) {
                    EggCrack.LOGGER.warning("A newer version of EggCrack is available: build #" + remoteVersion + ".");
                    EggCrack.LOGGER.warning("Download at GitHub: https://github.com/Manevolent/EggCrack");

                    if (guiEnabled) {
                        JLabel label = new JLabel();

                        JEditorPane ep = new JEditorPane("text/html",
                                "<html>An update for EggCrack is available for download online. " +
                                "If you would like to download this update, please visit our GitHub page:<br/><br/>" +
                                "<a href=\"https://github.com/Manevolent/EggCrack\">https://github.com/Manevolent/EggCrack</a><br/>" +
                                "Current build: #" + version + "<br/><b>Updated build: #" + remoteVersion + " (branch: " + branch + ")</b><br/><br/>" +
                                "If you would not like to be notified with these messages, see your eggcrack.json configuration.<br/><br/></html>"
                        );

                        ep.setBackground(label.getBackground());
                        ep.setEditable(false);
                        ep.addHyperlinkListener(new HyperlinkListener()
                        {
                            @Override
                            public void hyperlinkUpdate(HyperlinkEvent e)
                            {
                                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                                    if (guiAvailable)
                                    {
                                        try {
                                            Desktop.getDesktop().browse(e.getURL().toURI());
                                            System.exit(0);
                                        } catch (IOException e1) {
                                        } catch (URISyntaxException e1) {
                                        }
                                    }
                            }
                        });

                        JOptionPane.showMessageDialog(null, ep);
                    }
                } else {
                    EggCrack.LOGGER.fine("Update check complete; no new versions found.");
                }
            } catch (Exception ex) {
                EggCrack.LOGGER.warning("Problem retrieving updates from the repository:");
                ex.printStackTrace();
            }
        }

        //Load plugins in console mode.
        FilePluginManager pluginManager = new SystemPluginManager();
        pluginManager.registerPluginLoader("jar", new JsonPluginLoader());
        EggCrack.setInstance(new EggCrack(pluginManager, version));

        try {
            EggCrack.LOGGER.info("Loading plugins...");
            EggCrack.getInstance().loadPlugins(new File("." + File.separator + "plugins" + File.separator));
        } catch (UnsupportedOperationException ex) {
            EggCrack.LOGGER.warning("Problem loading plugins:");
            ex.printStackTrace();
        }

        //Find method.
        for (AuthenticationService thisService : EggCrack.getInstance().listAuthenticationServices())
            EggCrack.LOGGER.fine("Available authentication service: " + thisService.getName().toLowerCase());

        //Decide if we need to fork off into GUI mode or not.
        if (guiEnabled) {
            if (!guiAvailable)
                throw new UnsupportedOperationException("Desktop environment not supported on this system.");

            EggCrack.LOGGER.fine("Skipping console arguments and launching in GUI-mode...");

            //Run EggCrack.
            UserInterface.main(args);

            //Shutdown.
            shutdown();
            return;
        }


        String method = (String) optionSet.valueOf(methodArgument);
        AuthenticationService authenticationService = null;

        if (method.trim().length() <= 0) {
            EggCrack.LOGGER.severe("Authentication method not specified");
            return;
        } else {
            for (AuthenticationService thisService : EggCrack.getInstance().listAuthenticationServices()) {
                if (thisService.getName().equalsIgnoreCase(method)) {
                    authenticationService = thisService;
                    break;
                }
            }
        }

        if (authenticationService == null) {
            EggCrack.LOGGER.severe("No suitable authentication service was found for requested \"" + method.toLowerCase() + "\".");
            return;
        } else EggCrack.LOGGER.info("Using selected authentication service " + authenticationService.getName()
                + " (" + authenticationService.getClass().getName() + ").");

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

        //Import accounts from file.
        ExtendedList<Account> accountList = new ExtendedArrayList<Account>();
        if (!optionSet.has(checkAccountsArgument)) {
            File usernameFile = new File(optionSet.valueOf(usernameArgument).toString());
            BufferedReader usernameReader = new BufferedReader(new FileReader(usernameFile));
            while (usernameReader.ready()) {
                String line = usernameReader.readLine();
                if (line.trim().length() <= 0) continue;
                accountList.add(new AttemptedAccount(line.trim()));
            }
            usernameReader.close();

            EggCrack.LOGGER.info("Loaded " + accountList.size() + " usernames (" + usernameFile.getAbsolutePath() + ").");
        } else {
            File usernameFile = new File(optionSet.valueOf(checkAccountsArgument).toString());
            BufferedReader usernameReader = new BufferedReader(new FileReader(usernameFile));
            while (usernameReader.ready()) {
                String line = usernameReader.readLine();
                if (line.trim().length() <= 0) continue;

                String[] params = line.split(":");
                if (params.length < 2) {
                    EggCrack.LOGGER.warning("Bad line");
                }

                AttemptedAccount account = new AttemptedAccount(params[0].trim());
                account.setUncheckedPassword(params[1]);
                accountList.add(account);
            }
            usernameReader.close();

            EggCrack.LOGGER.info("Loaded " + accountList.size() + " unchecked accounts (" + usernameFile.getAbsolutePath() + ").");
        }

        //Import credentials from file.
        ExtendedList<Credential> credentialList = new ExtendedArrayList<Credential>();
        if (!optionSet.has(checkAccountsArgument)) {
            File passwordFile = new File(optionSet.valueOf(passwordArgument).toString());
            BufferedReader passwordReader = new BufferedReader(new FileReader(passwordFile));
            while (passwordReader.ready())
                credentialList.add(Credentials.createPassword(passwordReader.readLine().trim()));
            passwordReader.close();
            EggCrack.LOGGER.info("Loaded " + credentialList.size() + " passwords (" + passwordFile.getAbsolutePath() + ").");
        }

        //Import proxies from file.
        ExtendedList<Proxy> proxyList = new ExtendedArrayList<Proxy>();
        if (optionSet.has(proxyArgument)) {
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
        } else proxyList.add(Proxy.NO_PROXY);

        //Initialize the EggCrack instance to create a credential store.
        final Session session = new Session(
                executorService,
                authenticationService,
                accountList,
                credentialList,
                proxyList,
                objectiveList,
                outputList,
                tracker,
                checkUrl,
                optionSet.has(proxyTimeout) ? (Integer) optionSet.valueOf(proxyTimeout) : 1000
        );

        //Run EggCrack.
        session.run();

        //Shutdown.
        shutdown();
    }

    private static final void shutdown() {
        EggCrack.LOGGER.info("Unloading plugins...");
        EggCrack.getInstance().unloadPlugins();
        EggCrack.LOGGER.info("Shutdown complete.");

        System.exit(0);
    }
}
