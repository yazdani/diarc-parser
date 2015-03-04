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

public class NodeFillColorAction extends ColorAction {

    public static final int HOVER_COLOR = ColorLib.rgba(220, 75, 35, ComponentGraphDisplay.DEFAULT_OPACITY);
    public static final int COMPONENT_REGULAR_NODE_COLOR = ColorLib.rgba(255, 255, 255, ComponentGraphDisplay.DEFAULT_OPACITY);
    public static final int REGISTRY_NODE_COLOR = ColorLib.rgba(70, 124, 230, ComponentGraphDisplay.DEFAULT_OPACITY);
    public static final int SEARCH_COLOR = ColorLib.rgb(168, 221, 34); // with search, don't want
    //     any opacity at all, want the elements to really stand out.
    private ComponentGraphDisplay componentGraphDisplay;

    public NodeFillColorAction(String group, String field, ComponentGraphDisplay componentGraphDisplay) {
        super(group, field);
        this.componentGraphDisplay = componentGraphDisplay;
    }

    @Override
    public int getColor(VisualItem item) {
        // do the usual color helper stuff -- UNLESS the word matches the search filter:
        boolean searchingForMe = this.componentGraphDisplay.searchMatches(item);
        if (searchingForMe) {
            return SEARCH_COLOR;
        } else {
            return getColorHelper(item);
        }
    }

    // used both by this class for the fill, and by the NodeBorderColorAction.
    public static int getColorHelper(VisualItem item) {
        if (item.isHover()) {
            return HOVER_COLOR;
        }

        if (item.isHighlighted()) {
            return ComponentGraphDisplay.HIGHLIGHTING_COLOR;
        }

        NodeVisualizeEnum visEnum = (NodeVisualizeEnum) item.get(ComponentGraphDisplay.CATEGORY_VISUALIZE_ENUM);
        if (visEnum == NodeVisualizeEnum.REGISTRY) {
            return REGISTRY_NODE_COLOR;
        } else if (visEnum == NodeVisualizeEnum.COMPONENT_REGULAR) {
            return COMPONENT_REGULAR_NODE_COLOR;
        } else {
            System.out.println("Could not determine color for node of type " + visEnum);
            return ColorLib.red(255);
        }
    }
}