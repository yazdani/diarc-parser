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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class Util {

    public static boolean emptyOrNullString(String string) {
        return ((string == null) || (string.length() == 0));
    }

    /**
     * Convenience method that constructs an ID from type and name. Equivalent
     * to the {@link ade.ADEComponentInfo#getKey getKey} method in
     * {@link ade.ADEComponentInfo ADEComponentInfo}, for use when such an object does
     * not yet exist.
     *
     * Copied from ADEComponentImpl, as wan't sure if there were any repercussions
     * to making that method static.
     *
     * @param type the component type
     * @param name the component name
     * @return string concatenation <tt>type$name</tt>
     */
    public static String getKey(String type, String name) {
        StringBuilder sb = new StringBuilder(type);
        sb.append("$");
        sb.append(name);
        return sb.toString();
    }

    /**
     * Convenience method that extracts the component type from an ID (works for an
     * RMI string also).
     *
     * Copied from ADEComponentImpl, as wan't sure if there were any repercussions
     * to making that method static.
     *
     * @param id the ID
     * @return the type
     */
    public static String getTypeFromID(String id) {
        return (id.substring(0, id.indexOf("$")));
    }

    /**
     * Convenience method that extracts the component name from an ID (works for an
     * RMI string also).
     *
     * Copied from ADEComponentImpl, as wan't sure if there were any repercussions
     * to making that method static.
     *
     * @param id the ID
     * @return the name
     */
    public static String getNameFromID(String id) {
        return (id.substring(id.indexOf("$") + 1));
    }

    public static String parenthetical(String text) {
        return "(" + text + ")";
    }

    public static String quotational(String text) {
        return "\"" + text + "\"";
    }

    /**
     * Performs a wildcard matching for the text and pattern provided.
     *
     * A really nifty and simple implemenatation, used with permission from
     * http://www.adarshr.com/papers/wildcard.
     *
     * @param text the text to be tested for matches.
     *
     * @param pattern the pattern to be matched for. This can contain the
     * wildcard character '*' (asterisk).
     *
     * @return <tt>true</tt> if a match is found, <tt>false</tt> otherwise.
     */
    public static boolean wildCardMatch(String text, String pattern) {
        // convert to all lowercase, both text and pattern:
        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        // Create the cards by splitting using a RegEx. If more speed 
        // is desired, a simpler character based splitting can be done.
        String[] cards = pattern.split("\\*");

        // Iterate over the cards.
        for (String card : cards) {
            int idx = text.indexOf(card);

            // Card not detected in the text.
            if (idx == -1) {
                return false;
            }

            // Move ahead, towards the right of the text.
            text = text.substring(idx + card.length());
        }

        return true;
    }

    public static String stackTraceString(Exception e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        e.printStackTrace(new PrintWriter(out, true)); // true = automatic line flushing.
        return out.toString();
    }

    /**
     * breaks a string into a multi-line string
     */
    public static String breakStringIntoMultilineString(String message) {
        int APPROXIMATE_CHAR_COUNT_BEFORE_BREAK = 70;
        int count = 0;
        char[] chars = message.toCharArray();
        for (int i = 0; i < message.length(); i++) {
            count++;
            if (chars[i] == '\n') {
                count = 0;
            } else if (count > APPROXIMATE_CHAR_COUNT_BEFORE_BREAK) {
                if (chars[i] == ' ') {
                    chars[i] = '\n';
                    count = 0;
                }
            }
        }
        return new String(chars);
    }
}