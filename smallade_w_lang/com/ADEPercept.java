/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * ADEPercept.java
 *
 * @author Paul Schermerhorn
 *
 */

package com;

import java.awt.Color;
import java.io.Serializable;
import java.util.UUID;

/**
 * <code>ADEPercept</code> is a container for the perceptual information
 * typically needed by users.
 */
public class ADEPercept implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** The object's name */
    public String name;
    /** The object's type */
    public String type;
    /** The object's color, in text form */
    public String color;
    /** The object's color, in terms of awt.Color */
    public Color colorObject;
    public String inside = null;
    /** The open/closed state, true if type is box and it is open */
    public boolean open = false;
    /** The filled/unfilled state, true if type is box and it contains a block */
    public boolean filled = false;
    /** The relative heading of the object from the robot. */
    public double heading;
    /** The relative distance of the object from the robot. */
    public double distance;
    /** The global x coordinate, if available */
    public double sim_x;
    /** The global y coordinate, if available */
    public double sim_y;
    /** The global x coordinate of a point near the object, if available */
    public double sim_px;
    /** The global y coordinate of a point near the object, if available */
    public double sim_py;
    /** The width, if available */
    public double sim_w;
    /** The height, if available */
    public double sim_h;
    /** Timestamp */
    public long time = 0;
    /** Unique ID **/
    public UUID id;

    /**
     * Base constructor for ADEPercepts.
     * @param n name
     * @param t type
     * @param h heading
     * @param d distance
     * @param x global x coordinate
     * @param y global y coordinate
     * @param px global pseudo-x coordinate
     * @param py global pseudo-y coordinate
     * @param w width
     * @param b height
     */
    public ADEPercept(String n, String t, double h, double d, double x, double y, double px, double py, double w, double b) {
        name = n;
        type = t;
        heading = h;
        distance = d;
        sim_x = x;
        sim_y = y;
        sim_px = px;
        sim_py = py;
        sim_w = w;
        sim_h = b;
        time = System.currentTimeMillis();
    }

    /**
     * Constructor for ADEPercepts like blocks.
     * @param n name
     * @param t type
     * @param c color
     * @param i name of object containing this one
     * @param h heading
     * @param d distance
     * @param x global x coordinate
     * @param y global y coordinate
     * @param px global pseudo-x coordinate
     * @param py global pseudo-y coordinate
     * @param w width
     * @param b height
     */
    public ADEPercept(String n, String t, String c, String i, double h, double d, double x, double y, double px, double py, double w, double b) {
        this(n, t, h, d, x, y, px, py, w, b);
        color = c;
        inside = i;
    }

    /**
     * Constructor for ADEPercepts like boxes.
     * @param n name
     * @param t type
     * @param c color
     * @param o open/closed state
     * @param f fill state (i.e., does it contain a block?)
     * @param h heading
     * @param d distance
     * @param x global x coordinate
     * @param y global y coordinate
     * @param px global pseudo-x coordinate
     * @param py global pseudo-y coordinate
     * @param w width
     * @param b height
     */
    public ADEPercept(String n, String t, String c, boolean o, boolean f, double h, double d, double x, double y, double px, double py, double w, double b) {
        this(n, t, c, null, h, d, x, y, px, py, w, b);
        filled = f;
        open = o;
    }

    public String toString() {
        StringBuilder s = new StringBuilder("ADEPercept ");
        s.append("name: ");
        s.append(name);
        s.append("   ");
        s.append("type: ");
        s.append(type);
        s.append("   ");
        s.append("color: ");
        s.append(color);
        s.append("   ");
        s.append("filled: ");
        s.append(filled);
        s.append("   ");
        s.append("inside: ");
        s.append(inside);
        s.append("   ");
        s.append("heading: ");
        s.append(heading);
        s.append("   ");
        s.append("distance: ");
        s.append(distance);
        s.append("   ");
        s.append("sim_x: ");
        s.append(sim_x);
        s.append("   ");
        s.append("sim_y: ");
        s.append(sim_y);
        s.append("   ");
        s.append("width: ");
        s.append(sim_w);
        s.append("   ");
        s.append("height: ");
        s.append(sim_h);
        s.append("   ");
        s.append("ID: ");
        s.append(id);

        return s.toString();
    }
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
