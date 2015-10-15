package net.teamlixo.eggcrack.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import net.teamlixo.eggcrack.AuthenticatorThreadFactory;
import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.output.AttemptedAccount;
import net.teamlixo.eggcrack.authentication.configuration.ServiceConfiguration;
import net.teamlixo.eggcrack.objective.ObjectiveCompleted;
import net.teamlixo.eggcrack.session.Session;
import net.teamlixo.eggcrack.session.SessionListener;
import net.teamlixo.eggcrack.session.Tracker;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AccountListener;
import net.teamlixo.eggcrack.account.output.AccountOutput;
import net.teamlixo.eggcrack.account.output.FileAccountOutput;
import net.teamlixo.eggcrack.account.output.UrlAccountOutput;
import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.config.EggCrackConfiguration;
import net.teamlixo.eggcrack.credential.Credential;
import net.teamlixo.eggcrack.credential.Credentials;
import net.teamlixo.eggcrack.list.ExtendedList;
import net.teamlixo.eggcrack.list.array.ExtendedArrayList;
import net.teamlixo.eggcrack.objective.Objective;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class UserInterface extends JDialog implements AccountListener, SessionListener {
    private JPanel contentPane;
    private JSpinner maxthreads;
    private JTable table1;
    private JButton start;
    private JScrollPane scroll;
    private JScrollPane scroll2;
    private JLabel thdcount;
    private JLabel crackedcnt;
    private JLabel failedcnt;
    private JLabel tps;
    private JProgressBar progress;
    private JTabbedPane tabs;
    private JButton ul;
    private JFormattedTextField usernamesFile;
    private JButton pl;
    private JButton hl;
    private JButton sl;
    private JFormattedTextField socksProxies;
    private JFormattedTextField httpProxies;
    private JFormattedTextField passwordsFile;
    private JButton ol;
    private JFormattedTextField outputFile;
    private JButton exit;
    private JComboBox api;
    private JLabel proxiescnt;
    private JLabel eta;
    private JRadioButton cracking;
    private JRadioButton checking;
    private JLabel ulbl;
    private JLabel plbl;
    private JCheckBox sc;
    private JFormattedTextField submiturl;
    private JCheckBox oc;
    private JSpinner proxyTimeout;
    private JCheckBox checkProxies;
    private JLabel checkLbl;
    private JPanel pnl;
    private JLabel versionLabel;
    private JLabel aboutPanel;
    private JCheckBox objective;
    private JLabel completedLbl;
    private JSpinner completedSpinner;
    private JButton configureButton;
    private JCheckBox enableConsoleDebuggingCheckBox;
    private ProxiesInterface proxiesInterface = new ProxiesInterface();

    private volatile Session activeSession;

    private long lastAttempts = 0L;

    public UserInterface() {
        super((Frame) null, ModalityType.TOOLKIT_MODAL);

        setContentPane(contentPane);
        setModal(true);
        setResizable(false);

        setTitle("EggCrack 2.0");
        setName("EggCrack 2.0");

        try {
            versionLabel.setText(versionLabel.getText() + " build #" + EggCrackConfiguration.getJarVersionNumber());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<AuthenticationService> authenticationServices =
                new ArrayList<>(EggCrack.getInstance().listAuthenticationServices());
        String[] authenticationServiceNames = new String[authenticationServices.size()];
        for (int i = 0; i < authenticationServiceNames.length; i++)
            authenticationServiceNames[i] = authenticationServices.get(i).getName();

        api.setModel(new DefaultComboBoxModel(authenticationServiceNames));
        setupConfiguration();

        maxthreads.setModel(new SpinnerNumberModel(32, 1, 10240000, 2));
        proxyTimeout.setModel(new SpinnerNumberModel(5000, 1, 300000, 1000));
        proxyTimeout.setVisible(false);
        checkLbl.setVisible(false);
        completedLbl.setVisible(false);
        completedSpinner.setVisible(false);

        String[] columnNames = {
                "Username",
                "Password",
                "Requests",
                "Status"};

        table1.setRowHeight(35);
        table1.setModel(new DefaultTableModel(columnNames, 0));
        final AccountListener accountListener = this;
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (start.getText().equals("Stop")) {
                    if (activeSession != null) activeSession.setRunning(false);
                    else start.setText("Start");
                    return;
                }

                //Set up the executor service responsible for executing threads.
                ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(
                        (Integer) maxthreads.getValue(),
                        new AuthenticatorThreadFactory(Thread.MIN_PRIORITY)
                );

                Tracker tracker = new Tracker();

                String method = api.getSelectedItem().toString();
                AuthenticationService authenticationService = null;

                if (method.length() <= 0) {
                    JOptionPane.showMessageDialog(null, "Authentication method not specified.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    for (AuthenticationService thisService : EggCrack.getInstance().listAuthenticationServices()) {
                        if (thisService.getName().equals(method)) {
                            authenticationService = thisService;
                            break;
                        }
                    }
                }

                if (authenticationService == null)
                    JOptionPane.showMessageDialog(null, "Couldn't find authentication method \"" + method + "\".", "Error", JOptionPane.ERROR_MESSAGE);

                ExtendedList<Objective> objectiveList = new ExtendedArrayList<Objective>();
                if (objective.isSelected())
                    objectiveList.add(new ObjectiveCompleted((Integer) completedSpinner.getValue()));

                ExtendedList<AccountOutput> outputList = new ExtendedArrayList<AccountOutput>();
                if (outputFile.getText() != null && outputFile.getText().trim().length() > 0)
                    outputList.add(new FileAccountOutput(new File(outputFile.getText())));
                if (sc.isSelected() && submiturl.getText() != null && submiturl.getText().trim().length() > 0)
                    try {
                        outputList.add(new UrlAccountOutput(URI.create(submiturl.getText()).toURL()));
                    } catch (MalformedURLException e1) {
                        JOptionPane.showMessageDialog(null, "URL provided is malformed.", "Error", JOptionPane.ERROR_MESSAGE);
                    }

                //Import accounts from file.
                ExtendedList<Account> accountList = new ExtendedArrayList<Account>();
                try {
                    File usernameFile = new File(usernamesFile.getText());
                    BufferedReader usernameReader = new BufferedReader(new FileReader(usernameFile));
                    while (usernameReader.ready()) {
                        String line = usernameReader.readLine();
                        if (line.trim().length() <= 0) continue;
                        if (cracking.isSelected()) {
                            Account account = new AttemptedAccount(line.trim());
                            account.setListener(accountListener);
                            accountList.add(account);
                        } else if (checking.isSelected()) {
                            String[] args = line.split(":");
                            if (args.length < 2) continue;
                            Account account = new AttemptedAccount(args[0].trim());
                            account.setUncheckedPassword(args[1]);
                            account.setListener(accountListener);
                            accountList.add(account);
                        }

                    }
                    usernameReader.close();
                    EggCrack.LOGGER.info("Loaded " + accountList.size() + " accounts (" + usernameFile.getAbsolutePath() + ").");
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "Username file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, "Username file could not be read: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                //Import credentials from file.
                ExtendedList<Credential> credentialList = new ExtendedArrayList<Credential>();
                if (cracking.isSelected()) {
                    try {
                        File passwordFile = new File(passwordsFile.getText());
                        BufferedReader passwordReader = new BufferedReader(new FileReader(passwordFile));
                        while (passwordReader.ready())
                            credentialList.add(Credentials.createPassword(passwordReader.readLine().trim()));
                        passwordReader.close();
                        EggCrack.LOGGER.info("Loaded " + credentialList.size() + " passwords (" + passwordFile.getAbsolutePath() + ").");
                    } catch (FileNotFoundException ex) {
                        JOptionPane.showMessageDialog(null, "Password file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(null, "Password file could not be read: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                //Import proxies from file.
                ExtendedList<Proxy> proxyList = new ExtendedArrayList<Proxy>();
                try {
                    if (httpProxies.getText() != null && httpProxies.getText().trim().length() > 0) {
                        File proxyFile = new File(httpProxies.getText());
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
                        EggCrack.LOGGER.info("Loaded " + proxyList.size() + " HTTP proxies (" + proxyFile.getAbsolutePath() + ").");
                    }
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "HTTP proxy file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, "HTTP proxy file could not be read: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    if (socksProxies.getText() != null && socksProxies.getText().trim().length() > 0) {
                        File proxyFile = new File(socksProxies.getText());
                        BufferedReader proxyReader = new BufferedReader(new FileReader(proxyFile));
                        while (proxyReader.ready()) {
                            String[] proxyLine = proxyReader.readLine().split(":");
                            try {
                                proxyList.add(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyLine[0], Integer.parseInt(proxyLine[1]))));
                            } catch (Exception ex) {
                                EggCrack.LOGGER.warning("Problem loading proxy: " + ex.getMessage());
                            }
                        }
                        proxyReader.close();
                        EggCrack.LOGGER.info("Loaded " + proxyList.size() + " SOCKS proxies (" + proxyFile.getAbsolutePath() + ").");
                    }
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "SOCKS proxy file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, "SOCKS proxy file could not be read: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (accountList.size() <= 0) {
                    JOptionPane.showMessageDialog(null, "No accounts were loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (cracking.isSelected() && credentialList.size() <= 0) {
                    JOptionPane.showMessageDialog(null, "No passwords were loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (proxyList.size() <= 0) {
                    JOptionPane.showMessageDialog(null, "No proxies were loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    Session session = new Session(
                            executorService,
                            authenticationService,
                            accountList,
                            credentialList,
                            proxyList,
                            objectiveList,
                            outputList,
                            tracker,
                            checkProxies.isSelected() ? URI.create("http://google.com/").toURL() : null,
                            checkProxies.isSelected() ? Integer.parseInt(proxyTimeout.getValue().toString()) : 0
                    );

                    session.setListener(UserInterface.this);

                    Thread thread = new Thread(session);
                    thread.setDaemon(true);
                    thread.start();

                    activeSession = session;
                    start.setText("Stop");
                } catch (MalformedURLException e1) {
                    JOptionPane.showMessageDialog(null, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });
        scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {

            }
        });

        final JFileChooser chooser = new JFileChooser();
        ul.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = chooser.showOpenDialog(UserInterface.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    usernamesFile.setText(file.getAbsolutePath());
                }
            }
        });
        pl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = chooser.showOpenDialog(UserInterface.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    passwordsFile.setText(file.getAbsolutePath());
                }
            }
        });
        hl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = chooser.showOpenDialog(UserInterface.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    httpProxies.setText(file.getAbsolutePath());
                }
            }
        });
        sl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = chooser.showOpenDialog(UserInterface.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    socksProxies.setText(file.getAbsolutePath());
                }
            }
        });
        ol.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = chooser.showOpenDialog(UserInterface.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    outputFile.setText(file.getAbsolutePath());
                }
            }
        });
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        checking.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordsFile.setEnabled(false);
                pl.setEnabled(false);
                pl.setVisible(false);
                passwordsFile.setVisible(false);
                plbl.setVisible(false);
                ulbl.setText("User:Pass:");
            }
        });
        cracking.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordsFile.setEnabled(true);
                pl.setEnabled(true);
                pl.setVisible(true);
                passwordsFile.setVisible(true);
                plbl.setVisible(true);
                ulbl.setText("Usernames:");
            }
        });
        checkProxies.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                proxyTimeout.setVisible(checkProxies.isSelected());
                checkLbl.setVisible(checkProxies.isSelected());
            }
        });
        objective.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                completedLbl.setVisible(objective.isSelected());
                completedSpinner.setVisible(objective.isSelected());
            }
        });
        enableConsoleDebuggingCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EggCrack.LOGGER.getHandlers()[0].setLevel(enableConsoleDebuggingCheckBox.isSelected() ? Level.ALL : Level.INFO);
            }
        });
    }

    public void setupConfiguration() {
        String apiName = this.api.getSelectedItem() != null ? this.api.getSelectedItem().toString() : null;
        if (apiName == null) return;

        for (AuthenticationService thisService : EggCrack.getInstance().listAuthenticationServices()) {
            if (thisService.getName().equals(apiName)) {
                // See if this service has any configuration.
                if (thisService.getConfiguration() == null) break;

                final List<ServiceConfiguration.Option> options = thisService.getConfiguration().getOptions();
                if (options == null || options.size() <= 0) break;

                configureButton.setEnabled(activeSession == null || activeSession.isRunning());

                for (ActionListener al : configureButton.getActionListeners()) configureButton.removeActionListener(al);
                configureButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ConfigurationInterface configurationInterface = new ConfigurationInterface();
                        for (ServiceConfiguration.Option option : options) configurationInterface.addOption(option);
                        configurationInterface.setLocationRelativeTo(UserInterface.this);
                        configurationInterface.setVisible(true);
                    }
                });

                return;
            }
        }

        configureButton.setEnabled(false);
    }

    public static void main(String[] args) throws
            ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        try {
            UIManager.put("nimbusBase", new Color(10, 10, 10));
            UIManager.put("nimbusBlueGrey", new Color(200, 200, 210));
            UIManager.put("control", new Color(200, 200, 200));

            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }

        UserInterface dialog = new UserInterface();
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true); //Show.
    }

    private void createUIComponents() {

    }

    @Override
    public void onAccountStarted(final Account account) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ensureRow(account);
            }
        });
    }

    @Override
    public void onAccountFailed(final Account account) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Row row = getRow(account);
                if (row != null) row.remove();
            }
        });
    }

    @Override
    public void onAccountAttempting(final Account account, Credential credential) {
        if (credential != null) {
            final String password = credential.toString();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Row row = ensureRow(account);
                    row.setStatus("Trying " + password + "...");
                }
            });
        }
    }

    @Override
    public void onAccountCompleted(final Account account, final Credential credential) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Row row = ensureRow(account);
                row.setStatus("<html><b><font color=\"green\">Cracked</font></b></html>");
                row.setPassword(credential.toString());
            }
        });
    }

    @Override
    public void onAccountTried(final Account account, Credential credential) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ensureRow(account).sort();
            }
        });
    }

    @Override
    public void onAccountRequested(final Account account) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Row row = ensureRow(account);
                row.setRequests(row.getRequests() + 1);
            }
        });
    }

    private DefaultTableModel getTableModel() {
        return (DefaultTableModel) table1.getModel();
    }

    private Row getRow(Account account) {
        for (int i = 0; i < table1.getModel().getRowCount(); i++) {
            if (getTableModel().getValueAt(i, 0).toString().equals(account.getUsername()))
                return new Row(i);
        }

        return null;
    }

    private boolean hasRow(Account account) {
        return getRow(account) != null;
    }

    private Row getRow(int index) {
        if (getTableModel().getRowCount() > index)
            return new Row(index);
        else return null;
    }

    private void clearRows() {
        Row row = null;
        while ((row = getRow(0)) != null) row.remove();
    }

    private void emptyRows() {
        Row row = null;
        int x = 0;
        while ((row = getRow(x)) != null) {
            if (row.getPassword().equals("-")) row.remove();
            else x++;
        }
    }

    private Row ensureRow(Account account) {
        Row row = getRow(account);
        if (row == null) {
            getTableModel().addRow(new Object[]{account.getUsername(), "-", "0", "Initializing..."});
            return getRow(account);
        } else
            return row;
    }

    @Override
    public void started(Step step) {
        if (step == Step.CRACKING) {
            this.tabs.setSelectedIndex(1);
            this.lastAttempts = 0L;

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    configureButton.setEnabled(false);
                    proxiesInterface.setVisible(false);
                    clearRows();
                }
            });
        } else if (step == Step.PROXY_CHECKING) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    proxiesInterface.setLocationRelativeTo(UserInterface.this);
                    proxiesInterface.setVisible(true);

                    if (proxiesInterface.isCancelled()) {
                        EggCrack.LOGGER.info("Proxy checking cancelled.");
                        activeSession.setRunning(false);
                    }
                }
            });
        }
    }

    @Override
    public void update(float progress, Tracker tracker, int availableProxies) {
        if (proxiesInterface.isVisible()) {
            proxiesInterface.update(progress, availableProxies + " working");
            return;
        }

        this.progress.setValue((int) ((float) this.progress.getMaximum() * progress));

        if (availableProxies > 0)
            this.proxiescnt.setText(String.valueOf(availableProxies));
        else
            this.proxiescnt.setText("<html><b><font color=\"red\">0</font></b></html>");

        this.crackedcnt.setText(tracker.getCompleted() + "/" + (tracker.getTotal() - tracker.getFailed()));
        this.failedcnt.setText(tracker.getFailed() + "/" + tracker.getTotal());

        int attempts = tracker.getAttempts();
        this.tps.setText(String.valueOf(attempts - lastAttempts));
        lastAttempts = attempts;

        UserInterface.this.thdcount.setText(activeSession.getCurrentThreads());

        // ETA:

        if (progress > 0F) {
            double elapsedSeconds = (double) tracker.elapsedMilliseconds() / 1000D;
            long diffInSeconds = (long) Math.ceil((elapsedSeconds / (double) Math.min(1F, progress)) - elapsedSeconds);
            diffInSeconds = Math.max(0, Math.min(30 * 24 * 60 * 60, diffInSeconds));

            if (diffInSeconds > 0)
                eta.setText(getDurationBreakdown(diffInSeconds * 1000L));
            else eta.setText("");
        } else eta.setText("");
    }

    /**
     * Used from
     * http://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
     * <p/>
     * Convert a millisecond duration to a string format
     *
     * @param millis A duration to convert to a string form
     * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
     */
    public static String getDurationBreakdown(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        sb.append(days);
        sb.append(days == 1 ? " day, " : " days, ");
        sb.append(hours);
        sb.append(hours == 1 ? " hour, " : " hours, ");
        sb.append(minutes);
        sb.append(minutes == 1 ? " minute, " : " minutes, ");
        ;
        sb.append(seconds);
        sb.append(seconds == 1 ? " second" : " seconds");
        ;

        return (sb.toString());
    }

    @Override
    public void completed() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                setupConfiguration();

                UserInterface.this.tps.setText("0");
                UserInterface.this.tabs.setSelectedIndex(1);
                UserInterface.this.thdcount.setText("0");

                emptyRows();
                JOptionPane.showMessageDialog(UserInterface.this, "Cracking completed.", "EggCrack", JOptionPane.INFORMATION_MESSAGE);
                start.setText("Start");
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.setBackground(new Color(-1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setBackground(new Color(-1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(500, 600), new Dimension(500, 600), new Dimension(500, 600), 0, false));
        tabs = new JTabbedPane();
        tabs.setBackground(new Color(-1));
        tabs.setForeground(new Color(-1));
        panel1.add(tabs, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(8, 2, new Insets(10, 10, 10, 10), -1, -1));
        panel2.setBackground(new Color(-1));
        tabs.addTab("Control", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 15, 0), -1, -1));
        panel3.setBackground(new Color(-1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cracking = new JRadioButton();
        cracking.setSelected(true);
        cracking.setText("Cracking");
        panel3.add(cracking, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checking = new JRadioButton();
        checking.setText("Checking");
        panel3.add(checking, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(10, 0));
        panel4.setBackground(new Color(-1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Login API:");
        panel4.add(label1, BorderLayout.WEST);
        api = new JComboBox();
        api.setForeground(new Color(-1));
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Minecraft Yggdrasil Authentication");
        api.setModel(defaultComboBoxModel1);
        panel4.add(api, BorderLayout.CENTER);
        configureButton = new JButton();
        configureButton.setEnabled(false);
        configureButton.setText("Configure...");
        panel4.add(configureButton, BorderLayout.EAST);
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 3, new Insets(5, 10, 5, 2), -1, -1));
        panel5.setBackground(new Color(-1));
        panel2.add(panel5, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-6710887)), "Credentials", TitledBorder.CENTER, TitledBorder.TOP, new Font("Levenim MT", panel5.getFont().getStyle(), panel5.getFont().getSize()), new Color(-16777216)));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel6.setBackground(new Color(-1));
        panel5.add(panel6, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        ulbl = new JLabel();
        ulbl.setText("Usernames:");
        panel6.add(ulbl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        usernamesFile = new JFormattedTextField();
        usernamesFile.setEditable(false);
        usernamesFile.setText("");
        panel6.add(usernamesFile, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        ul = new JButton();
        ul.setText("Load");
        panel6.add(ul, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel7.setBackground(new Color(-1));
        panel5.add(panel7, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        plbl = new JLabel();
        plbl.setText("Passwords:");
        panel7.add(plbl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        passwordsFile = new JFormattedTextField();
        passwordsFile.setEditable(false);
        panel7.add(passwordsFile, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        pl = new JButton();
        pl.setText("Load");
        panel7.add(pl, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(3, 1, new Insets(5, 10, 5, 2), -1, -1));
        panel8.setBackground(new Color(-1));
        panel2.add(panel8, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-6710887)), "Proxies", TitledBorder.CENTER, TitledBorder.TOP, new Font("Levenim MT", panel8.getFont().getStyle(), panel8.getFont().getSize()), new Color(-16777216)));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel9.setBackground(new Color(-1));
        panel8.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("HTTP Proxies:");
        panel9.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        httpProxies = new JFormattedTextField();
        httpProxies.setEditable(false);
        httpProxies.setText("");
        panel9.add(httpProxies, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        hl = new JButton();
        hl.setText("Load");
        panel9.add(hl, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel10.setBackground(new Color(-1));
        panel8.add(panel10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("SOCKS Proxies:");
        panel10.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        socksProxies = new JFormattedTextField();
        socksProxies.setEditable(false);
        socksProxies.setText("");
        panel10.add(socksProxies, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        sl = new JButton();
        sl.setText("Load");
        panel10.add(sl, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pnl = new JPanel();
        pnl.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        pnl.setBackground(new Color(-1));
        panel8.add(pnl, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), new Dimension(-1, 28), new Dimension(-1, 28), 0, false));
        checkProxies = new JCheckBox();
        checkProxies.setBackground(new Color(-1));
        checkProxies.setSelected(false);
        checkProxies.setText("Check proxies");
        pnl.add(checkProxies, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        checkLbl = new JLabel();
        checkLbl.setText("Timeout (ms):");
        pnl.add(checkLbl, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        proxyTimeout = new JSpinner();
        proxyTimeout.setEnabled(true);
        pnl.add(proxyTimeout, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(3, 4, new Insets(5, 10, 5, 2), -1, -1));
        panel11.setBackground(new Color(-1));
        panel2.add(panel11, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel11.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-6710887)), "Methodology", TitledBorder.CENTER, TitledBorder.TOP, new Font("Levenim MT", panel11.getFont().getStyle(), panel11.getFont().getSize()), new Color(-16777216)));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel12.setBackground(new Color(-1));
        panel11.add(panel12, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Max Threads:");
        panel12.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        maxthreads = new JSpinner();
        panel12.add(maxthreads, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel13.setBackground(new Color(-1));
        panel11.add(panel13, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), new Dimension(-1, 28), new Dimension(-1, 28), 0, false));
        objective = new JCheckBox();
        objective.setBackground(new Color(-1));
        objective.setSelected(false);
        objective.setText("Objective");
        panel13.add(objective, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        completedLbl = new JLabel();
        completedLbl.setText("Completed:");
        completedLbl.setVisible(true);
        panel13.add(completedLbl, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        completedSpinner = new JSpinner();
        completedSpinner.setEnabled(true);
        panel13.add(completedSpinner, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(2, 1, new Insets(5, 10, 5, 2), -1, -1));
        panel14.setBackground(new Color(-1));
        panel2.add(panel14, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel14.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-6710887)), "Output", TitledBorder.CENTER, TitledBorder.TOP, new Font("Levenim MT", panel14.getFont().getStyle(), panel14.getFont().getSize()), new Color(-16777216)));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel15.setBackground(new Color(-1));
        panel14.add(panel15, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        outputFile = new JFormattedTextField();
        outputFile.setEditable(false);
        panel15.add(outputFile, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        ol = new JButton();
        ol.setText("...");
        panel15.add(ol, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        oc = new JCheckBox();
        oc.setBackground(new Color(-1));
        oc.setSelected(true);
        oc.setText("Output File:");
        panel15.add(oc, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel16.setBackground(new Color(-1));
        panel14.add(panel16, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        submiturl = new JFormattedTextField();
        submiturl.setEditable(true);
        submiturl.setText("http://myapi.com/submit.php?user=$user&password=$pass");
        panel16.add(submiturl, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        sc = new JCheckBox();
        sc.setBackground(new Color(-1));
        sc.setSelected(false);
        sc.setText("Submit:");
        panel16.add(sc, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel17.setBackground(new Color(-1));
        panel2.add(panel17, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        start = new JButton();
        start.setText("Start");
        panel17.add(start, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer2 = new Spacer();
        panel17.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        exit = new JButton();
        exit.setText("Exit");
        panel17.add(exit, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel18.setBackground(new Color(-1));
        tabs.addTab("Status", panel18);
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new GridLayoutManager(7, 1, new Insets(10, 10, 1, 10), -1, -1));
        panel19.setBackground(new Color(-10066330));
        panel18.add(panel19, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel20.setAutoscrolls(true);
        panel20.setBackground(new Color(-10066330));
        panel20.setForeground(new Color(-3355444));
        panel19.add(panel20, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setFont(new Font("Arial", Font.BOLD, 13));
        label5.setForeground(new Color(-3355444));
        label5.setText("Current Threads:");
        panel20.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0, false));
        thdcount = new JLabel();
        thdcount.setForeground(new Color(-3355444));
        thdcount.setText("0");
        panel20.add(thdcount, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel21.setAutoscrolls(true);
        panel21.setBackground(new Color(-10066330));
        panel21.setForeground(new Color(-3355444));
        panel19.add(panel21, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setFont(new Font("Arial", Font.BOLD, 13));
        label6.setForeground(new Color(-3355444));
        label6.setText("Accounts Cracked:");
        panel21.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0, false));
        crackedcnt = new JLabel();
        crackedcnt.setForeground(new Color(-3355444));
        crackedcnt.setText("0/0");
        panel21.add(crackedcnt, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel22 = new JPanel();
        panel22.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel22.setAutoscrolls(true);
        panel22.setBackground(new Color(-10066330));
        panel22.setForeground(new Color(-3355444));
        panel19.add(panel22, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setFont(new Font("Arial", Font.BOLD, 13));
        label7.setForeground(new Color(-3355444));
        label7.setText("Accounts Failed:");
        panel22.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0, false));
        failedcnt = new JLabel();
        failedcnt.setForeground(new Color(-3355444));
        failedcnt.setText("0/0");
        panel22.add(failedcnt, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel23 = new JPanel();
        panel23.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel23.setAutoscrolls(true);
        panel23.setBackground(new Color(-10066330));
        panel23.setForeground(new Color(-3355444));
        panel19.add(panel23, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setFont(new Font("Arial", Font.BOLD, 13));
        label8.setForeground(new Color(-3355444));
        label8.setText("Attempts/sec:");
        panel23.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0, false));
        tps = new JLabel();
        tps.setForeground(new Color(-3355444));
        tps.setText("0");
        panel23.add(tps, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progress = new JProgressBar();
        panel19.add(progress, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel24 = new JPanel();
        panel24.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel24.setAutoscrolls(true);
        panel24.setBackground(new Color(-10066330));
        panel24.setForeground(new Color(-3355444));
        panel19.add(panel24, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setFont(new Font("Arial", Font.BOLD, 13));
        label9.setForeground(new Color(-3355444));
        label9.setText("Available Proxies:");
        panel24.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0, false));
        proxiescnt = new JLabel();
        proxiescnt.setForeground(new Color(-3355444));
        proxiescnt.setText("-");
        panel24.add(proxiescnt, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        eta = new JLabel();
        eta.setFont(new Font("Arial", Font.BOLD, 13));
        eta.setForeground(new Color(-3355444));
        eta.setText("");
        panel19.add(eta, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, new Dimension(500, -1), 0, false));
        scroll = new JScrollPane();
        scroll.setAutoscrolls(true);
        scroll.setBackground(new Color(-1));
        panel18.add(scroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        table1 = new JTable();
        table1.setGridColor(new Color(-1));
        table1.setIntercellSpacing(new Dimension(0, 0));
        table1.setMinimumSize(new Dimension(30, 80));
        table1.setRequestFocusEnabled(false);
        table1.setRowMargin(0);
        table1.setRowSelectionAllowed(false);
        scroll.setViewportView(table1);
        final JPanel panel25 = new JPanel();
        panel25.setLayout(new GridLayoutManager(8, 4, new Insets(20, 20, 20, 20), -1, -1));
        panel25.setBackground(new Color(-1));
        tabs.addTab("About", panel25);
        final JPanel panel26 = new JPanel();
        panel26.setLayout(new GridLayoutManager(6, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel26.setBackground(new Color(-1));
        panel25.add(panel26, new GridConstraints(0, 0, 6, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setFont(new Font(label10.getFont().getName(), label10.getFont().getStyle(), 28));
        label10.setText("EggCrack");
        panel26.add(label10, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setFocusable(false);
        label11.setFont(new Font(label11.getFont().getName(), Font.BOLD, 14));
        label11.setText("Version:");
        panel26.add(label11, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        versionLabel = new JLabel();
        versionLabel.setFocusable(false);
        versionLabel.setFont(new Font(versionLabel.getFont().getName(), versionLabel.getFont().getStyle(), 14));
        versionLabel.setText("2.0");
        panel26.add(versionLabel, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setFocusable(false);
        label12.setFont(new Font(label12.getFont().getName(), Font.BOLD, 14));
        label12.setText("Release Date:");
        panel26.add(label12, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setFocusable(false);
        label13.setFont(new Font(label13.getFont().getName(), label13.getFont().getStyle(), 14));
        label13.setText("October 10th, 2015");
        panel26.add(label13, new GridConstraints(2, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setFocusable(false);
        label14.setFont(new Font(label14.getFont().getName(), Font.BOLD, 14));
        label14.setText("Author:");
        panel26.add(label14, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        label15.setFocusable(false);
        label15.setFont(new Font(label15.getFont().getName(), label15.getFont().getStyle(), 14));
        label15.setText("Manevolent");
        panel26.add(label15, new GridConstraints(3, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setFocusable(false);
        label16.setFont(new Font(label16.getFont().getName(), Font.BOLD, 14));
        label16.setText("License:");
        panel26.add(label16, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        label17.setFocusable(false);
        label17.setFont(new Font(label17.getFont().getName(), label17.getFont().getStyle(), 14));
        label17.setText("GNU GPL v2");
        panel26.add(label17, new GridConstraints(4, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enableConsoleDebuggingCheckBox = new JCheckBox();
        enableConsoleDebuggingCheckBox.setBackground(new Color(-1));
        enableConsoleDebuggingCheckBox.setText("Enable console debugging");
        panel26.add(enableConsoleDebuggingCheckBox, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel25.add(spacer3, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel27 = new JPanel();
        panel27.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel27.setBackground(new Color(-1));
        panel25.add(panel27, new GridConstraints(6, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel27.setBorder(BorderFactory.createTitledBorder("    "));
        aboutPanel = new JLabel();
        aboutPanel.setText("<html> <p>I'm currently an active member of <b>Team Lixo</b>. You can see <br/>our videos at <a href=\"https://youtube.com/teamlixo/\">https://youtube.com/teamlixo</a></p> <br/><br/> <p> EggCrack is now an open-source project! Come check us out on GitHub:<br/> <a href=\"https://github.com/Manevolent/EggCrack\">https://github.com/Manevolent/EggCrack</a> </p>\n<br/><br/>\n<p>EggCrack is running in GUI mode. If you would like to run\nEggCrack from<br/> the console, use the <i>-console</i> flag.</p>");
        panel27.add(aboutPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel28 = new JPanel();
        panel28.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel28.setBackground(new Color(-1));
        contentPane.add(panel28, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final HeaderImage headerImage1 = new HeaderImage();
        panel28.add(headerImage1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(500, 100), null, 0, false));
        checkLbl.setLabelFor(maxthreads);
        label4.setLabelFor(maxthreads);
        completedLbl.setLabelFor(maxthreads);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(checking);
        buttonGroup.add(checking);
        buttonGroup.add(cracking);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private class Row {
        private int rowIndex;

        public Row(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public void setPassword(String password) {
            getTableModel().setValueAt(password, rowIndex, 1);
            sort();
        }

        public int getRequests() {
            return Integer.parseInt(getTableModel().getValueAt(rowIndex, 2).toString());
        }

        public void setRequests(int requests) {
            getTableModel().setValueAt(Integer.toString(requests), rowIndex, 2);
        }

        public String getPassword() {
            return getTableModel().getValueAt(rowIndex, 1).toString();
        }

        public void setStatus(String status) {
            getTableModel().setValueAt(status, rowIndex, 3);
            sort();
        }

        public void remove() {
            getTableModel().removeRow(rowIndex);
        }

        public void sort() {
            getTableModel().moveRow(rowIndex, rowIndex, getTableModel().getRowCount() - 1);
        }
    }
}
