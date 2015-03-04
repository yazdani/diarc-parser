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
package ade.gui.sysview.windows;

import ade.gui.sysview.ADESystemView;
import java.awt.EventQueue;
import utilities.Pair;

public class InfoViewerWindow extends AbstractTextWindow {

    private static final long serialVersionUID = 1L;

    public InfoViewerWindow(final ADESystemView sysView,
            final String componentID) throws Exception {
        super(sysView, componentID, true, false);
    }

    @Override
    protected void updateOrSetText() {
        try {
            String componentInfoPrintText = (String) sysView.callComponent(
                    "guiGetComponentInfoPrintText",
                    sysView.registryAccessKey, componentID, true);
            textPane.setText(componentInfoPrintText);
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    scrollPane.getVerticalScrollBar().setValue(0);
                }
            });

            buttonErrorUpdating.setVisible(false);
        } catch (Exception e) {
            lastUpdateException = e;
            buttonErrorUpdating.setVisible(true);
        }
    }

    @Override
    protected void onClosing() {
        sysView.otherWindows.remove(new Pair<OtherWindowType, String>(
                OtherWindowType.INFO, componentID));
    }

    @Override
    protected String getWindowDescription() {
        return "Info";
    }
}
