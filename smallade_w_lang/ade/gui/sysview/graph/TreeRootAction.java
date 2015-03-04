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

import java.util.Iterator;
import prefuse.Visualization;
import prefuse.action.GroupAction;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.tuple.TupleSet;

/**
 * Switch the root of the tree by requesting a new spanning tree at the desired
 * root. This is a piece of prefuse magic, taken out of "Demonstration of a
 * node-link tree viewer" by <a href="http://jheer.org">jeffrey heer</a>
 */
public class TreeRootAction extends GroupAction {

    public TreeRootAction(String graphGroup) {
        super(graphGroup);
    }

    public void run(double frac) {
        TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
        if (focus == null || focus.getTupleCount() == 0) {
            return;
        }

        Graph g = (Graph) m_vis.getGroup(m_group);
        Node f = null;
        @SuppressWarnings("rawtypes")
        Iterator tuples = focus.tuples();
        while (tuples.hasNext() && !g.containsTuple(f = (Node) tuples.next())) {
            f = null;
        }
        if (f == null) {
            return;
        }
        g.getSpanningTree(f);
    }
}
