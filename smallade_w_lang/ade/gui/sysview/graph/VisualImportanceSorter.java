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

import prefuse.Visualization;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.ItemSorter;

public class VisualImportanceSorter extends ItemSorter {

    @Override
    public int score(VisualItem item) {
        // hover items > highlighted items > root > SEARCH_ITEMS set > normal VisualItem instances
        //  (within normal ones, NODES with non-regular color are ranked higher)

        if (item.isHover()) {
            return 27;
        }
        if (item.isHighlighted()) {
            return 26;
        }
        if (item.isInGroup(Visualization.FOCUS_ITEMS)) {
            return 25;
        }
        if (item.isInGroup(Visualization.SEARCH_ITEMS)) {
            return 24;
        }


        if (item instanceof NodeItem) {
            if (item.getFillColor() != NodeFillColorAction.COMPONENT_REGULAR_NODE_COLOR) {
                return 22;
            }
        }

        int temp = 0;

        // if haven't quit yet, just regular node or edge
        if (item instanceof NodeItem) {
            int depth = ((NodeItem) item).getDepth();
            temp = 20 - depth;
        } else if (item instanceof EdgeItem) {
            int depth = Math.min(((EdgeItem) item).getSourceNode().getDepth(),
                    ((EdgeItem) item).getTargetNode().getDepth());
            temp = 10 - depth;
        }

        if (temp < 0) {
            temp = 0;
        }


        return temp;
    }
}
