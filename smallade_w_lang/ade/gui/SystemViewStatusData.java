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
import java.util.HashMap;

/**
 * data structure for the ADESystemView to request a registry's status
 */
public class SystemViewStatusData implements Serializable {

    private static final long serialVersionUID = 1L;
    // map of IDs to SystemViewComponentInfo
    public HashMap<String, SystemViewComponentInfo> componentIDtoInfoMap =
            new HashMap<String, SystemViewComponentInfo>();
}
