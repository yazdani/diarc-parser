/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 *
 * Copyright 1997-2013 Matthias Scheutz and the HRILab Development Team
 * All rights reserved.  For information or questions, please contact
 * the director of the HRILab, Matthias Scheutz, at mscheutz@gmail.com
 * 
 * Redistribution and use of all files of the ADE package, in source and
 * binary forms with or without modification, are permitted provided that
 * (1) they retain the above copyright notice, this list of conditions
 * and the following disclaimer, and (2) redistributions in binary form
 * reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR ANY
 * OF THE CONTRIBUTORS TO THE ADE PROJECT BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.

 * Note: This license is equivalent to the FreeBSD license.
 */
package ade;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Utility class for passing minimal information (a subset of that contained in
 * the {@link ade.ADEComponentInfo ADEComponentInfo} class) back and forth between
 * components. Mostly for use to pass information from a component to an
 * {@link ade.ADERegistryImpl ADERegistryImpl} for use in the
 * {@link ade.ADEComponentInfo#update} method).
 */
public class ADEMiniComponentInfo implements Serializable {

    private static String prg = "ADEMiniComponentInfo";
    // TODO: id should be split into separate type/name fields
    /**
     * Component id (in <tt>type$name</tt> format).
     */
    public String id;
    public HashSet<String> userAccess;
    public HashSet<String> onlyonhosts;
    public int recoveryMultiplier = 1;
    public ADEGlobals.ComponentState state;
    public ADEGlobals.RecoveryState recState;
    public HashSet<String> clients;
    public HashSet<String> components;

    /**
     * Constructor that allows manual filling in of data.
     */
    public ADEMiniComponentInfo() {
        id = new String();
        userAccess = new HashSet<String>();
        onlyonhosts = new HashSet<String>();
        clients = new HashSet<String>();
        components = new HashSet<String>();
    }

    /**
     * Constructor with only the id, state, and recovery state.
     */
    public ADEMiniComponentInfo(String i, ADEGlobals.ComponentState s, ADEGlobals.RecoveryState rs) {
        id = i;
        state = s;
        recState = rs;
    }

    /**
     * Constructor with all information parameterized.
     */
    public ADEMiniComponentInfo(String i, HashSet<String> ua,
            HashSet<String> ooh, int rm,
            ADEGlobals.ComponentState st, ADEGlobals.RecoveryState rst,
            HashSet<String> cls, HashSet<String> svs) {
        //System.out.println(prg +": @@@@@@@@@ created for "+ i +", state="+ st +" with "+ cls.size() +" clients, "+ svs.size() +" components");
        id = i;
        userAccess = new HashSet<String>();
        for (String s : ua) {
            userAccess.add(s);
        }
        onlyonhosts = new HashSet<String>();
        for (String s : ua) {
            onlyonhosts.add(s);
        }
        recoveryMultiplier = rm;
        state = st;
        recState = rst;
        clients = new HashSet<String>();
        for (String s : cls) {
            clients.add(s);
        }
        components = new HashSet<String>();
        for (String s : svs) {
            components.add(s);
        }
        //System.out.println(prg +": @@@@@@@@@ exiting with "+ clients.size() +" clients, "+ components.size() +" components");
    }

    /**
     * Print the data contained herein.
     */
    public void print() {
        System.out.println(toString());
    }

    /**
     * Return the string representation of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(prg);
        sb.append(": ");
        sb.append(id);
        if (state != null) {
            sb.append("\n\tState: ");
            sb.append(state);
        }
        if (recState != null) {
            sb.append("\n\tRecovery State: ");
            sb.append(recState);
        }
        sb.append("\n\tRecovery mult: ");
        sb.append(recoveryMultiplier);
        if (clients != null) {
            sb.append("\n\tHave refs: [");
            if (clients.size() > 0) {
                for (String s : clients) {
                    sb.append(s);
                    sb.append(", ");
                }
                sb.setLength(sb.length() - 2);
            } else {
                sb.append("none");
            }
        }
        if (components != null) {
            sb.append("]\n\tGave refs: [");
            if (components.size() > 0) {
                for (String s : components) {
                    sb.append(s);
                    sb.append(", ");
                }
                sb.setLength(sb.length() - 2);
            } else {
                sb.append("none");
            }
        }
        sb.append("]\n");
        return sb.toString();
    }
}
