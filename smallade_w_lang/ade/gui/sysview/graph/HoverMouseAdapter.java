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
import java.util.Iterator;
import javax.swing.JLabel;
import prefuse.controls.ControlAdapter;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

public class HoverMouseAdapter extends ControlAdapter {

    private JLabel titleLabel;
    private ComponentGraphDisplay myVisualization;

    public HoverMouseAdapter(ComponentGraphDisplay myVisualization, JLabel nodeTitleLabel) {
        super();
        this.myVisualization = myVisualization;
        this.titleLabel = nodeTitleLabel;
    }

    @Override
    public void itemEntered(VisualItem item, MouseEvent e) {
        HoveredEdgesCommon(item, true);
    }

    private void HoveredEdgesCommon(VisualItem item, boolean entered) {
        if (item.canGetString(ComponentGraphDisplay.CATEGORY_ID)) {
            if (item instanceof NodeItem) {
                String itemID = item.getString(ComponentGraphDisplay.CATEGORY_ID);

                if (itemID != null) { // a node:
                    // set title
                    if (entered) {
                        titleLabel.setText(itemID);
                    } else {
                        titleLabel.setText(null);
                    }

                    item.setHighlighted(entered);
                    setNeighborHighlight((NodeItem) item, entered);
                    myVisualization.runRepaintCommand();

                }
            }
        }
    }

    protected void setNeighborHighlight(NodeItem n, boolean state) {
        @SuppressWarnings("rawtypes")
        Iterator iter = n.edges();
        while (iter.hasNext()) {
            EdgeItem eitem = (EdgeItem) iter.next();
            NodeItem nitem = eitem.getAdjacentItem(n);

            eitem.setHighlighted(state);
            nitem.setHighlighted(state);
        }
    }

    @Override
    public void itemExited(VisualItem item, MouseEvent e) {
        HoveredEdgesCommon(item, false);
    }
}
