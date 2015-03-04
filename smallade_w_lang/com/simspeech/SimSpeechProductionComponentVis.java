/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimSpeechProductionComponentVis.java
 *
 * @author Paul Schermerhorn
 */
package com.simspeech;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import java.beans.PropertyVetoException;
import static utilities.Util.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/** <code>SimSpeechProductionComponentVis</code>.  Pops up a box for spoken text
 * instead of playing it to a sound device.
 */
public class SimSpeechProductionComponentVis extends ADEGuiPanel {
    private boolean verbose = false;
    private int tx;
    private int ty;
    private boolean isSpeaking = false;
    private Color textColor;

    private Container IFrame = null;
    private JTextArea IText;
    private double ITextWidth = 30;

    SimSpeechProductionComponentVis me;

    /* Called periodically by the ADEGui, but only if the frame is visible, so updates
     * have to be done in a separate thread (in Updater below).
     */
    @Override
    public void refreshGui() {
        this.repaint();
    }

    /**
     * The <code>Updater</code> is the main loop that does whatever this
     * server does.
     */
    private class Updater extends Thread {

        private boolean shouldRead;

        public Updater() {
            shouldRead = true;
        }

        @Override
        public void run() {
            long ts = 0;
            while (shouldRead) {
                try {
                    ArrayList<Object> ret = (ArrayList)callComponent("getText", ts);
                    if (ret != null) {
                        ts = (Long)ret.get(0);
                        String text = (String)ret.get(1);
                        //System.out.println("got text: "+text);
                        String[] words = text.split(" ");
                        long tmptime = 500 * words.length;
                        if (tmptime < 1000) {
                            tmptime = 1000;
                        } else if (tmptime > 4000) {
                            tmptime = 4000;
                        }
                        final long time = tmptime;
                        IText.setText(text);
                        isSpeaking = true;
                        iconify(false);
                        new Thread() {
                            @Override
                            public void run() {
                                // sleep, then hide
                                Sleep(time);
                                iconify(true);
                                isSpeaking = false;
                            }
                        }.start();

                    } else if (! isSpeaking) {
                        iconify(true);
                    }
                } catch (Exception ace) {
                    System.err.println("SSPSVis: error getting text: "+ace);
                }
                Sleep(200);
            }
        }

        public void halt() {
            shouldRead = false;
        }
    }

    private void iconify(boolean i) {
        /*
        if (IFrame == null) {
            IFrame = findParentFrame();
            if (IFrame != null && IFrame instanceof JFrame)
                IFrame.setLocation(tx, ty);
        }
        */
        if (IFrame != null) {
            if (IFrame instanceof JFrame) {
                //IFrame.setLocation(tx, ty);
                if (i) {
                    ((JFrame)IFrame).setVisible(false);
                    //((JFrame)IFrame).setExtendedState(Frame.ICONIFIED);
                } else {
                    IFrame.setLocation(tx, ty);
                    ((JFrame)IFrame).setVisible(true);
                    //IFrame.setLocation(tx, ty);
                    //((JFrame)IFrame).setExtendedState(Frame.NORMAL);
                }
            } else {
                try {
                    ((JInternalFrame)IFrame).setIcon(i);
                } catch (PropertyVetoException ex) {
                }
            }
        } else {
            //System.out.println("can't iconify!");
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
        while (IFrame == null) {
            Sleep(100);
        }
        Insets i = IFrame.getInsets();
        w += i.left + i.right;
        h += i.top + i.bottom;
        return new Dimension(w, h);
    }

    /**
     * SimSpeechProductionComponentVis constructor.
     */
    public SimSpeechProductionComponentVis(ADEGuiCallHelper callHelper) {
        super(callHelper, 5);

        //IFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //ITextPanel = new JPanel();

        try {
            textColor = (Color)callComponent("getTextColor");
            int[] loc = (int[])callComponent("getInitialLoc");
            tx = loc[0];
            ty = loc[1];
            //System.out.println("SSPSVis: setting location to: "+tx+","+ty);
        } catch (Exception ace) {
            System.err.println("SSPSVis: error getting initial parameters: "+ace);
            textColor = Color.BLACK;
            tx = ty = 0;
        }
        IText = new JTextArea();
        IText.setColumns((int) ITextWidth);
        IText.setFont(IText.getFont().deriveFont(0, 16.0f));
        IText.setLineWrap(true);
        IText.setWrapStyleWord(true);
        IText.setEditable(false);
        IText.setForeground(textColor);
        this.add(IText, BorderLayout.PAGE_START);
        this.setSize(this.getPreferredSize());
        me = this;
        new Thread() {
            @Override
            public void run() {
                IFrame = findParentFrame();
                while (IFrame == null) {
                    Sleep(100);
                    IFrame = findParentFrame();
                }
                IText.setBackground(IFrame.getBackground());
                if (IFrame instanceof JFrame) {
                    IFrame.setLocation(tx, ty);
                    try {
                        callComponent("setLocalGui", true);
                    } catch (Exception ace) {
                        System.err.println("SSPSVis: error getting initial parameters: "+ace);
                    }
                } else {
                    me.setSize(IText.getPreferredSize());
                }
            }
        }.start();
        Updater u = new Updater();
        u.start();
    }
}
