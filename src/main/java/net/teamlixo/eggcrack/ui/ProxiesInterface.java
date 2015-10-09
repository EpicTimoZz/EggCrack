package net.teamlixo.eggcrack.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ProxiesInterface extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JProgressBar progressBar;
    private JLabel description;
    private volatile boolean cancelled;

    public ProxiesInterface() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setSize(new Dimension(350, 300));

        description.setText("Initializing...");
        progressBar.setIndeterminate(true);

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onCancel() {
// add your code here if necessary
        cancelled = true;
        setVisible(false);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setVisible(boolean visible) {
        cancelled = false;
        super.setVisible(visible);
    }

    public void update(float progress, String s) {
        description.setText(s);

        progressBar.setMaximum(progressBar.getWidth());
        progressBar.setValue((int)Math.floor(progressBar.getWidth() * progress));
        progressBar.setIndeterminate(false);
    }
}
