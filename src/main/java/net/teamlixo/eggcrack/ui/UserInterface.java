package net.teamlixo.eggcrack.ui;

import net.teamlixo.eggcrack.AuthenticatorThreadFactory;
import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.output.AttemptedAccount;
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
import java.util.concurrent.TimeUnit;

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
    private ProxiesInterface proxiesInterface = new ProxiesInterface();

    private volatile Session activeSession;

    private long lastAttempts = 0L;

    public UserInterface() {
        super((Frame)null, ModalityType.TOOLKIT_MODAL);

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
        for (int i = 0; i < authenticationServiceNames.length; i ++)
            authenticationServiceNames[i] = authenticationServices.get(i).getName();

        api.setModel(new DefaultComboBoxModel(authenticationServiceNames));

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
                ExecutorService executorService = Executors.newFixedThreadPool(
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
                        if (thisService.getName().equalsIgnoreCase(method)) {
                            authenticationService = thisService;
                            break;
                        }
                    }
                }

                if (authenticationService == null)
                    JOptionPane.showMessageDialog(null, "Couldn't find authentication method \"" + method + "\".", "Error", JOptionPane.ERROR_MESSAGE);

                ExtendedList<Objective> objectiveList = new ExtendedArrayList<Objective>();
                if (objective.isSelected()) objectiveList.add(new ObjectiveCompleted((Integer) completedSpinner.getValue()));

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
                            checkProxies.isSelected() ? URI.create("http://google.com/").toURL() : null
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
    public void started(SessionListener.Step step) {
        if (step == Step.CRACKING) {
            this.tabs.setSelectedIndex(1);

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    proxiesInterface.setVisible(false);
                    clearRows();
                }
            });
        } else if (step == Step.PROXY_CHECKING) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    proxiesInterface.setVisible(true);
                    if(activeSession != null && proxiesInterface.isCancelled())
                        activeSession.setRunning(false);
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

        this.progress.setValue((int) ((float)this.progress.getMaximum() * progress));

        if (availableProxies > 0)
            this.proxiescnt.setText(String.valueOf(availableProxies));
        else
            this.proxiescnt.setText("<html><b><font color=\"red\">0</font></b></html>");

        this.crackedcnt.setText(tracker.getCompleted() + "/" + (tracker.getTotal() - tracker.getFailed()));
        this.failedcnt.setText(tracker.getFailed() + "/" + tracker.getTotal());

        int attempts = tracker.getAttempts();
        this.tps.setText(String.valueOf(attempts - lastAttempts));
        lastAttempts = attempts;

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
     *
     * Convert a millisecond duration to a string format
     *
     * @param millis A duration to convert to a string form
     * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
     */
    public static String getDurationBreakdown(long millis)
    {
        if(millis < 0)
        {
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
        sb.append(minutes == 1 ? " minute, " : " minutes, ");;
        sb.append(seconds);
        sb.append(seconds == 1 ? " second" : " seconds");;

        return(sb.toString());
    }

    @Override
    public void completed() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                UserInterface.this.tabs.setSelectedIndex(1);
                emptyRows();
                JOptionPane.showMessageDialog(UserInterface.this, "Cracking completed.", "EggCrack", JOptionPane.INFORMATION_MESSAGE);
                start.setText("Start");
            }
        });
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
