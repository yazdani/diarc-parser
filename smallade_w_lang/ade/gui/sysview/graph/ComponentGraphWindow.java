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

import ade.ADERegistry;
import ade.gui.SystemViewComponentInfo;
import ade.gui.SystemViewStatusData;
import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import prefuse.controls.DragControl;
import prefuse.util.FontLib;
import prefuse.util.ui.UILib;
import utilities.Pair;

/**
 * A window that displays component information & connectivity. Multiple components
 * with the same name ARE allowed, so long as the type$name key (which is how
 * ADE keeps track of components) is unique.
 */
public class ComponentGraphWindow extends JInternalFrame {

    private static final long serialVersionUID = 1L;
    private ADESystemView sysView;
    private String thisRegistryID;
    private SystemViewStatusData formerStatusData;
    public ComponentGraphDisplay myVisualization;
    public JLabel nodeTitleLabel;
    private JTextField searchField;
    private JLabel searchLabel;
    private JLabel clearSearchButton;

    /**
     * static method to show/dispose graph, and to update menu checkbox
     * appropriately
     */
    public static void showGraph(ADESystemView sysView, boolean showIt) {
        if (showIt) {
            // if hasn't been created yet, create it now
            if (sysView.componentGraph == null) {
                sysView.componentGraph = new ComponentGraphWindow(sysView);
                sysView.addWindow(sysView.componentGraph);
            } else {
                // otherwise, bring it to front, etc.
                sysView.showWindow(sysView.componentGraph);
            }
            sysView.componentGraph.resetAllData();
        } else {
            sysView.componentGraph.setVisible(false);
        }

        sysView.menuBar.windowMenu.componentGraphMenuItem.setSelected(showIt);
    }

    public ComponentGraphWindow(ADESystemView sysView) {
        this.sysView = sysView;
        this.thisRegistryID = Util.getKey(
                ADERegistry.class.getCanonicalName(), sysView.registryName);

        setTitle("Component Graph");
        setSize(500, 350);

        formatInternalFrameAndAddWindowClosingListener();

        initContentPane();

        resetAllData(); // on creation, perform reset.	
    }

    /**
     * clears any remembered former status, so that re-creates entire graph.
     * useful in case something went wrong with former graph creation, and want
     * a true "reset"
     */
    private void resetAllData() {
        // init former data to empty data, that way don't have to check for null
        formerStatusData = new SystemViewStatusData();
    }

    private void initContentPane() {
        this.setLayout(new BorderLayout());
        this.add(createVisualization(), BorderLayout.CENTER);
    }

