/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 * 
 * Copyright 1997-2013 Matthias Scheutz
 * All rights reserved. Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 */
package ade.gui;

import java.io.Serializable;

/**
 * simple data structure to hold boolean flags for what information the GUI
 * wants to know. That way, not sending oodles of stuff that the GUI doesn't
 * care about
 */
public class InfoRequestSpecs implements Serializable {

    private static final long serialVersionUID = 1L;
    // NAME and TYPE are always going to be returned in a SystemViewComponentInfo, so there's 
    //     not even a point to request them.
    public boolean host;
    public boolean registryName;
    public boolean startDirectory;
    public boolean clients; // other components that this component makes calls to.
}
