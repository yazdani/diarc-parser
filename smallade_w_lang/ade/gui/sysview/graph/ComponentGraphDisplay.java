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

import ade.ADERegistry;
import ade.gui.Util;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.PolarLocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.FontAction;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.search.PrefixSearchTupleSet;
import prefuse.data.search.SearchTupleSet;
import prefuse.data.tuple.DefaultTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;

/**
 * Component Visualization, in tree form, based on "Demonstration of a node-link
 * tree viewer" by <a href="http://jheer.org">jeffrey heer</a>
 */
public class ComponentGraphDisplay extends Display {

    private static final long serialVersionUID = 1L;
    // the categories that each node will store (for component id, name, and visualization enum)
    public static final String CATEGORY_ID = "id";
    public static final String CATEGORY_NAME = "name"; // store name separately,
    //    rather than re-generating it time and time again out of the id.
    public static final String CATEGORY_VISUALIZE_ENUM = "visEnum";
    static final String tree = "tree";
    public static final String treeNodes = "tree.nodes";
    static final String treeEdges = "tree.edges";
    static final String linear = "linear";
    static final String LABEL = "label";
    public static final int DEFAULT_OPACITY = 255; // no opacity
    public static final int HIGHLIGHTING_COLOR = ColorLib.rgba(255, 255, 0, DEFAULT_OPACITY);
    private LabelRenderer m_nodeRenderer;
    private EdgeCustomRenderer m_edgeRenderer;
    public ActionList animate = new ActionList(1250);
    public Graph myGraph;
    public HashMap<String, Node> myGraphNodes = new HashMap<String, Node>(); // map of IDs to Nodes.
    public HashSet<Edge> myGraphEdges = new HashSet<Edge>();
    public String centeredAround;
    private String searchKeyword;
    public static final String VISUALIZATION_RUN_WORD = "layout";
    RadialTreeLayout treeLayout;
    TreeRootAction myTreeRootAction;

