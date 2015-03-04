/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 * Last update: April 2010
 *
 * ADEState.java
 */
package ade;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Map;
// Not sure if I want to include this...
//import java.io.FileWriter;
//import java.io.IOException;

/**
Provides a common receptacle, used to periodically store the state of an
{@link ade.ADEComponentImpl ADEComponentImpl} and passed to the {@link
ade.ADERegistryImpl ADERegistryImpl} with which it is registered for later
restoration in case a failure occurs. The actual data storage and retrieval
are left up to the implementer (using the <tt>protected</tt> {@link
ade.ADEComponentImpl#saveState saveState} and {@link ade.ADEComponentImpl#loadState
loadState} methods).
<p>
Note that this class uses a <tt>ConcurrentHashMap</tt> as its backing store,
<b>not</b> a standard <tt>HashMap</tt>. This avoids synchronization hazards
at the cost of disallowing both keys and values to be <tt>null</tt>. By
default, state saving is disabled. To enable it, use the {@link
ade.ADEComponentImpl#enableState enableState} method.

@author Jim Kramer
 */
public class ADEState implements Serializable {

    private static final String prg = "ADEState";
    // included so we can later read files even if the class changed...
    private static final long serialVersionUID = 7526472295622776147L;
    /** The timestamp. */
    private long timestamp;
    /** The {@link java.util.HashMap HashMap} in which the state of fields
     * is saved. The key is the name of a field, while the value is the value
     * (stored as an object). */
    private ConcurrentHashMap<String, Object> state;

    /** Constructor. */
    public ADEState() {
        state = new ConcurrentHashMap<String, Object>();
        timestamp = System.currentTimeMillis();
    }

    /** Sets the named <tt>tag</tt> to the specified value, adding the
     * storage slot if it does not yet exist.
     * @param tag The name of the slot
     * @param obj The initial value */
    public void setValue(String tag, Object obj) {
        // TODO: should probably enforce each object's serializability
        if (tag != null && obj != null) {
            state.put(tag, obj);
        }
    }

    /** Returns the value associated with the specified tag (which may be
     * <tt>null</tt>).
     * @return The <tt>Object</tt> associated with the specified tag; if
     *   no such <tt>tag</tt> exists, <tt>null</tt> is returned */
    public Object getValue(String tag) {
        if (state.containsKey(tag)) {
            return state.get(tag);
        }
        return null;
    }

    /** Returns the timestamp value. */
    public final long getTimestamp() {
        return timestamp;
    }

    /** Sets the timestamp to the current system time. */
    public final void setTimestamp() {
        timestamp = System.currentTimeMillis();
    }

    /** Return the contents of the state HashMap in string form. Items will
     * appear, one to a line, in the format <tt>key[tab]value</tt>, where
     * <tt>value</tt> is simply a call to the object's <tt>toString</tt>
     * method. Items are not gauranteed to be in any particular order.
     * @return A single string containing one item per line */
    public final String getPrint() {
        StringBuilder sb = new StringBuilder();
        if (!state.isEmpty()) {
            Set<Map.Entry<String, Object>> eset = state.entrySet();
            for (Map.Entry<String, Object> entry : eset) {
                sb.append(entry.getKey());
                sb.append("\t");
                sb.append(entry.getValue());
                sb.append("\n");
            }
        } else {
            sb.append("No key/value pairs exist\n");
        }
        // take off the last (newline) character
        return sb.substring(0, (sb.length() - 1));
    }

    /** Print the string obtained from {@link #getPrint} to
     * <tt>System.out</tt>. */
    public final void print() {
        System.out.println(getPrint());
    }
    // TODO: not sure if we want this functionality...
    /** Write the state to a file for (more) permanent storage. This method
     * is empty and more a place holder than anything at this point, until a
     * decision is made as to its utility.
     * @param fwr The {@link java.io.FileWriter FileWriter} used to write
     *   the state out to an file
     * @throws IOException Passes on any exception from <tt>fwr</tt> */
    //public void writeState(FileWriter fwr) { }
}
