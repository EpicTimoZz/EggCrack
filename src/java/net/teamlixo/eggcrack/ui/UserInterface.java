package net.teamlixo.eggcrack.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class UserInterface extends JDialog {
    private JPanel contentPane;
    private JSpinner maxthreads;
    private JSpinner interval;
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
    private JCheckBox skipNonPremiumAccountsCheckBox;
    private JCheckBox autoSort;
    private JLabel attemptcnt;
    private JLabel eta;
    private JRadioButton cracking;
    private JRadioButton checking;
    private JLabel ulbl;
    private JLabel plbl;
    private JCheckBox sc;
    private JFormattedTextField submiturl;
    private JCheckBox oc;
    private JCheckBox threadClock;
    private JSpinner proxyTimeout;
    private JCheckBox checkProxies;
    private JLabel checkLbl;
    private JPanel pnl;

    public UserInterface() {
        super((Frame)null, ModalityType.TOOLKIT_MODAL);

        setContentPane(contentPane);
        setModal(true);
        setResizable(false);
        setTitle("EggCrack by Manevolent");
        setName("EggCrack by Manevolent");

        maxthreads.setModel(new SpinnerNumberModel(32, 1, 10240000, 2));
        interval.setModel(new SpinnerNumberModel(5, 0, 60000, 5));
        proxyTimeout.setModel(new SpinnerNumberModel(5000, 1, 300000, 1000));
        proxyTimeout.setVisible(false);
        checkLbl.setVisible(false);

        String[] columnNames = {"Username",
                "Password",
                "Requests",
                "Status"};

        table1.setRowHeight(35);
        table1.setModel(new DefaultTableModel(columnNames, 0));
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

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
                System.exit(0);
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
                System.out.println(pnl.size());
            }
        });
    }

    public static void main(String[] args) {

        try {

            UIManager.put("nimbusBase", new Color(10, 10, 10));
            UIManager.put("nimbusBlueGrey", new Color(200, 200, 210));
            UIManager.put("control", new Color(150, 150, 150));

            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }


        } catch (Exception e) {
            // If Nimbus is not available, fall back to cross-platform
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {

            }
        }

        UserInterface dialog = new UserInterface();
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        System.exit(0);
    }

    private void createUIComponents() {

    }
}
