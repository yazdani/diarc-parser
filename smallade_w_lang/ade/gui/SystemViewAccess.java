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
 * data structure to to hold access information for the registry.
 */
public class SystemViewAccess implements Serializable {

    private static final long serialVersionUID = 1L;
    private String userID;
    private String password;

    public SystemViewAccess(String userID, String password) {
        this.userID = userID;
        this.password = password;
    }

    public String getUserID() {
        return userID;
    }

    public String getPassword() {
        return password;
    }
}
