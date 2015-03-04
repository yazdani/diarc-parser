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

import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.icons.IconFetcher;
import ade.gui.logview.ADELogPlaybackVis.LogPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class PlaybackModePanel extends JPanel implements LogPanel {

    private static final long serialVersionUID = 1L;
    private final ADELogPlaybackVis logVis;
    private JCheckBox checkboxRun;
    private JButton timeButton;
    private JSlider slider;

    public PlaybackModePanel(final ADELogPlaybackVis logVis) {
        this.logVis = logVis;
        setLayout(new BorderLayout(0, 0));


        JButton btnSwitchToLog = new JButton("<html>Switch to<br/>Live mode</html>",
                IconFetcher.get16x16icon("start-here.png"));
        btnSwitchToLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logVis.switchToLiveMode();
            }
        });
        add(btnSwitchToLog, BorderLayout.WEST);


        JButton btnChooseAnotherLog = new JButton("<html>Choose another<br/>log file</html>",
                IconFetcher.get16x16icon("document-open.png"));
        btnChooseAnotherLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logVis.promptToOpenLogFile();
                // already in playback mode, so nothing else to do.
            }
        });
        add(btnChooseAnotherLog, BorderLayout.EAST);

        JPanel panel = new JPanel();
        add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout(0, 0));

        checkboxRun = new JCheckBox("Run");
        checkboxRun.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logVis.setPlaybackRunning(checkboxRun.isSelected());
            }
        });
        checkboxRun.setSelected(true);
        panel.add(checkboxRun, BorderLayout.WEST);

        timeButton = new JButton("");
        timeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeButtonPressed();
            }
        });
        panel.add(timeButton, BorderLayout.EAST);

        slider = createSlider();

        panel.add(slider, BorderLayout.CENTER);

    }

    private JSlider createSlider() {
        final JSlider slider = new JSlider();
        slider.setMaximum(0); // something completely useless/not valid -- that way,
        //    will obviously need to update itself and tickmarks at refreshGUI()

        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        slider.setPreferredSize(new Dimension(200, 40));

        slider.addMouseListener(new MouseAdapter() {
            private boolean formerRun;

            @Override
            public void mouseReleased(MouseEvent e) {
                logVis.setPlaybackRunning(formerRun);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    formerRun = (Boolean) logVis.callComponent("getPlaybackRunning");

                    logVis.setPlaybackRunning(formerRun);
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(logVis,
                            UtilUI.messageInScrollPane(Util.stackTraceString(e1)),
                            "Could not stop running while moving the slider",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // if this is a user-caused change
                if (slider.getValueIsAdjusting()) {
                    setPlaybackPosition(slider.getValue());
                }
            }
        });

        return slider;
    }

    private void timeButtonPressed() {
        String jumpTo = JOptionPane.showInputDialog(this,
                "Please specify a time, "
                + "in milliseconds, to jump to:", slider.getValue());
        if (jumpTo != null) {
            try {
                int jumpToInt = Integer.parseInt(jumpTo);
                if ((jumpToInt < 0) || (jumpToInt > slider.getMaximum())) {
                    throw new NumberFormatException(jumpToInt + " is < 0, or greater "
                            + "than the maximum playback position");
                } else {
                    setPlaybackPosition(jumpToInt);
                }
            } catch (NumberFormatException e1) {
                JOptionPane.showMessageDialog(this,
                        e1, "Invalid time specified",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void refreshLogPanel() throws Exception {
        // ensure that the slider still displays the maximum position properly

        int maximumPosition = (Integer) logVis.callComponent("maxPlaybackPosition");
        if (maximumPosition != slider.getMaximum()) {
            slider.setMaximum(maximumPosition);
            slider.setMajorTickSpacing(maximumPosition / 2);
            slider.setMinorTickSpacing(maximumPosition / 10);
        }

        int position = (Integer) logVis.callComponent("getPlaybackPosition");
        slider.setValue(position);
        timeButton.setText(Integer.toString(position));

        checkboxRun.setSelected((Boolean) logVis.callComponent("getPlaybackRunning"));
    }

    void setPlaybackPosition(int position) {
        try {
            logVis.callComponent("setPlaybackPosition", logVis.credentials, position);
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(logVis,
                    UtilUI.messageInScrollPane(Util.stackTraceString(e1)),
                    "Could not set playback position",
                    JOptionPane.ERROR_MESSAGE);
        }

        slider.setValue(position);
        timeButton.setText(Integer.toString(position));
    }
}
