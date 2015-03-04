/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * GoalManagerVis.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 */
package com.action;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import com.ActionStatus;
import static utilities.Util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** 
 * <code>GoalManagerVis</code> is the GUI visualizer for the goal manager.
 */
public class GoalManagerVis extends ADEGuiPanel {
    private boolean verbose = false;

    private Container IFrame = null;
    private JTabbedPane tabs;
    private JTable manager;
    private JTable completedGoals;
    private HashMap<Long, JTable> goals;

    /* Called periodically by the ADEGui, but only if the frame is visible, so updates
     * have to be done in a separate thread (in Updater below).
     */
    @Override
    public void refreshGui() {
        ArrayList<ActionInterpreterInfo> goalInfo = new ArrayList<ActionInterpreterInfo>();
        GoalManagerInfo managerInfo;
        HashSet<Long> gids = new HashSet<Long>();
        HashSet<JTable> removeTabs = new HashSet<JTable>();

        // PWS: could optimize this to only update the selected pane...probably
        try {
            Object gi = callComponent("getGoalInfo");
            for (Object o:(ArrayList)gi) {
                goalInfo.add((ActionInterpreterInfo)o);
            }
            for (ActionInterpreterInfo i:goalInfo) {
                JTable t = goals.get(i.goalID);
                if (t == null) {
                    // create a new table and add it
                    t = newGoalTable(i);
                    goals.put(i.goalID, t);
                    tabs.insertTab(i.cmd, null, t, i.cmd, tabs.getTabCount()-1);
                } else {
                    // update existing goal
                    updateGoalTable(i, t);
                }
                // keep track of which gids have been seen this update
                gids.add(i.goalID);
            }
            // Remove tabs for completed goals, but only if getGoalInfo succeeds
            for (int i = 0; i < tabs.getTabCount(); i++) {
                JTable t = (JTable)tabs.getComponentAt(i);
                if (t == completedGoals || t == manager)
                    continue;
                Long gid = (Long)t.getValueAt(4, 1);
                if (! gids.contains(gid)) {
                    ActionStatus as = ActionStatus.UNKNOWN;
                    DefaultTableModel m = (DefaultTableModel)completedGoals.getModel();
                    try {
                        as = (ActionStatus)callComponent("goalStatus",gid);
                    } catch (Exception gse) {
                        System.err.println("AMVis: error getting goal status: "+gse);
                    }
                    m.insertRow(0, new Object[] {gid, t.getValueAt(1,1), as});
                    removeTabs.add(t);
                }
            }
            for (JTable t:removeTabs) {
                tabs.remove(t);
            }
        } catch (Exception e) {
            System.err.println("AMVis: error getting goal info: "+e);
            //e.printStackTrace();
        }

        try {
            managerInfo = (GoalManagerInfo)callComponent("getInfo");
            updateManagerTable(managerInfo, manager);
        } catch (Exception e) {
            System.err.println("AMVis: error getting goal manager info: "+e);
        }

        this.repaint();
    }

