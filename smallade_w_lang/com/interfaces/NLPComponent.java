/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * NLPComponent.java
 */
package com.interfaces;

import ade.*;
import java.rmi.*;

import java.util.ArrayList;

/**
 * NLPComponent is the base set of methods for natural language processing in
 * ADE.
 */
public interface NLPComponent extends ADEComponent {

    /** Accumulates text, word-at-a-time
     *  @param incoming an arraylist containing the sentence-to-date
     *    this will be matched against discourse's records to determine changes
     *  @return true if sentence is understood. */
    public boolean addWords(ArrayList<String> incoming) throws RemoteException;

    /** Accumulates text, sentence-at-a-time
     *  @return true if sentence is understood. */
    public boolean addUtterance(String incoming) throws RemoteException;

    /** Speaks appropriate text
     * @param text the text to be spoken
     * @param useAffect whether or not to use affective output
     * @param wait whether or not to block until speaking call returns
    @MethodConditions(
        Preconditions = {"have(com.interface.SpeechProductionComponent)"},
        Postconditions = {"said(text)"}
    )
    public boolean sayText(final String text, boolean useAffect, boolean wait) throws RemoteException;
     */

    /** Set the discourse's current actor.
     * @param name the new actor's name
     */
    public void setActor(String name) throws RemoteException;

    /** Set the discourse's current interactor.
     * @param name the new interactor's name
     */
    public void setInteractor(String name) throws RemoteException;
}						