    public ComponentGraphDisplay() {
        super(new Visualization());

        myGraph = new Graph(true); // DIRECTED graph (directed so that can show connections
        //    -- who has which clients).

        myGraph.addColumn(CATEGORY_ID, String.class);
        myGraph.addColumn(CATEGORY_NAME, String.class);
        myGraph.addColumn(CATEGORY_VISUALIZE_ENUM, NodeVisualizeEnum.class);

        // -- set up visualization --
        m_vis.add(tree, myGraph);
        m_vis.setInteractive(treeEdges, null, false);

        // -- set up renderers --
        m_nodeRenderer = new NodeRenderer();
        m_edgeRenderer = new EdgeCustomRenderer();

        DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
        rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
        m_vis.setRendererFactory(rf);

        // -- set up processing actions --

        // colors
        ItemAction nodeColorFill = new NodeFillColorAction(treeNodes, VisualItem.FILLCOLOR, this);
        ItemAction nodeColorLine = new NodeBorderColorAction(treeNodes, VisualItem.STROKECOLOR);
        ItemAction textColor = new NodeTextColorAction(treeNodes, this);

        ItemAction edgeColorLine = new EdgeLineColorAction(treeEdges, VisualItem.STROKECOLOR);
        ItemAction edgeColorArrows = new EdgeLineColorAction(treeEdges, VisualItem.FILLCOLOR);


        FontAction fonts = new FontAction(treeNodes,
                FontLib.getFont("Tahoma", 10));


        // recolor
        ActionList recolor = new ActionList();
        recolor.add(nodeColorFill);
        recolor.add(nodeColorLine);
        recolor.add(edgeColorLine);
        recolor.add(edgeColorArrows);
        recolor.add(textColor);
        m_vis.putAction("recolor", recolor);

        // repaint
        ActionList repaint = new ActionList();
        repaint.add(recolor);
        repaint.add(new RepaintAction());
        m_vis.putAction("repaint", repaint);

        // animate paint change
        ActionList animatePaint = new ActionList(1000);
        animatePaint.add(new ColorAnimator(treeNodes));
        animatePaint.add(new ColorAnimator(treeEdges));
        animatePaint.add(new RepaintAction());
        m_vis.putAction("animatePaint", animatePaint);

        // create the tree layout action
        treeLayout = new RadialTreeLayout(tree);
        treeLayout.setAutoScale(true);
        m_vis.putAction("treeLayout", treeLayout);

        CollapsedSubtreeLayout subLayout = new CollapsedSubtreeLayout(tree);
        m_vis.putAction("subLayout", subLayout);


        // create the main layout action
        ActionList layout = new ActionList();
        myTreeRootAction = new TreeRootAction(tree);
        layout.add(myTreeRootAction);
        layout.add(fonts);
        layout.add(treeLayout);
        layout.add(subLayout);
        layout.add(recolor);
        m_vis.putAction(VISUALIZATION_RUN_WORD, layout);

        // animated transition
        animate.add(new QualityControlAnimator());
        animate.add(new VisibilityAnimator(tree));
        animate.add(new PolarLocationAnimator(treeNodes, linear));
        animate.add(new ColorAnimator(treeNodes));
        animate.add(new ColorAnimator(treeEdges));
        animate.add(new RepaintAction());
        m_vis.putAction("animate", animate);
        m_vis.alwaysRunAfter(VISUALIZATION_RUN_WORD, "animate");

        // ------------------------------------------------

        // initialize the display
        setItemSorter(new VisualImportanceSorter());
        // ------------------------------------------------

        // maintain a set of items that should be interpolated linearly
        // this isn't absolutely necessary, but makes the animations nicer
        // the PolarLocationAnimator should read this set and act accordingly
        m_vis.addFocusGroup(linear, new DefaultTupleSet());
        m_vis.getGroup(Visualization.FOCUS_ITEMS).addTupleSetListener(
                new TupleSetListener() {
                    public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
                        TupleSet linearInterp = m_vis.getGroup(linear);
                        if (add.length < 1) {
                            return;
                        }
                        linearInterp.clear();
                        for (Node n = (Node) add[0]; n != null; n = n.getParent()) {
                            linearInterp.addTuple(n);
                        }
                    }
                });

