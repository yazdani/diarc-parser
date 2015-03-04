/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * VoiceProsodyComponent.java
 */
package com.interfaces;

import ade.ADEComponent;
import java.rmi.*;

public interface VoiceProsodyComponent extends ADEComponent {
	/**
	 * sets prosody to STRESS ANGER or CONFUSION
     *
     * @param newEmo the new prosody to be used
	 */
	public void setEmotion(String newEmo) throws RemoteException;

	/**
	 * returns the current emotion in use
	 */
	public String getEmotion()throws RemoteException;

    public String getVoice() throws RemoteException;

    public void setVoice(String v) throws RemoteException;

}						
