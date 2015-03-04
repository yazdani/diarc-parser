/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 * 
 * Copyright 1997-2013 Matthias Scheutz
 * All rights reserved. Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 */
package ade.gui.logview;

import ade.gui.icons.IconFetcher;
import ade.gui.logview.ADELogPlaybackVis.LogPanel;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

class LiveModePanel extends JPanel implements LogPanel {

    private static final long serialVersionUID = 1L;

    public LiveModePanel(final ADELogPlaybackVis logVis) {
        this.setLayout(new GridLayout(1, 2));


        JButton btnSwitchToLog = new JButton("Switch to Log Playback mode",
                IconFetcher.get16x16icon("appointment-new.png"));
        btnSwitchToLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logVis.switchToLogPlaybackMode();
            }
        });
        this.add(btnSwitchToLog);


        JButton btnChooseAnotherLog = new JButton("Choose log file",
                IconFetcher.get16x16icon("document-open.png"));
        btnChooseAnotherLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // if request to open log file succeeded
                if (logVis.promptToOpenLogFile()) {
                    if (JOptionPane.showConfirmDialog(logVis,
                            "Switch to Log Playback mode now?") == JOptionPane.YES_OPTION) {
                        logVis.switchToLogPlaybackMode();
                    }
                }
            }
        });
        this.add(btnChooseAnotherLog);
    }

    @Override
    public void refreshLogPanel() throws Exception {
        // nothing to do in live mode.
    }
}
