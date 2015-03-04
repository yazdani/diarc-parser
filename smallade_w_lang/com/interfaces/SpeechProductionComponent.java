/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * SpeechProductionComponent.java
 */
package com.interfaces;

import ade.ADEComponent;
import java.rmi.*;

/**
Defines a standard set of methods that must be defined in an
ADEComponent to send text for speech production.

Naturally, it is not required to conform or even to use the
interfaces. However, by explicitly declaring them, it acts
as a guarantee that robot control code will work mostly as
expected no matter what ADEComponent it is connected with.
 */
public interface SpeechProductionComponent extends ADEComponent {
    /** Speaks appropriate text, blocking.
     * @param text the text to be spoken
     */
    public boolean sayText(String text) throws RemoteException;

    /** Speaks appropriate text
     * @param text the text to be spoken
     * @param wait whether or not to block until speaking call returns
     */
    public boolean sayText(String text, boolean wait) throws RemoteException;

    /** Checks if speech is being produced.
     * @return <tt>true</tt> if speech is being produced, <tt>false</tt>
     * otherwise
     * @throws RemoteException if an error occurs */
    public boolean isSpeaking() throws RemoteException;

    /** Stops an ongoing utterance.
     * @return <tt>true</tt> if speech is interrupted, <tt>false</tt>
     * otherwise.
     * @throws RemoteException if an error occurs */
    public boolean stopUtterance() throws RemoteException;
}						
