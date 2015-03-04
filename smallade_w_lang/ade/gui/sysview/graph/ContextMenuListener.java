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
package ade.gui.sysview.graph;

import ade.gui.sysview.ADESystemView;
import ade.gui.sysview.context.ContextMenu;
import java.awt.event.MouseEvent;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;

public class ContextMenuListener extends ControlAdapter {

    private ADESystemView sysView;
    private ComponentGraphDisplay componentGraphDisplay;

    public ContextMenuListener(ADESystemView sysView, ComponentGraphDisplay componentGraphDisplay) {
        this.sysView = sysView;
        this.componentGraphDisplay = componentGraphDisplay;
    }

    @Override
    public void itemClicked(VisualItem item, MouseEvent e) {
        if (UILib.isButtonPressed(e, Control.RIGHT_MOUSE_BUTTON)
                && e.getClickCount() == 1) {
            if (item.getGroup().equals(ComponentGraphDisplay.treeNodes)) {
                String componentID = item.getString(ComponentGraphDisplay.CATEGORY_ID);
                ContextMenu contextMenu = new ContextMenu(sysView, componentID);
                contextMenu.show(componentGraphDisplay, e.getX(), e.getY());
            }
        }
    }
}