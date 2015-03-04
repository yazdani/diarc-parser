/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimSpeechProductionComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.simspeech;

import com.interfaces.*;
import java.awt.Color;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface SimSpeechProductionComponent extends SpeechProductionComponent {
    /**
     * Get the most recent text (if available).
     * @param ts the timestamp for the most recent retrieved text
     * @return if the current text is newer than ts, a list with the new ts and text; null otherwise
     */
    public ArrayList<Object> getText(long ts) throws RemoteException;

    /**
     * Get the color specified for text (mostly useful for the visualizer).
     * @return the color text should be displayed in
     */
    public Color getTextColor() throws RemoteException;

    /**
     * Get the display geometry (mostly useful for the visualizer).
     * @return the x,y screen coordinates for the output frame
     */
    public int[] getInitialLoc() throws RemoteException;

    /**
     * Register/deregister local GUI (mostly useful for the visualizer).  If useGui
     * is false, speech output will be sent to the terminal.
     * @param useGui true if local visualizer present, false otherwise
     */
    public void setLocalGui(boolean useGui) throws RemoteException;
}
