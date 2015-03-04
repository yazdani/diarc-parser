/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: March 2012
 *
 * SpeechProcComponent.java
 */
package com.interfaces;

import ade.ADEComponent;
import java.rmi.*;

/**
Defines a standard set of functions implemented in a server that needs 
to process speech input that a speech recognizer can call.
 */
public interface SpeechProcComponent extends ADEComponent {
    /** Called to indicate that speech recognition has commenced (so e.g., 
	a dialog manager will know not to start talking now */
    public void onsetDetected() throws RemoteException;
    /** Called to indiciate that speech recognition has stopped (so e.g., 
	a dialog manager can safely begin talking */
    public void offsetDetected() throws RemoteException;
}