/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * TemplateComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.template;

import ade.*;
import java.rmi.*;

/** <code>TemplateComponent</code>.  This is just a template from which new
 * servers can be constructed.  Copy this file and TemplateComponentImpl.java
 * to their new home, change the package name, search and replace the
 * class names, and fill in the server-specific details (i.e., remote interface
 * specification).
 */
public interface TemplateComponent extends ADEComponent {
    // Add whatever public interface calls you want other ADE servers to
    // have access to here, and implement them in your ComponentImpl.java.

    /**
     * An example of a remote call that fetches data from the server.
     *     Note that this remote call is used both by other servers and the
     *     server visualization -- e.g., there is no special interface
     *     that needs to be implemented for GUI-related methods.  
     * @return server name, followed by an incrementing counter (initially
     *     seeded to a random value, so that the output of the two servers
     *     is different) 
     */
    public String getComponentNameAndCounter() throws RemoteException;

}
// vi:ai:smarttab:expandtab:ts=8 sw=4