    // create a new generic table model with unmodifiable cells
    static DefaultTableModel newTableModel() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            };
        };
        return model;
    }

    // create a new goal manager table
    static JTable newManagerTable(GoalManagerInfo i) {
        JTable t;
        DefaultTableModel m = newTableModel();

        m.addColumn("Property");
        m.addColumn("Value");
        m.addRow(new Object[] {"Agent Name", i.agentname});
        m.addRow(new Object[] {"Goals", i.numGoals});
        m.addRow(new Object[] {"Cycle Time", i.cycleTime});
        m.addRow(new Object[] {"Positive Affect", i.positiveAffect});
        m.addRow(new Object[] {"Negative Affect", i.negativeAffect});

        t = new JTable(m);
        t.getColumnModel().getColumn(0).setPreferredWidth(100);
        t.getColumnModel().getColumn(1).setPreferredWidth(100);

        return t;
    }

    // update a goal table
    static void updateManagerTable(GoalManagerInfo i, JTable t) {
        t.setValueAt(i.agentname,0,1);
        t.setValueAt(i.numGoals,1,1);
        t.setValueAt(i.cycleTime,2,1);
        t.setValueAt(i.positiveAffect,3,1);
        t.setValueAt(i.negativeAffect,4,1);
    }

    // create a new goal table
    static JTable newGoalTable(ActionInterpreterInfo i) {
        JTable t;
        DefaultTableModel m = newTableModel();

        m.addColumn("Property");
        m.addColumn("Value");
        m.addRow(new Object[] {"Agent Name", i.agentname});
        m.addRow(new Object[] {"Command", i.cmd});
        m.addRow(new Object[] {"Current Action", i.currentAction});
        m.addRow(new Object[] {"Status", i.status});
        m.addRow(new Object[] {"Goal ID", i.goalID});
        m.addRow(new Object[] {"Priority", i.priority});
        m.addRow(new Object[] {"Cost", i.cost});
        m.addRow(new Object[] {"Benefit", i.benefit});
        m.addRow(new Object[] {"Maximum Urgency", i.maxUrgency});
        m.addRow(new Object[] {"Minimum Urgency", i.minUrgency});
        m.addRow(new Object[] {"Positive Affect", i.posAff});
        m.addRow(new Object[] {"Negative Affect", i.negAff});

        t = new JTable(m);
        t.getColumnModel().getColumn(0).setPreferredWidth(100);
        t.getColumnModel().getColumn(1).setPreferredWidth(100);

        return t;
    }

    // update a goal table
    static void updateGoalTable(ActionInterpreterInfo i, JTable t) {
        t.setValueAt(i.agentname,0,1);
        t.setValueAt(i.cmd,1,1);
        t.setValueAt(i.currentAction,2,1);
        t.setValueAt(i.status,3,1);
        t.setValueAt(i.goalID,4,1);
        t.setValueAt(i.priority,5,1);
        t.setValueAt(i.cost,6,1);
        t.setValueAt(i.benefit,7,1);
        t.setValueAt(i.maxUrgency,8,1);
        t.setValueAt(i.minUrgency,9,1);
        t.setValueAt(i.posAff,10,1);
        t.setValueAt(i.negAff,11,1);
    }

    // create a new completed goal table
    static JTable newCompletedTable() {
        JTable t;
        DefaultTableModel m = newTableModel();

        m.addColumn("Goal ID");
        m.addColumn("Command");
        m.addColumn("Status");

        t = new JTable(m);
        t.getColumnModel().getColumn(0).setPreferredWidth(100);
        t.getColumnModel().getColumn(1).setPreferredWidth(100);

        return t;
    }

    /** get initial size.  by default, just the preferred size of the panel
     * That said, sometimes the panel does not know how to determine its own
     * preferred size well, or the initial size might be set to something
     * entirely different.  If so, getInitSize(boolean isInternalWindow) or
     * getPreferredSize() or possibly both should definitely be overwritten.
     * If you want to distinguish between an initial size for a big-window
     * GUI versus as an internal frame inside the ADE SystemView, you should
     * likewise override this method.
     * @param isInternalWindow
     */
    @Override
    public Dimension getInitSize(boolean isInternalWindow) {
        Dimension minSize = this.getPreferredSize();
        int w = 500;
        int h = 250;
        if (isInternalWindow) {
            h += 20; // seems to be about the size of the title bar -- for now...
        }
        return new Dimension(w, h);
    }

    /**
     * GoalManagerVis constructor.
     */
    public GoalManagerVis(ADEGuiCallHelper callHelper) {
        super(callHelper, 200);

        try {
            GoalManagerInfo i = (GoalManagerInfo)callComponent("getInfo");
            manager = newManagerTable(i);
        } catch (Exception e) {
            System.err.println("AMVis: error getting goal manager info: "+e);
            manager = new JTable();
        }
        completedGoals = newCompletedTable();

        tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab("Goal Manager", null, manager, "Goal Manager");
        tabs.addTab("Completed Goals", null, completedGoals, "Completed Goals");
        this.setLayout(new BorderLayout());
        this.add(tabs, BorderLayout.CENTER);
        new Thread() {
            @Override
            public void run() {
                IFrame = findParentFrame();
                while (IFrame == null) {
                    Sleep(100);
                    IFrame = findParentFrame();
                }
            }
        }.start();

        goals = new HashMap<Long, JTable>();
    }
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
