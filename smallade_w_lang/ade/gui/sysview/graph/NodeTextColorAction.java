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
import prefuse.visual.VisualItem;

public class NodeTextColorAction extends ColorAction {

    private static int BLACK_COLOR = ColorLib.gray(0);
    private static int WHITE_COLOR = ColorLib.gray(255);
    private ComponentGraphDisplay componentGraphDisplay;

    public NodeTextColorAction(String group, ComponentGraphDisplay componentGraphDisplay) {
        super(group, VisualItem.TEXTCOLOR);
        this.componentGraphDisplay = componentGraphDisplay;
    }

    @Override
    public int getColor(VisualItem item) {
        boolean searchingForMe = this.componentGraphDisplay.searchMatches(item);
        if (searchingForMe) {
            return BLACK_COLOR; // will look better on the highlighting.
        }

        // return white black color, for everything except really dark labels (like registry node color),
        //    for which should return white)
        if (item.isHighlighted()) {
            return BLACK_COLOR; // black will look best with highlighting
        }

        NodeVisualizeEnum visEnum = (NodeVisualizeEnum) item.get(ComponentGraphDisplay.CATEGORY_VISUALIZE_ENUM);
        if (visEnum == NodeVisualizeEnum.REGISTRY) {
            return WHITE_COLOR;
        } else if (visEnum == NodeVisualizeEnum.COMPONENT_REGULAR) {
            return BLACK_COLOR;
        } else {
            System.out.println("Could not determine color for node text of type " + visEnum);
            return BLACK_COLOR;
        }
    }
}
