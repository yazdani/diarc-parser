/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * SpeechRecognitionComponent.java
 */
package com.interfaces;

import ade.ADEComponent;
import java.rmi.*;

/**
Defines a standard set of methods that must be defined in an
ADEComponent to get the text resulting from speech recognition.

Naturally, it is not required to conform or even to use the
interfaces. However, by explicitly declaring them, it acts
as a guarantee that robot control code will work mostly as
expected no matter what ADEComponent it is connected with.
 */
public interface SpeechRecognitionComponent extends ADEComponent {

    /** Gets the most recently recognized text.
     * @return The most recently recognized text */
    public String getText() throws RemoteException;
}						
