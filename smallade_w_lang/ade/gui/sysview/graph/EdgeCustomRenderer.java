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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import prefuse.Constants;
import prefuse.render.EdgeRenderer;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;

public class EdgeCustomRenderer extends EdgeRenderer {

    // wrapper class, just so can expose getRawShape as public, 
    //    even though, internally, protected.
    private class EdgeRendererWrapper extends EdgeRenderer {

        @Override
        public Shape getRawShape(VisualItem item) {
            return super.getRawShape(item);
        }
    }
    private EdgeRendererWrapper lineEdgeRenderer;
    private EdgeRendererWrapper arrowEdgeRenderer;

    public EdgeCustomRenderer() {
        lineEdgeRenderer = new EdgeRendererWrapper();
        lineEdgeRenderer.setDefaultLineWidth(1);
        lineEdgeRenderer.setArrowType(Constants.EDGE_ARROW_NONE);

        arrowEdgeRenderer = new EdgeRendererWrapper();
        arrowEdgeRenderer.setDefaultLineWidth(1);
        arrowEdgeRenderer.setArrowType(Constants.EDGE_ARROW_FORWARD);
        arrowEdgeRenderer.setArrowHeadSize(10, 15);
    }

    @Override
    protected Shape getRawShape(VisualItem item) {
        return getAppropriateRenderer(item).getRawShape(item);
    }

    @Override
    public void render(Graphics2D g, VisualItem item) {
        getAppropriateRenderer(item).render(g, item);
    }

    @Override
    public boolean locatePoint(Point2D p, VisualItem item) {
        return getAppropriateRenderer(item).locatePoint(p, item);
    }

    @Override
    public void setBounds(VisualItem item) {
        getAppropriateRenderer(item).setBounds(item);
    }

    public static boolean isRegistryEdge(EdgeItem item) {
        NodeVisualizeEnum nodeTypeEnum = (NodeVisualizeEnum) ((EdgeItem) item).getSourceNode().get(
                ComponentGraphDisplay.CATEGORY_VISUALIZE_ENUM);
        return (nodeTypeEnum == NodeVisualizeEnum.REGISTRY);
    }

    private EdgeRendererWrapper getAppropriateRenderer(VisualItem item) {
        // if highlighted and not registry edge, use an arrow renderer.
        if (item.isHighlighted() && !isRegistryEdge((EdgeItem) item)) {
            return arrowEdgeRenderer;
        } else {
            return lineEdgeRenderer;
        }
    }
}
