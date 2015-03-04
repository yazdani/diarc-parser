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

import java.awt.event.MouseEvent;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;

public class ReRootOnDoubleClickControl extends ControlAdapter {

    ComponentGraphDisplay myVisualization;

    public ReRootOnDoubleClickControl(ComponentGraphDisplay myVisualization) {
        this.myVisualization = myVisualization;
    }

    @Override
    public void itemClicked(VisualItem item, MouseEvent e) {
        if (UILib.isButtonPressed(e, Control.LEFT_MOUSE_BUTTON)
                && e.getClickCount() == 2) {
            myVisualization.reCenterAroundComponentNodeAndRunLayout(item);
        }
    }
}
