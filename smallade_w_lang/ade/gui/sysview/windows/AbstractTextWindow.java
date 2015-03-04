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
import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public abstract class AbstractTextWindow extends JInternalFrame {

    private static final long serialVersionUID = 1L;
    private static final boolean DEFAULT_LINE_WRAP = false;
    protected ADESystemView sysView;
    protected String componentID;
    protected Exception lastUpdateException;
    protected JButton buttonErrorUpdating;
    protected JTextArea textPane;
    protected JCheckBox checkboxAutoscroll;
    protected JCheckBox checkboxLineWrap;
    protected JScrollPane scrollPane;

    public AbstractTextWindow(final ADESystemView sysView,
            final String componentID,
            boolean showManualRefreshButton,
            boolean showAutoScrollCheckbox) throws Exception {
        this.sysView = sysView;
        this.componentID = componentID;

        initializeWindow(); // initializes everything apart from 
        //    the GUI components, which are initialized below


        // now, do GUI initialization

        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.SOUTH);

        if (showAutoScrollCheckbox) {
            checkboxAutoscroll = new JCheckBox("Auto-scroll");
            checkboxAutoscroll.setSelected(true);
            panel.add(checkboxAutoscroll);
        }

        checkboxLineWrap = new JCheckBox("Line wrap");
        checkboxLineWrap.setSelected(DEFAULT_LINE_WRAP);
        checkboxLineWrap.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textPane.setLineWrap(checkboxLineWrap.isSelected());
            }
        });
        panel.add(checkboxLineWrap);

        if (showManualRefreshButton) {
            JButton refreshButton = new JButton("Refresh",
                    IconFetcher.get16x16icon("view-refresh.png"));
            refreshButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateOrSetText();
                }
            });
            panel.add(refreshButton);
        }

        buttonErrorUpdating = new JButton("Error updating!",
                IconFetcher.get16x16icon("dialog-warning.png"));
        buttonErrorUpdating.setVisible(false);
        buttonErrorUpdating.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(sysView, UtilUI.messageInScrollPane(
                        Util.stackTraceString(lastUpdateException)),
                        "An error occured while trying to update Output window",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(buttonErrorUpdating);

        scrollPane = new JScrollPane();
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        textPane = new JTextArea();
        textPane.setLineWrap(DEFAULT_LINE_WRAP);
        textPane.setEditable(false); // for info only, not editable
        scrollPane.setViewportView(textPane);


        updateOrSetText(); // before the window is ever opened

        this.setVisible(true);
    }

    /**
     * ********* ABSTRACT METHODS ****************
     */
    protected abstract void updateOrSetText();

    protected abstract String getWindowDescription();

    protected abstract void onClosing();

    private void initializeWindow() throws Exception {
        this.setTitle(Util.getNameFromID(this.componentID) + " "
                + getWindowDescription() + " "
                + "[" + Util.getTypeFromID(this.componentID) + "]");

        // also do some internal-frame-related GUI initialization:
        this.setSize(450, 300);

        this.setIconifiable(true);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setResizable(true);

        this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);

        this.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                onClosing();
            }
        });
    }

    /**
     * note: the following code was based on a stackoverflow.com answer:
     * http://stackoverflow.com/questions/4045722/how-to-make-jtextpane-
     * autoscroll-only-when-scroll-bar-is-at-bottom-and-scroll-loc 
	 *
     */
    protected void scrollToBottomIfAutoscrollChecked() {
        if (checkboxAutoscroll.isSelected()) {
            BoundedRangeModel scrollBarModel = scrollPane.getVerticalScrollBar().getModel();
            if (scrollBarModel.getExtent() + scrollBarModel.getValue() < scrollBarModel.getMaximum()) {
                // Ask EventQueue to scroll to bottom (invokeLater so that first
                //     gets done appending new text).
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                Rectangle visibleRect = textPane.getVisibleRect();
                                visibleRect.y = textPane.getHeight() - visibleRect.height;
                                textPane.scrollRectToVisible(visibleRect);
                            }
                        });
                    }
                });
            }
        }
    }
}
