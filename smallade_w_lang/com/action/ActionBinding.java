/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionBinding.java
 *
 * Last update: November 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import java.io.*;

/**
 * An <code>ActionBinding</code> is an association between a variable name and a
 * value.  This is really a variable, insofar as it can be uninitialized
 * (i.e., have no value associated with it).  An uninitialized binding
 * consists of a name and a type, and may later be bound to an object.
 */
class ActionBinding implements Cloneable, Serializable {
    /** The name of this variable */
    public final String bName;
    /** The type of this variable (e.g., person, beverage) */
    public final String bType;
    /** This variable's value */
    private Object bValue;
    /** This variable's default value */
    public final Object bDefault;
    /** Local variable? */
    public final boolean bLocal;
    /** Return variable? */
    public final boolean bReturn;

    /**
     * A new unbound variable
     * @param name the name of this variable
     * @param type the type of this variable (e.g., person, beverage)
     * @param ret whether return values will be bound here
     */
    public ActionBinding(String name, String type, String def, boolean ret) {
        if (name.charAt(0) == '!') {
            bLocal = true;
            //bName = new String(name.substring(1));
        } else {
            bLocal = false;
            //bName = new String(name);
        }
        bName = name;
        if (type != null) {
            bType = type;
            bDefault = def;
        } else {
            bType = "entity";
            bDefault = def;
        }
        bReturn = ret;
    }

    /**
     * A new bound variable
     * @param name the name of this variable
     * @param type the type of this variable (e.g., person, beverage)
     * @param def the default value of this variable
     * @param value the value to which the variable is bound
     * @param ret whether return values will be bound here
     */
    public ActionBinding(String name, String type, String def, Object value, boolean ret) {
        if (name.charAt(0) == '!') {
            bLocal = true;
            //bName = new String(name.substring(1));
        } else {
            bLocal = false;
            //bName = new String(name);
        }
        bName = name;
        if (type != null) {
            bType = type;
            bDefault = def;
        } else {
            bType = "entity";
            bDefault = def;
        }
        bValue = value;
        bReturn = ret;
    }

    /**
     * Bind (or rebind) a variable
     * @param value the value to which the variable is bound
     */
    public void bind(Object value) {
        bValue = value;
    }

    /**
     * Bind (or rebind) a variable, recursing through bindings to the bottom
     * @param value the value to which the variable is bound
     */
    public void bindDeep(Object value) {
        // if bValue is an instance of "class binding"
        if (this.getClass().isInstance(bValue)) {
            ActionBinding tmpBinding = (ActionBinding)bValue;
            // Keep going
            //System.out.println("bindDeep: recursing from " + bName + " to bind " + value);
            //System.out.println("bindDeep: " + bName + " currently (deeply) bound to " + tmpBinding.getBindingDeep());
            //System.out.println("bindDeep: " + bName + " currently bound to " + getBinding());
            tmpBinding.bindDeep(value);
            //System.out.println("bindDeep: " + bName + " now (deeply) bound to " + tmpBinding.getBindingDeep());
        } else {
            //System.out.println("bindDeep: binding " + bName + " to " + value);
            bValue = value;
        }
    }

    /**
     * get the variable's binding
     * @return the value to which the variable is bound
     */
    public Object getBinding() {
        return bValue;
    }

    /**
     * get the variable's binding, recursing through bindings to the bottom
     * @return the value to which the variable is bound
     */
    public Object getBindingDeep() {
        ActionBinding tmpBinding;
        // If the object to which this is bound is an ActionBinding, recurse
        if (this.getClass().isInstance(bValue)) {
            tmpBinding = (ActionBinding)bValue;
            return tmpBinding.getBindingDeep();
        } else {
            return bValue;
        }
    }

    /**
     * get the variable's binding name, recursing through bindings to the
     * bottom
     * @return the name to which the variable is bound
     */
    public String getBindingNameDeep() {
        ActionBinding tmpBinding;
        // If the object to which this is bound is an ActionBinding, recurse
        if (this.getClass().isInstance(bValue)) {
            tmpBinding = (ActionBinding)bValue;
            return tmpBinding.getBindingNameDeep();
        } else {
            return bName;
        }
    }

    /**
     * get the variable's binding type, recursing through bindings to the
     * bottom
     * @return the type to which the variable is bound
     */
    public String getBindingTypeDeep() {
        ActionBinding tmpBinding;
        // If the object to which this is bound is an ActionBinding, recurse
        if (this.getClass().isInstance(bValue)) {
            tmpBinding = (ActionBinding)bValue;
            return tmpBinding.getBindingTypeDeep();
        } else {
            return bType;
        }
    }

    @Override
    public Object clone() {
        try {
            ActionBinding cloned = (ActionBinding)super.clone();

            return cloned;
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    @Override
    public String toString() {
        if (bValue == null) {
            return bName + " " + bType;
        } else if (this.getClass().isInstance(bValue)) {
            ActionBinding tmpBinding = (ActionBinding)bValue;
            return bName + " " + bType + " " +
         tmpBinding.bName;
        } else {
            return bName + " " + bType + " " + bValue;
        }
    }
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
