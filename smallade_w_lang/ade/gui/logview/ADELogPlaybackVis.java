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

import ade.ADELogHelper;
import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.icons.IconFetcher;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ADELogPlaybackVis extends ADEGuiPanel {

    private static final long serialVersionUID = 1L;
    private ADELogPlaybackVis meTheLogPlayback = this;

    interface LogPanel {

        public void refreshLogPanel() throws Exception;
    }
    protected final UUID credentials; // passed-in unique ID, from ADEComponentImpl	
    private JPanel alternatingPanelPlaceholder;
    private LiveModePanel livePanel;
    private PlaybackModePanel playbackPanel;
    private Exception lastUpdateException;
    private JButton buttonErrorUpdating;

    public ADELogPlaybackVis(ADEGuiCallHelper guiCallHelper, UUID credentials) {
        super(guiCallHelper, 100);

        this.setLayout(new BorderLayout());

        this.credentials = credentials;

        initGUI();
    }

    private void initGUI() {
        this.alternatingPanelPlaceholder = new JPanel(new CardLayout());
        this.add(alternatingPanelPlaceholder);


        this.livePanel = new LiveModePanel(this);
        this.playbackPanel = new PlaybackModePanel(this);
        alternatingPanelPlaceholder.add(livePanel, new Boolean(true).toString());
        alternatingPanelPlaceholder.add(playbackPanel, new Boolean(false).toString());


        buttonErrorUpdating = new JButton("Error!",
                IconFetcher.get16x16icon("dialog-warning.png"));
        buttonErrorUpdating.setVisible(false);
        buttonErrorUpdating.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(meTheLogPlayback, UtilUI.messageInScrollPane(
                        Util.stackTraceString(lastUpdateException)),
                        "An error occured while trying to update the Log Playback window",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        this.add(buttonErrorUpdating, BorderLayout.EAST);
    }

    @Override
    public void refreshGui() {
        try {
            boolean isLive = (Boolean) callComponent("isLive");

            LogPanel appropriatePanel = (isLive ? livePanel : playbackPanel);
//    		if (currentPanel != appropriatePanel) {
//    			currentPanel = appropriatePanel;
//    			
//    			alternatingPanelPlaceholder.removeAll();
//    			alternatingPanelPlaceholder.add(
//    					(Component)currentPanel, BorderLayout.CENTER);
//    			alternatingPanelPlaceholder.invalidate();
//    		}

            ((CardLayout) alternatingPanelPlaceholder.getLayout()).show(
                    alternatingPanelPlaceholder, new Boolean(isLive).toString());

            appropriatePanel.refreshLogPanel();

            // if got all the way here, no error!
            buttonErrorUpdating.setVisible(false);
        } catch (Exception e) {
            lastUpdateException = e;
            buttonErrorUpdating.setVisible(true);
        }
    }

    @Override
    public Dimension getInitSize(boolean isInternalWindow) {
        if (isInternalWindow) {
            return new Dimension(450, 50);
        } else {
            return new Dimension(800, 50);
        }
    }

    /**
     * will ask for log file to open, will return true if action not canceled
     */
    public boolean promptToOpenLogFile() {
        try {
            String fileToOpen = ADELogHelper.promptForFile(
                    this, (String) callComponent("getPlaybackLogFileName"));
            if (fileToOpen != null) {
                callComponent("openPlaybackLogFile", credentials, fileToOpen);
                return true;
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(meTheLogPlayback,
                    UtilUI.messageInScrollPane(Util.stackTraceString(e1)),
                    "Could not open playback file",
                    JOptionPane.ERROR_MESSAGE);
        }

        // if hasn't returned with true
        return false;
    }

    public String getPlaybackLogFile() {
        try {
            return (String) callComponent("getPlaybackLogFileName");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(meTheLogPlayback,
                    UtilUI.messageInScrollPane(Util.stackTraceString(e)),
                    "Could not get log playback file",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public void switchToLogPlaybackMode() {
        try {
            if (getPlaybackLogFile() == null) {
                JOptionPane.showMessageDialog(this,
                        "The component does not currently have a specified log file."
                        + "\nPlease choose a log file first.",
                        "Could not switch to log playback mode",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                callComponent("setUpdateModeLive", credentials, false);
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(this,
                    UtilUI.messageInScrollPane(Util.stackTraceString(e1)),
                    "Could not switch to log playback mode",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void switchToLiveMode() {
        try {
            callComponent("setUpdateModeLive", credentials, true);
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(meTheLogPlayback,
                    UtilUI.messageInScrollPane(Util.stackTraceString(e1)),
                    "Could not go live",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setPlaybackRunning(boolean flag) {
        try {
            callComponent("setPlaybackRunning", credentials, flag);
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(meTheLogPlayback,
                    UtilUI.messageInScrollPane(Util.stackTraceString(e1)),
                    "Could not set component's running status",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
