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
package ade.gui.sysview.menu;

import ade.gui.sysview.ADESystemView;
import javax.swing.JMenuBar;

public class MenuBar extends JMenuBar {

    private static final long serialVersionUID = 1L;
    public WindowMenu windowMenu;

    public MenuBar(final ADESystemView sysView) {
        this.add(new ConfigMenu(sysView));

        this.add(new RunMenu(sysView));

        windowMenu = new WindowMenu(sysView);
        this.add(windowMenu);

        this.add(new AboutMenu(sysView));
    }
}
