/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimSpeechRecognitionComponentVis.java
 *
 * @author Paul Schermerhorn
 */
package com.simspeech;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import java.io.*;
import java.util.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import static utilities.Util.*;

/**
 * GUI clicky speech interface to stand in for speech recognition for
 * testing NLP, etc.
 */
public class SimSpeechRecognitionComponentVis extends ADEGuiPanel {
    private String SConfig;
    public static boolean useUnk = false;
    public static boolean useCommand = true;
    public static boolean toLower = true;
    private Color textColor;

    private Container SFrame = null;
    private JPanel SPanel;
    private JButton SCommandButton;
    private JTextField SCommandText;
    private ButtonGroup speakerchoice;
    private JButton SUnkButton;
    private long prevClick = 0;

    /* Called periodically by the ADEGui.
     */
    @Override
    public void refreshGui() {
        this.repaint();
    }

    private class MouseInputListener implements MouseListener {
        @Override
        public void mousePressed(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof JButton) {
                canLogIt("mousePressed: "+((JButton)c).getText());
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof JButton) {
                canLogIt("mouseReleased: "+((JButton)c).getText());
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof JButton) {
                canLogIt("mouseEntered: "+((JButton)c).getText());
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof JButton) {
                canLogIt("mouseExited: "+((JButton)c).getText());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof JButton) {
                canLogIt("mouseClicked: "+((JButton)c).getText());
            }
        }
    }

    private class InputListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            String Command = null;
            long t0 = 0L, t1 = 0L, t2, t3;
	    String speaker = "";
            if (event.getActionCommand().equals("Command")) {
                // Text command
                Command = SCommandText.getText();
                SCommandText.setText("");
                canLogIt("Text " + Command);
            } else if (event.getActionCommand().equals("<unk>")) {
                String text;
                double p0 = Math.random();
                double p1 = Math.random();
                if (p0 < 0.25) {
                    text = "I didn't catch that.";
                } else if (p0 < 0.5) {
                    text = "I couldn't understand you.";
                } else if (p0 < 0.75) {
                    text = "could you repeat that?";
                } else {
                    text = "I missed that.";
                }
                if (p1 < 0.33) {
                    text = "I'm sorry, " + text;
                } else if (p1 < 0.67) {
                    text = "Sorry, " + text;
                }
                /*
                try {
                    call(nlp, "sayText", text, false, true);
                } catch (ADEException ace) {
                    System.out.println(prg + ": " + ace);
                }
                */
                canLogIt("<unk>");
            } else {
                // Button command
                long now = System.currentTimeMillis();
                long elapsed = now - prevClick;
                prevClick = now;
                if (elapsed < 100) {
                    return;
                }
                Command = event.getActionCommand();
                t0 = System.currentTimeMillis();
                canLogIt("Button " + Command);
                t1 = System.currentTimeMillis();
                if (useCommand)
                    SCommandText.setText("");
            }
            if (Command == null) {
                return;
            }
            String input = Command.trim();
            if (toLower) {
                input = input.toLowerCase();
            }
            t2 = System.currentTimeMillis();
            // send it to the server
            try {
                callComponent("setText", input);
            } catch (Exception ace) {
                System.err.println("SSRSVis: error sending received text: "+ace);
            }
            t3 = System.currentTimeMillis();
            canLogIt("call times: "+t0+" "+t1+" "+t2+" "+t3);
        }
    }

    private void canLogIt(String log) {
        try {
            callComponent("logText", log);
        } catch (Exception ace) {
            System.err.println("SSRSVis: error logging: "+ace);
        }
    }

    /**
     * Parse the config file, which is just a list of the text for which
     * buttons will be made.
     * @param configfile the file from which the initialization
     * information resides
     */
    private void parseConfigFile(File configfile) {
        if (! configfile.exists()) {
            System.err.println(getClass().getName() + " ERROR: " + configfile.getName() + " not found!");
            System.exit(1);
        }
        System.out.println(getClass().getName() + " using config file: " + configfile.getName());
        BufferedReader br;
        try {
            FileReader fr = new FileReader(configfile);
            br = new BufferedReader(fr);
        } catch (FileNotFoundException fnfe) {
            ClassLoader cl = this.getClass().getClassLoader();
            InputStream in = cl.getResourceAsStream(configfile.getPath().replace("\\","/"));
            InputStreamReader inr = new InputStreamReader(in);
            br = new BufferedReader(inr);
        }
        try {
            String str;
            JButton TmpButton;
            while ((str = br.readLine()) != null) {
                TmpButton = new JButton(str);
                TmpButton.setForeground(textColor);
                TmpButton.addActionListener(new InputListener());
                TmpButton.addMouseListener(new MouseInputListener());
                SPanel.add(TmpButton);
            }
        } catch (IOException ioe) {
            System.err.println("Error parsing config file: " + ioe);
        }
    }

    /** get initial size.  by default, just the preferred size of the panel
     * That said, sometimes the panel does not know how to determine its own
     * preferred size well, or the initial size might be set to something
     * entirely different.  If so, getInitSize(boolean isInternalWindow) or
     * getPreferredSize() or possibly both should definitely be overwritten.
     * If you want to distinguish between an initial size for a big-window
     * GUI versus as an internal frame inside the ADE SystemView, you should
     * likewise override this method.
     * @param isInternalWindow
     */
    @Override
    public Dimension getInitSize(boolean isInternalWindow) {
        Dimension minSize = this.getPreferredSize();
        int w = minSize.width;
        int h = minSize.height;
        while (SFrame == null) {
            Sleep(100);
        }
        Insets i = SFrame.getInsets();
        w += i.left + i.right;
        h += i.top + i.bottom;
        return new Dimension(w, h);
    }

    /**
     * Constructs the SimSpeechRecognitionComponentVis
     */
    public SimSpeechRecognitionComponentVis(ADEGuiCallHelper callHelper) {
        super(callHelper, 100); // 

        // need to: get config file name, text color, useCommand, useUnk, etc.
        try {
            textColor = (Color)callComponent("getTextColor");
        } catch (Exception ace) {
            System.err.println("SSPSVis: error getting button text color: "+ace);
            textColor = Color.BLACK;
        }
        try {
            SConfig = (String)callComponent("getConfigFile");
        } catch (Exception ace) {
            System.err.println("SSPSVis: error getting config file: "+ace);
            SConfig = "com/simspeech/config/default";
        }
        try {
            boolean[] flags = (boolean[])callComponent("getVisFlags");
            useCommand = flags[0];
            useUnk = flags[1];
            toLower = flags[2];
        } catch (Exception ace) {
            System.err.println("SSPSVis: error getting visualization flags: "+ace);
        }

        // Create frame and panel
        SPanel = new JPanel(new GridLayout(0, 1));

        // Add buttons from config file
        parseConfigFile(new File(SConfig));

        if (useCommand) {
            // Add text field and button
            SCommandText = new JTextField(2);
            SCommandText.addActionListener(new InputListener());
            SPanel.add(SCommandText);
            SCommandButton = new JButton("Command");
            SCommandButton.setForeground(textColor);
            SCommandButton.addActionListener(new InputListener());
            SCommandButton.addMouseListener(new MouseInputListener());
            SPanel.add(SCommandButton);
        }
        if (useUnk) {
            SUnkButton = new JButton("<unk>");
            SUnkButton.addActionListener(new InputListener());
            SUnkButton.addMouseListener(new MouseInputListener());
            SPanel.add(SUnkButton);
	}

        // Put it all together
        this.add(SPanel, BorderLayout.CENTER);
        new Thread() {
            @Override
            public void run() {
                SFrame = findParentFrame();
                while (SFrame == null) {
                    Sleep(100);
                    SFrame = findParentFrame();
                }
            }
        }.start();

    }
}