        SearchTupleSet search = new PrefixSearchTupleSet();
        m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);
        search.addTupleSetListener(new TupleSetListener() {
            public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
                m_vis.cancel("animatePaint");
                runCommand("recolor");
                runCommand("animatePaint");
            }
        });
    }

    protected void runCommand(String action) {
        try {
            this.getVisualization().run(action);
        } catch (Exception e) {
            System.out.println("Error visualizing graph, hopefully this is just a chance error.");
        }
    }

    public void runLayoutCommand() {
        // perform layout
        runCommand(VISUALIZATION_RUN_WORD);
    }

    public void runRepaintCommand() {
        runCommand("repaint");
    }

    public void addComponentNode(String componentID) {
        // only add node if it doesn't already exist!
        if (!myGraphNodes.containsKey(componentID)) {
            Node node = myGraph.addNode();
            node.setString(CATEGORY_NAME, Util.getNameFromID(componentID));
            node.setString(CATEGORY_ID, componentID);

            NodeVisualizeEnum visualizeType;
            if (Util.getTypeFromID(componentID).equals(ADERegistry.class.getCanonicalName())) {
                visualizeType = NodeVisualizeEnum.REGISTRY;
            } else {
                visualizeType = NodeVisualizeEnum.COMPONENT_REGULAR;
            }

            node.set(CATEGORY_VISUALIZE_ENUM, visualizeType);
            myGraphNodes.put(componentID, node);
        }
    }

    public void addComponentEdge(String fromComponent, String toComponent) {
        boolean shouldAddEdge = true;
        try {
            if (myGraph.getEdge(myGraphNodes.get(fromComponent), myGraphNodes.get(toComponent)) != null) {
                shouldAddEdge = false;  // edge already exists, don't need to re-add it.
            }
        } catch (NullPointerException e) {
            // if either the graph nodes don't exist, or some other error, then there is no
            //    edge as far as I am concerned!
            // do nothing.  shouldAddEdge will take care of things.
        }

        if (shouldAddEdge) {
            Edge newEdge = myGraph.addEdge(myGraphNodes.get(fromComponent), myGraphNodes.get(toComponent));
            myGraphEdges.add(newEdge);
        }
    }

    public void reCenterAroundComponentNodeAndRunLayout(VisualItem item) {
        TupleSet ts = m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
        ts.setTuple(item);
        runCommand(VISUALIZATION_RUN_WORD);
    }

    public void reCenterAroundComponentNodeAndRunLayout(String componentID) {
        centeredAround = componentID;

        // Search through all the visual items and see if one of them is the one I'm looking for:
        //     This is, admittedly, not very efficient, but I couldn't for the life of me
        //     figure out how to find a VisualItem by its name. Given that this is not that
        //     often-used of an operation, I doubt that this should be a problem...
        @SuppressWarnings("rawtypes")
        Iterator items = m_vis.items(treeNodes);
        while (items.hasNext()) {
            VisualItem anItem = ((VisualItem) items.next());
            if (componentID.equals(anItem.getString(CATEGORY_ID))) {
                reCenterAroundComponentNodeAndRunLayout(anItem);
                return;
            }
        }

        // if still here, didn't find node
        System.out.println("Could not find node \"" + componentID + "\" for re-centering.");
    }

    @SuppressWarnings("unchecked")
    public void removeComponentNode(String componentID) {
        // BEGIN BY DELETING THE EDGES CONNECTING TO THIS NODE:

        Iterator<Edge> myIter = myGraphNodes.get(componentID).edges();
        HashSet<Edge> edgesToRemove = new HashSet<Edge>();
        // temporary store of edges to remove.  it would probably be possible
        //    to remove everything directly from the iterator, but it feels safer
        //    to first catalog the edges, and then remove them (so as not to 
        // 	  accidentally modify the iterator list while en route).

        while (myIter.hasNext()) {
            edgesToRemove.add(myIter.next());
        }

        removeComponentEdges(edgesToRemove);


        // NOW REMOVE THE NODE ITSELF, AND ANY MENTION OF IT

        Node nodeToRemove = myGraphNodes.remove(componentID);
        myGraph.removeNode(nodeToRemove);
    }

    public void removeComponentEdge(String from, String to) {
        Edge possibleEdge = myGraph.getEdge(myGraphNodes.get(from), myGraphNodes.get(to));
        if (possibleEdge != null) {
            removeComponentEdge(possibleEdge);
        }
    }

    private void removeComponentEdge(Edge theEdge) {
        myGraphEdges.remove(theEdge);
        myGraph.removeEdge(theEdge);
    }

    private void removeComponentEdges(HashSet<Edge> edgesToRemove) {
        for (Edge eachEdge : edgesToRemove) {
            removeComponentEdge(eachEdge);
        }
    }

    @SuppressWarnings("rawtypes")
    public Iterator getVisItems() {
        return m_vis.items();
    }

    public boolean searchMatches(VisualItem item) {
        if (Util.emptyOrNullString(searchKeyword)) {
            return false;
        } else {
            // do the check case-insensitively.  keyword is already converted
            //    to all-lower-case when it is set.
            String itemName = item.getString(CATEGORY_NAME).toLowerCase();
            return Util.wildCardMatch(itemName, searchKeyword);
        }
    }

    public int countSearchMatches(String text) {
        int count = 0;

        @SuppressWarnings("rawtypes")
        Iterator items = m_vis.items(treeNodes);
        while (items.hasNext()) {
            VisualItem anItem = ((VisualItem) items.next());
            if (searchMatches(anItem)) {
                count++;
            }
        }

        return count;
    }

    public void setSearchKeyword(String text) {
        if (Util.emptyOrNullString(text)) {
            searchKeyword = null;
        } else {
            searchKeyword = text.toLowerCase();
        }
    }
}
