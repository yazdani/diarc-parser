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

import prefuse.action.assignment.ColorAction;
import prefuse.visual.VisualItem;

public class NodeBorderColorAction extends ColorAction {

    public NodeBorderColorAction(String group, String field) {
        super(group, field);
    }

    @Override
    public int getColor(VisualItem item) {
        if (item.isHover()) {
            return NodeFillColorAction.HOVER_COLOR;
        }

        return NodeFillColorAction.getColorHelper(item);
    }
}