    private JPanel createVisualization() {
        myVisualization = new ComponentGraphDisplay();

        // add the one root node:
        myVisualization.addComponentNode(thisRegistryID);

        myVisualization.reCenterAroundComponentNodeAndRunLayout(thisRegistryID);


        nodeTitleLabel = new JLabel();
        nodeTitleLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        nodeTitleLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        nodeTitleLabel.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 16));


        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(nodeTitleLabel, BorderLayout.WEST);
        bottomPanel.add(createSearchPanel(), BorderLayout.EAST);


        JPanel panel = new JPanel(new BorderLayout());
        panel.add(myVisualization, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        Color BACKGROUND = Color.BLACK;
        Color FOREGROUND = Color.WHITE;
        UILib.setColor(panel, BACKGROUND, FOREGROUND);

        addVisualizationMouseListeners();

        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout());

        searchLabel = new JLabel();
        searchLabel.setIcon(IconFetcher.get16x16icon("system-search.png"));
        searchPanel.add(searchLabel, BorderLayout.WEST);

        searchField = new JTextField(10);

        clearSearchButton = UtilUI.createClickableLabel("process-stop.png", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText(null);
            }
        });

        clearSearchButton.setVisible(false); // will only get visible once text is typed into search box

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                countSearchMatches();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                countSearchMatches();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                countSearchMatches();
            }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);

        clearSearchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                searchField.setText(null);
            }
        });
        searchPanel.add(clearSearchButton, BorderLayout.EAST);

        return searchPanel;
    }

    private void countSearchMatches() {
        String text = searchField.getText();
        myVisualization.setSearchKeyword(text);
        myVisualization.runRepaintCommand();
        if (Util.emptyOrNullString(text)) {
            searchLabel.setText(null);
            clearSearchButton.setVisible(false);
        } else {
            clearSearchButton.setVisible(true);
            int matches = myVisualization.countSearchMatches(searchField.getText());
            if (matches == 1) {
                searchLabel.setText("1 match ");
            } else {
                searchLabel.setText(matches + " matches ");
            }
        }
    }

    private void addVisualizationMouseListeners() {
        myVisualization.addControlListener(new HoverMouseAdapter(myVisualization, this.nodeTitleLabel));
        myVisualization.addControlListener(new DragControl()); // to allow dragging of nodes
        myVisualization.addControlListener(new ReRootOnDoubleClickControl(myVisualization));
        myVisualization.addControlListener(new ContextMenuListener(sysView, myVisualization));
    }

    private void formatInternalFrameAndAddWindowClosingListener() {
        this.setResizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setFocusable(true);

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

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (myVisualization != null) {
                    myVisualization.runLayoutCommand();
                }
            }
        });
    }

    private void windowClosingCallback() {
        sysView.menuBar.windowMenu.componentGraphMenuItem.setSelected(false);
    }

    public void refreshGUI(SystemViewStatusData newStatusData) {
        // sometimes (rather rarely, and for no particular reason),
        //     the Graph visualization will error.  Thus, best keep it in a try-catch,
        //     and, on catching the error, try to reset the window and alert the user
        //     (that way, if problem persists, they at least know to close the graph window)

        try {
            // accumulate changes into hashsets first, that way can do them in the right order, 
            //    i.e., adding all nodes before adding edges (which might reference unexisting nodes),
            //    and so know whether to cancel the existing visualization layout-moving or not.
            HashSet<String> nodeIDsToAdd = new HashSet<String>();
            HashSet<Pair<String, String>> edgeIDsToAdd = new HashSet<Pair<String, String>>();
            HashSet<String> nodeIDsToRemove = new HashSet<String>();
            HashSet<Pair<String, String>> edgeIDsToRemove = new HashSet<Pair<String, String>>();


            // add new nodes
            for (SystemViewComponentInfo eachComponentInfo : newStatusData.componentIDtoInfoMap.values()) {
                String eachComponentInfoID = Util.getKey(eachComponentInfo.type, eachComponentInfo.name);
                boolean nodeFormerlyExisted = formerStatusData.componentIDtoInfoMap.containsKey(eachComponentInfoID);
                if (!nodeFormerlyExisted) {
                    nodeIDsToAdd.add(eachComponentInfoID);
                    // also add connection from the registry that the component sits on to the newly created node
                    if ((eachComponentInfo.registryName != null) && (eachComponentInfo.registryName.length() > 0)) {
                        // needed the if check because the original registry would not be registered to anything
                        //     other than itself.
                        edgeIDsToAdd.add(new Pair<String, String>(
                                Util.getKey(ADERegistry.class.getCanonicalName(), eachComponentInfo.registryName),
                                eachComponentInfoID));
                    }
                }


                // former clients could conceivably have been null, if graph wasn't turned on at the time.
                //    but if so, just skip the for loop altogether.  Current, on the other hand, can be
                //    empty, but will never be null.
                HashSet<String> formerClients = null;
                if (nodeFormerlyExisted) {
                    formerClients = formerStatusData.componentIDtoInfoMap.get(eachComponentInfoID).clients;
                }

                // add new clients:
                if (eachComponentInfo.clients != null) {
                    for (String eachNewClientID : eachComponentInfo.clients) {
                        if ((formerClients == null)
                                || (!formerClients.contains(eachNewClientID))) {
                            // the "eachNewClient" string is actually of the form type$name.  
                            //     but the visualization only cares about the name:
                            edgeIDsToAdd.add(new Pair<String, String>(eachNewClientID, eachComponentInfoID));
                        }
                    }
                }

                // if formerly visualized this node, ensure that remove any unused old connections:
                if (nodeFormerlyExisted) {
                    if (formerClients != null) {
                        for (String formerConnectedComponentID : formerClients) {
                            if (!eachComponentInfo.clients.contains(formerConnectedComponentID)) {
                                edgeIDsToRemove.add(new Pair<String, String>(formerConnectedComponentID, eachComponentInfoID));
                            }
                        }
                    }
                }

            }

            // remove no longer used nodes:
            for (String formerNode : formerStatusData.componentIDtoInfoMap.keySet()) {
                if (!newStatusData.componentIDtoInfoMap.containsKey(formerNode)) {
                    nodeIDsToRemove.add(formerNode);
                }
            }



            if ((nodeIDsToAdd.isEmpty())
                    && (edgeIDsToAdd.isEmpty())
                    && (nodeIDsToRemove.isEmpty())
                    && (edgeIDsToRemove.isEmpty())) {
                // easy life, nothing to do!
            } else {
                boolean reCenterAroundRegistry = false;

                // first add all nodes
                for (String eachNodeIDtoAdd : nodeIDsToAdd) {
                    myVisualization.addComponentNode(eachNodeIDtoAdd);
                }
                // then all edges
                for (Pair<String, String> eachEdgePairToAdd : edgeIDsToAdd) {
                    myVisualization.addComponentEdge(eachEdgePairToAdd.item1, eachEdgePairToAdd.item2);
                }
                // then remove all dead nodes
                for (String eachNodeIDtoRemove : nodeIDsToRemove) {
                    myVisualization.removeComponentNode(eachNodeIDtoRemove);
                    if (eachNodeIDtoRemove.equals(myVisualization.centeredAround)) {
                        reCenterAroundRegistry = true;
                    }
                }
                // then remove all dead edges
                for (Pair<String, String> eachEdgePairToRemove : edgeIDsToRemove) {
                    myVisualization.removeComponentEdge(eachEdgePairToRemove.item1, eachEdgePairToRemove.item2);
                }



                // ... and, after all this ...
                if (reCenterAroundRegistry) {
                    myVisualization.reCenterAroundComponentNodeAndRunLayout(thisRegistryID);
                }
                myVisualization.runLayoutCommand();

                countSearchMatches(); // since new ones might now match.
            }


            // update former to the current.
            formerStatusData = newStatusData;

        } catch (Exception e) {
            System.out.println("An exception occured while trying to display the Component Graph Window "
                    + "\nof ADE SystemView.  Attempting to reset the graph visualization data."
                    + "\nIf the problem persists, close the graph visualization window.");

            formerStatusData = null; // see if the reset helps
        }
    }
}
