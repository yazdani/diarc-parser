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
package ade.gui.sysview.table;

import ade.gui.SystemViewStatusData;
import ade.gui.sysview.ADESystemView;
import ade.gui.sysview.context.ContextMenu;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class ComponentTableWindow extends JInternalFrame {

    private static final long serialVersionUID = 1L;
    private ADESystemView sysView;
    private JTable table;
    private ComponentTableModel tableModel;

    /**
     * static method to show/dispose table, and to update menu checkbox
     * appropriately
     */
    public static void showTable(ADESystemView sysView, boolean showIt) {
        if (showIt) {
            // if haven't created it yet, create it now.
            if (sysView.componentTable == null) {
                sysView.componentTable = new ComponentTableWindow(sysView);
                sysView.addWindow(sysView.componentTable);
            } else {
                // otherwise, bring it to front, etc.
                sysView.showWindow(sysView.componentTable);
            }
        } else {
            sysView.componentTable.setVisible(false);
        }

        sysView.menuBar.windowMenu.componentTableMenuItem.setSelected(showIt);
    }

    public ComponentTableWindow(ADESystemView sysView) {
        this.sysView = sysView;

        setTitle("Component Listing");
        setSize(300, 200);

        createTable();

        formatInternalFrameAndAddWindowClosingListener();
    }

    private void createTable() {
        JScrollPane scrollPane = new JScrollPane();
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        table = new JTable();
        table.setFillsViewportHeight(true);
        scrollPane.setViewportView(table);

        this.tableModel = new ComponentTableModel(this.table);
        this.table.setModel(tableModel);

        // select a single row only
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { // right click
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        ContextMenu contextMenu = new ContextMenu(
                                sysView, tableModel.getComponentID(row));
                        e.consume(); // not sure if it does much, but it doesn't hurt...
                        contextMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });

    }

    private void formatInternalFrameAndAddWindowClosingListener() {
        this.setResizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setFocusable(true);

        this.setMinimumSize(new Dimension(100, 50));

        // on closing, no need to dispose it, just hide it.  That way
        //    will come back in same location and same size, which should be convenient
        //    (if window size has changed, will bring back so that within bounds)
        this.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);

        this.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            // catch the closing, aka hiding, event
            public void internalFrameClosing(InternalFrameEvent e) {
                windowClosingCallback();
            }
        });
    }

    private void windowClosingCallback() {
        sysView.menuBar.windowMenu.componentTableMenuItem.setSelected(false);
    }

    public void refreshGUI(SystemViewStatusData statusData) {
        tableModel.updateBasedOnNewData(statusData);
    }
}
