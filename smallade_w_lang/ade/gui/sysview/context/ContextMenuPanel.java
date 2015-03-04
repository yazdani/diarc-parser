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
package ade.gui.sysview.context;

import ade.gui.ADEGuiVisualizationSpecs;
import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import ade.gui.sysview.windows.InfoViewerWindow;
import ade.gui.sysview.windows.Lambdas;
import ade.gui.sysview.windows.OtherWindowType;
import ade.gui.sysview.windows.OutputViewerWindow;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import utilities.Pair;

public class ContextMenuPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private ContextMenu contextMenu;
    private ADESystemView systemView;
    private String componentID;

    protected ContextMenuPanel(ContextMenu contextMenu,
            final ADESystemView systemView, final String componentID) {
        this.contextMenu = contextMenu;
        this.systemView = systemView;
        this.componentID = componentID;


        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        createVisualizationMenuItems();

        this.add(createOtherWindowToggleInStretchableComponent(
                "Show terminal output", "utilities-terminal.png",
                OtherWindowType.OUTPUT, new Lambdas.WindowCreator() {
            public JInternalFrame createWindow() throws Exception {
                return new OutputViewerWindow(systemView, componentID);
            }
        }));

        this.add(createOtherWindowToggleInStretchableComponent(
                "Show component info", "accessories-text-editor.png",
                OtherWindowType.INFO, new Lambdas.WindowCreator() {
            @Override
            public JInternalFrame createWindow() throws Exception {
                return new InfoViewerWindow(systemView, componentID);
            }
        }));


        JButton shutdown = new JButton("Shut down component");
        shutdown.setIcon(IconFetcher.get16x16icon("process-stop.png"));
        shutdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shutdownComponent();
            }
        });
        this.add(UtilUI.createStretchablePanelForComponent(shutdown));
    }

    private Component createOtherWindowToggleInStretchableComponent(
            String title, String iconName, OtherWindowType windowType,
            final Lambdas.WindowCreator windowCreator) {

        final Pair<OtherWindowType, String> matchingPair =
                new Pair<OtherWindowType, String>(windowType, componentID);

        final JToggleButton toggleButton = new JToggleButton(title,
                systemView.otherWindows.containsKey(matchingPair));
        toggleButton.setIcon(IconFetcher.get16x16icon(iconName));
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                contextMenu.madeSelection();

                if (toggleButton.isSelected()) {
                    try {
                        JInternalFrame window = windowCreator.createWindow();
                        systemView.otherWindows.put(matchingPair, window);
                        systemView.addWindow(window);
                    } catch (Exception e1) {
                        String message = "Could not create the requested window for component " + componentID
                                + " due to the following exception:\n\n" + Util.stackTraceString(e1);
                        JOptionPane.showMessageDialog(contextMenu, UtilUI.messageInScrollPane(message),
                                "Error creating window", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    systemView.otherWindows.get(matchingPair).dispose();
                }
            }
        });

        return UtilUI.createStretchablePanelForComponent(toggleButton);
    }

    protected void shutdownComponent() {
        // begin by closing the context menu, because either will see error message, or the 
        //    component will disappear, and so the context menu will be a relic of the past
        contextMenu.setVisible(false);

        try {
            boolean shutdownResult = (Boolean) systemView.callComponent("guiShutdownComponent",
                    systemView.registryAccessKey, componentID, true);
            if (!shutdownResult) {
                JOptionPane.showMessageDialog(this, "Could not shut down component " + componentID + ".",
                        "Error shutting down component", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            String message = "Could not shutdown component " + componentID
                    + " due to the following exception:\n\n" + Util.stackTraceString(e);
            JOptionPane.showMessageDialog(this, UtilUI.messageInScrollPane(message),
                    "Error shutting down component", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createVisualizationMenuItems() {
        try {
            ADEGuiVisualizationSpecs visSpecs = (ADEGuiVisualizationSpecs) systemView.callComponent("guiGetComponentVisualizationSpecs",
                    systemView.registryAccessKey, componentID, true);

            if ((visSpecs == null) || (visSpecs.size() == 0)) {
                this.add(UtilUI.createStretchablePanelForComponent(
                        new JLabel("No visualizations available",
                        IconFetcher.get16x16icon("help-browser.png"), JLabel.LEFT), 5));
            }

            for (final Entry<String, ADEGuiVisualizationSpecs.Item> eachVisSpec : visSpecs.entrySet()) {
                this.add(new VisualizationMenuItem(contextMenu, systemView, componentID, eachVisSpec));
            }

            this.add(new JSeparator());
        } catch (Exception ex) {
            this.add(UtilUI.createStretchablePanelForComponent(
                    new JLabel("Could not get visualization info",
                    IconFetcher.get16x16icon("process-stop.png"), JLabel.LEFT), 5));
            System.err.println("Could not get visualization information.  Exception = " + ex);
            ex.printStackTrace();
        }
    }
}
