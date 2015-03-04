/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: March 2012
 *
 * SRComponent.java
 */
package com.interfaces;

import ade.ADEComponent;
import java.util.*;
import java.rmi.*;

/**
Defines a standard set of functions implemented in a server that needs 
to process speech input that a speech recognizer can call.
 */
public interface SRComponent extends ADEComponent {
    /** Returns a list of speakers for which the SRComponent has 
	a speech model and can thus identify */
    public List<String> getAvailableSpeakers() throws RemoteException;
    
    ///** Send true to turn on inclusion of words the SR thinks exist 
    //    but is not able to decipher. False (default) to leave out such 
    //    tokens */
    //public void setIncludeUnknown(boolean includeUnknown) throws RemoteException;
    
    public int registerForWordReporting(String mytype, String myname) throws RemoteException;
    
    public int registerForOnsetOffsetReporting(String mytype, String myname) throws RemoteException;

}