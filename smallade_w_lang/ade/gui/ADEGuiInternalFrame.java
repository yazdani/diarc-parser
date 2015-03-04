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
package ade.gui;

import ade.gui.ADEGuiPanel.VisibilityDeterminer;
import ade.gui.sysview.VisualizationManager;
import java.awt.Dimension;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

public class ADEGuiInternalFrame extends JInternalFrame implements InternalFrameListener {

    private static final long serialVersionUID = 1L;
    private ADEGuiInternalFrame meTheFrame = this;
    private boolean myFirstSetVisibleCall = true; // assume that still hasn't
    //      called it; ie: will be first call
    private VisualizationManager visManager;
    private ADEGuiPanel myGUIPanel;
    private String componentID;
    private String visType;

    public ADEGuiInternalFrame(ADEGuiPanel guiPanel,
            VisualizationManager visManager, String componentID, String visType) {
        this.myGUIPanel = guiPanel;
        this.visManager = visManager;
        this.componentID = componentID;
        this.visType = visType;

        this.setContentPane(myGUIPanel);

        myGUIPanel.setVisibilityDeterminer(new VisibilityDeterminer() {
            @Override
            public boolean isVisible() {
                // update GUI -- provided the frame is visible and 
                //    not iconified (aka "minimized"), otherwise it would be a waste!
                return (meTheFrame.isVisible() && (!(meTheFrame.isIcon())));
            }
        });

        this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);

        this.setResizable(true);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);

        this.addInternalFrameListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return ADEGuiPanel.getSizeWithInsets(this.getContentPane().getPreferredSize(), this.getInsets());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        // check if this was the first call to show the frame:
        if (visible && myFirstSetVisibleCall) {
            this.setSize(ADEGuiPanel.getSizeWithInsets(myGUIPanel.getInitSize(true), this.getInsets()));
            // 		true = is an internal frame.  I know this because this *IS* the INTERNAL frame class!

            this.setTitle(myGUIPanel.getInitTitle());
            myFirstSetVisibleCall = false;

            // notify GUI that it is loaded -- i.e., so it knows 
            //       to update itself and start the update timer.
            myGUIPanel.isLoaded(true); // true = is an internal frame
            //     I know this because this *IS* the INTERNAL frame class!
        }
    }

    @Override
    public void internalFrameClosed(InternalFrameEvent e) {
        visManager.removeComponentVisualization(this, this.componentID, visType);
    }

    @Override
    public void internalFrameDeiconified(InternalFrameEvent e) {
        // now that's de-iconafied, refresh the GUI immediately
        myGUIPanel.refreshGui();
    }

    @Override
    public void internalFrameActivated(InternalFrameEvent e) {/* don't care */

    }

    @Override
    public void internalFrameClosing(InternalFrameEvent e) {/* don't care */

    }

    @Override
    public void internalFrameDeactivated(InternalFrameEvent e) {/* don't care */

    }

    @Override
    public void internalFrameIconified(InternalFrameEvent e) {/* don't care */

    }

    @Override
    public void internalFrameOpened(InternalFrameEvent e) {/* don't care */

    }
}
