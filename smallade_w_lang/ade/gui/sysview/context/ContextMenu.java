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
package ade.gui.sysview.context;

import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.sysview.ADESystemView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

public class ContextMenu extends JPopupMenu {

    private static final long serialVersionUID = 1L;
    private ContextMenuPanel contextMenuPanel;
    private ContextMenu meTheContextMenu = this;
    private boolean madeAtLeastOneSelection = false; // flag, to know whether or not
    //    to make the menu disappear on losing mouse focus.
    private MouseAdapter mouseExitedContextMenuListener;

    public ContextMenu(ADESystemView systemView, String componentID) {
        this.mouseExitedContextMenuListener = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (!meTheContextMenu.contains(SwingUtilities.convertPoint(
                        (Component) e.getSource(), e.getPoint(), meTheContextMenu))) {
                    // looks like truly did exit.  So, check if should close it:
                    if (madeAtLeastOneSelection) {
                        meTheContextMenu.setVisible(false);
                    }
                }
            }
        };


        JPanel header = new JPanel(new BorderLayout());

        JButton close = new JButton("X");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                meTheContextMenu.setVisible(false);
                // hide the menu.  Hopefully this will dispose it as well, because
                //     it looks like context menus do not have a "dispose" method.
            }
        });
        header.add(close, BorderLayout.EAST);

        // only display name in title, otherwise really lengthy and not all that helpful!
        JLabel titleLabel = new JLabel(Util.getNameFromID(componentID));

        titleLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 0, 5));
        titleLabel.setFont(new Font(titleLabel.getFont().getName(),
                Font.BOLD, titleLabel.getFont().getSize()));
        titleLabel.setBackground(Color.BLUE);
        header.add(titleLabel, BorderLayout.CENTER);


        this.add(header);

        this.add(new JSeparator());

        this.contextMenuPanel = new ContextMenuPanel(this, systemView, componentID);

        this.add(contextMenuPanel);


        this.addMouseListener(this.mouseExitedContextMenuListener);

        for (Component eachComponent : UtilUI.getAllComponents(this)) {
            eachComponent.addMouseListener(this.mouseExitedContextMenuListener);
        }
    }

    public void madeSelection() {
        this.madeAtLeastOneSelection = true;
    }
}
