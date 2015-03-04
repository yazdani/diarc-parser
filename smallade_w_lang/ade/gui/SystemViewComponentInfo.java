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
import java.util.HashSet;

/**
 * datastructure for information that the SystemView GUI may want to know from
 * the registry (a much more limited, and hence more network-transportable
 * subset of info than what is present in the ADEComponentInfo file)
 */
public class SystemViewComponentInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    public String name;
    public String type;
    public String host;
    public String registryName;
    public String startDirectory;
    public HashSet<String> clients; // connections are mutual
    //     so if visualizing every component, can visualize either clients or components.
    //     I'm choosing to visualize the clients, i.e., things that I can call 

    public String getComponentID() {
        return Util.getKey(this.type, this.name);
    }
}
