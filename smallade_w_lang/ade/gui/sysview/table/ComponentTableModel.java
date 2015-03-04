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

import ade.gui.SystemViewComponentInfo;
import ade.gui.SystemViewStatusData;
import java.util.TreeMap;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

public class ComponentTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] columnNames = {"Type", "Name", "Host", "Registry"};
    //     if you make any changes here, make sure to make appropriate changes to the
    //     getValueAt(int row, int col) method as well!
    private static final int COLUMN_INDEX_TYPE = 0;
    private static final int COLUMN_INDEX_NAME = 1;
    private static final int COLUMN_INDEX_HOST = 2;
    private static final int COLUMN_INDEX_REGISTRY = 3;
    private TreeMap<String, SystemViewComponentInfo> componentIDtoInfoMap;
    //     treemap so that automatically gets sorted.  (will be sorted by 
    //     type$name, so all types will be sorted together; but that seems
    //     to be as good a sorting paradigm as any other -- names would be
    //     equally arbitrary, but harder to implement).
    private Object[] cachedIDkeyset; // a cache of the current ordering of IDs,
    //     cached for efficiency.
    private JTable myTable;

    public ComponentTableModel(JTable table) {
        this.myTable = table;

        this.componentIDtoInfoMap = new TreeMap<String, SystemViewComponentInfo>();
        this.cachedIDkeyset = new Object[]{};
    }

    /**
     * the most important method, called by the ComponentTable window, to refresh
     * the data
     *
     * @param i
     */
    public void updateBasedOnNewData(SystemViewStatusData statusData) {
        // the easiest thing to do is actually just clear the values every time.
        //     For the size of tables we're talking about with ADE, it is probably
        //     about as efficient as searching through the data for 
        //     possible updates, inserting rows in particular alphabetical order, etc.

        // before clear all the data, though, first check if any row was selected:
        int formerSelectedRowIndex = myTable.getSelectedRow();
        String formerSelectedRowIDifAny = null;
        if (formerSelectedRowIndex >= 0) {
            formerSelectedRowIDifAny = componentIDtoInfoMap.get(
                    cachedIDkeyset[formerSelectedRowIndex]).getComponentID();
        }

        componentIDtoInfoMap.clear();

        for (SystemViewComponentInfo eachInfo : statusData.componentIDtoInfoMap.values()) {
            componentIDtoInfoMap.put(eachInfo.getComponentID(), eachInfo);
        }

        cachedIDkeyset = componentIDtoInfoMap.keySet().toArray();

        this.fireTableDataChanged(); // everything except the columns may have changed

        if (formerSelectedRowIndex >= 0) {
            int newSelectionRow = findIndexForComponentID(formerSelectedRowIDifAny);
            if (newSelectionRow >= 0) {
                myTable.setRowSelectionInterval(newSelectionRow, newSelectionRow);
            }
        }
    }

    private int findIndexForComponentID(String lookingFor) {
        int counter = 0;
        for (Object each : cachedIDkeyset) {
            if (each.toString().equals(lookingFor)) {
                return counter;
            }
            counter++;
        }
        // if hasn't returned with actual found index
        return -1;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return componentIDtoInfoMap.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        SystemViewComponentInfo info = componentIDtoInfoMap.get(cachedIDkeyset[row]);

        if (col == COLUMN_INDEX_TYPE) {
            return info.type;
        } else if (col == COLUMN_INDEX_NAME) {
            return info.name;
        } else if (col == COLUMN_INDEX_HOST) {
            return info.host;
        } else if (col == COLUMN_INDEX_REGISTRY) {
            return info.registryName;
        } else {
            System.out.println("Unknown column data number in \"getValueAt\" method of "
                    + this.getClass().getCanonicalName());
            return null;
        }
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public String getComponentID(int row) {
        return componentIDtoInfoMap.get(cachedIDkeyset[row]).getComponentID();
    }
}
