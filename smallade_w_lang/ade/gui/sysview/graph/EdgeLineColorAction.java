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
import prefuse.util.ColorLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;

public class EdgeLineColorAction extends ColorAction {

    private static final int REGISTRY_EDGE = ColorLib.rgba(200, 200, 200,
            (int) (ComponentGraphDisplay.DEFAULT_OPACITY * 0.6));
    private static final int NODE_EDGE_COLOR = ColorLib.rgba(255, 255, 255,
            ComponentGraphDisplay.DEFAULT_OPACITY);

    public EdgeLineColorAction(String group, String field) {
        super(group, field);
    }

    @Override
    public int getColor(VisualItem item) {
        return getDefultEdgeColor(item);
    }

    public static int getDefultEdgeColor(VisualItem item) {
        if (item.isHighlighted()) {
            return ComponentGraphDisplay.HIGHLIGHTING_COLOR;
        }

        if (EdgeCustomRenderer.isRegistryEdge((EdgeItem) item)) {
            return REGISTRY_EDGE;
        } else {
            return NODE_EDGE_COLOR;
        }
    }
}
