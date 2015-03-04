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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public abstract class AbstractUpdatingTextWindow extends AbstractTextWindow {

    private static final long serialVersionUID = 1L;
    private Timer updateTimer;

    public AbstractUpdatingTextWindow(
            ADESystemView sysView, String componentID,
            boolean showAutoScrollCheckbox) throws Exception {
        super(sysView, componentID, false, showAutoScrollCheckbox);
        //    no need to show manual update button on a live-updating window.


        this.updateTimer = new Timer(getUpdateDelay(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateOrSetText();
            }
        });
        updateTimer.start();

        this.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                updateTimer.stop();
            }
        });
    }

    protected abstract int getUpdateDelay();
}
