/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimSpeechInputComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.simspeech;

import ade.ADEComponent;
import java.awt.Color;
import java.rmi.RemoteException;

public interface SimSpeechInputComponent extends ADEComponent {
    /** Gets the most recently input wav file.
     * @return The most recently recognized text */
    public String getText() throws RemoteException;

    /**
     * Get the color specified for text (mostly useful for the visualizer).
     * @return the color text should be displayed in
     */
    public Color getTextColor() throws RemoteException;

    /**
     * Get the configuration file for input buttons (mostly useful for the visualizer).
     * @return the config file to use
     */
    public String getConfigFile() throws RemoteException;

    /**
     * Get the flags relevant to visualization (mostly useful for the visualizer).
     * Examples include whether to display the command box/button and the "unk" button.
     * @return an array of boolean flags
     */
    public boolean[] getVisFlags() throws RemoteException;

    /**
     * Set input text (mostly useful for the visualizer).
     * @param in the new speech input
     */
    public void setText(String in) throws RemoteException;

    /**
     * Send text to be logged (mostly useful for the visualizer).
     * @param log the text to be logged
     */
    public void logText(String log) throws RemoteException;
}
