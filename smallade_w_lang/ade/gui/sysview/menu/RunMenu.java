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

import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import ade.gui.sysview.dialogs.ComponentLauncherWindow;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class RunMenu extends JMenu {

    private static final long serialVersionUID = 1L;
    private ADESystemView sysView;

    public RunMenu(ADESystemView sysView) {
        super("Run");

        this.sysView = sysView;

        this.add(createRunNewComponentMenuItem());
    }

    private JMenuItem createRunNewComponentMenuItem() {
        JMenuItem runNewComponentItem = new JMenuItem("Run new component", IconFetcher.get16x16icon("start-here.png"));
        runNewComponentItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComponentLauncherWindow componentLauncher = new ComponentLauncherWindow(sysView);
                sysView.createWindowForHelperPanel(componentLauncher,
                        new Dimension(500, 350), true, true, "Component Launcher",
                        JFrame.DISPOSE_ON_CLOSE);
            }
        });
        return runNewComponentItem;
    }
}
