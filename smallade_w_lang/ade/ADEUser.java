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
import java.security.*;
import java.util.*;

/**
 * The user representation in the ADE system.
 */
public class ADEUser implements Serializable {

    private String uid;         // the user id
    private String pass;        // the *encrypted* password
    private HashSet<String> userAccess; // user's access level -- MS: what is stored there???
    private boolean isAdmin;    // is the user an administrator?
    private boolean isComponent;   // is the user an ADEComponent?
    //static public enum User

    public ADEUser(String id, String p, HashSet<String> acc) {
        this(id, p, false, true, acc);
    }

    public ADEUser(String id, String p, boolean a, boolean s) {
        this(id, p, false, true, null);
        HashSet<String> acc = new HashSet<String>();
        if (a) {
            acc.add(ADEGlobals.ALL_ACCESS);
        }
        userAccess = acc;
    }

    /**
     * Create a user object.
     *
     * @param id the user id
     * @param p the (plaintext) password
     * @param a is the user an administrator?
     * @param s is the user an ADEComponent?
     * @param acc the user's access level
     */
    public ADEUser(String id, String p, boolean a, boolean s, HashSet<String> acc) {
        uid = id;
        pass = SHA1it(p);
        userAccess = acc;
        isAdmin = a;
        isComponent = s;
    }

    public String getUID() {
        return uid;
    }

    public void setUID(String id) {
        uid = id;
    }

    public String getPwd() {
        return pass;
    }

    public void setPassword(String p) {
        pass = SHA1it(p);
    }

    public boolean validPassword(String p) {
        return pass.equalsIgnoreCase(SHA1it(p));
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean b) {
        isAdmin = b;
    }

    public boolean isComponent() {
        return isComponent;  // cannot change once set
    }

    // restrict to package for now...
    // TODO: this should be access control via credentials
    HashSet<String> getAllowances() {
        return userAccess;
    }

    public boolean setUserAccess(HashSet<String> acc) {
        userAccess = acc;
        return (userAccess != null);
    }

    public boolean addUserAccess(String acc) {
        userAccess.add(acc);
        return (userAccess.contains(acc));
    }

    public void removeUserAccess(String acc) {
        if (userAccess.contains(acc)) {
            userAccess.remove(acc);
        }
    }

    /**
     * Computes the SHA-1 digest of a string and returns it as a hex-encoded
     * string.
     *
     * @param message The string to be digested
     * @return a <code>String</code> with the hex-encoded SHA-1 digest of the
     * message
     */
    protected static String SHA1it(String message) {
        MessageDigest encryptor;
        StringBuffer encrypted;
        byte[] bmsg = message.getBytes();
        boolean more2 = true;
        try {
            encryptor = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("This JVM stinks. Get one that implements SHA.");
            System.err.println("Error: " + nsae);
            return null;
        }
        for (int i = 0; i < bmsg.length; i++) {
            encryptor.update(bmsg[i]);
        }
        bmsg = encryptor.digest();
        encrypted = new StringBuffer();
        for (int i = 0; i < bmsg.length; i++) {
            int t = bmsg[i] & 0xFF; // make sure it's positive
            if (t <= 0x0F) {
                encrypted.append("0");
            }
            encrypted.append(Integer.toHexString(t));
        }
        return encrypted.toString();
    }
}
