/**
 * ADE 1.0 (c) copyright Matthias Scheutz
 * @author Matthias Scheutz
 *
 * All rights reserved. Do not copy or use without permission. 
 * For questions regarding ADE, contact Matthias Scheutz at mscheutz@gmail.com
 * 
 * Last update: August 2012
 *
 * ADENotificationImpl.java
 */
package ade;

import java.io.*;
import java.util.Arrays;

public class ADENotification implements Serializable {
    String[] valuefunctions;
    String callbackfunctionname;
    String[][] conditions;
    Runnable runner;
    ADERemoteCallTimer rct;
    boolean active;

    public ADENotification(String[] valuefunctions,String[][] conditions,String callbackfunctionname) {
        this.valuefunctions = valuefunctions;
        this.conditions = conditions;
        this.callbackfunctionname = callbackfunctionname;
    }
    
    // two notifications are equal if they only differ w.r.t. the conditions
    @Override
    public boolean equals(Object noteobject) {
        if (!(noteobject instanceof ADENotification)) {
            return false;
        }
        ADENotification note = (ADENotification)noteobject;
        if (!note.callbackfunctionname.equals(callbackfunctionname)) {
            return false;
        }
        for(int i=0; i<note.valuefunctions.length; i++) {
            if (!note.valuefunctions[i].equals(valuefunctions[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Arrays.deepHashCode(this.valuefunctions);
        hash = 59 * hash + (this.callbackfunctionname != null ? this.callbackfunctionname.hashCode() : 0);
        return hash;
    }
}
