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

import java.awt.BasicStroke;
import prefuse.Constants;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.visual.VisualItem;

public class NodeRenderer extends LabelRenderer {

    private static final BasicStroke LINE_STROKE = new BasicStroke(2);

    public NodeRenderer() {
        super(ComponentGraphDisplay.CATEGORY_NAME);
        this.setRenderType(AbstractShapeRenderer.RENDER_TYPE_DRAW_AND_FILL);
        this.setHorizontalAlignment(Constants.CENTER);
        this.setRoundedCorner(8, 8);
    }

    @Override
    protected BasicStroke getStroke(VisualItem item) {
        return LINE_STROKE;
    }
}
