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
package ade.gui.sysview.windows;

import java.util.Iterator;
import javax.swing.JInternalFrame;

/**
 * An interface in order to encapsulate the Window-Creation "lambda"
 */
public class Lambdas {

    public interface WindowCreator {

        JInternalFrame createWindow() throws Exception;
    }

    public interface WindowIterator {

        Iterator<? extends JInternalFrame> getWindows();
    }
}
