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

import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.sysview.ADESystemView;
import java.util.UUID;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import utilities.Pair;

public class OutputViewerWindow extends AbstractUpdatingTextWindow {

    private static final long serialVersionUID = 1L;
    private UUID consumerID;
    private Document textPaneDocument;
    private static final int UPDATE_DELAY = 250; // 4x/sec should be 
    //    more than enough for updating (sysout and syserr buffer anyway).

    public OutputViewerWindow(final ADESystemView sysView,
            final String componentID) throws Exception {
        super(sysView, componentID, true);

        this.consumerID = (UUID) sysView.callComponent(
                "guiRegisterOutputRedirectionConsumer",
                sysView.registryAccessKey, componentID, true);

        this.textPaneDocument = this.textPane.getDocument();
    }

    @Override
    protected void updateOrSetText() {
        try {
            String newOutput = (String) sysView.callComponent("guiGetAccumulatedRedirectedOutput",
                    this.sysView.registryAccessKey, this.componentID, this.consumerID, true);
            textPaneDocument.insertString(textPaneDocument.getLength(), newOutput, null);

            scrollToBottomIfAutoscrollChecked();

            buttonErrorUpdating.setVisible(false);
        } catch (Exception e) {
            lastUpdateException = e;
            buttonErrorUpdating.setVisible(true);
        }

    }

    protected void onClosing() {
        sysView.otherWindows.remove(new Pair<OtherWindowType, String>(
                OtherWindowType.OUTPUT, componentID));

        try {
            sysView.callComponent("guiDeregisterOutputRedirectionConsumer",
                    sysView.registryAccessKey, componentID, consumerID, true);
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(sysView, UtilUI.messageInScrollPane(
                    "An error occured while trying to close & de-register "
                    + "the Output Viewer window.\n\n" + Util.stackTraceString(e1)),
                    "Output de-registration error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected String getWindowDescription() {
        return "Output (live)";
    }

    @Override
    protected int getUpdateDelay() {
        return UPDATE_DELAY;
    }
}
