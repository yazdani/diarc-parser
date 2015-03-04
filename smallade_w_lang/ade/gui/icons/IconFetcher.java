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
package ade.gui.icons;

import java.net.URL;
import javax.swing.ImageIcon;

/**
 * A class to fetch icons, for use by the ADE SystemView GUI and by any other
 * visualizations. The icons themselves come from the open-source public-domain
 * Tango Icon Library project, http://tango.freedesktop.org/Tango_Icon_Library
 *
 */
public class IconFetcher {

    /**
     * returns an image icon based on the file name (with extension)
     */
    public static ImageIcon get16x16icon(String name) {
        URL imgURL = IconFetcher.class.getResource("size16/" + name);
        if (imgURL != null) {
            return new ImageIcon(imgURL, name);
        } else {
            return null;
        }
    }
}
