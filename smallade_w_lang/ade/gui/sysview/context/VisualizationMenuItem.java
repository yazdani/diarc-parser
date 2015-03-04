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

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiCreatorUtil;
import ade.gui.ADEGuiPanel;
import ade.gui.ADEGuiVisualizationSpecs;
import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class VisualizationMenuItem extends JPanel {

    private static final long serialVersionUID = 1L;
    private ContextMenu contextMenu;
    private ADESystemView systemView;
    private String componentID;
    private String visType;
    private ADEGuiVisualizationSpecs.Item visSpec;
    private JButton visName;
    private JButton removeAllButton;

    protected VisualizationMenuItem(ContextMenu contextMenu,
            ADESystemView systemView, String componentID,
            Entry<String, ADEGuiVisualizationSpecs.Item> eachVisSpec) {
        this.contextMenu = contextMenu;
        this.systemView = systemView;
        this.componentID = componentID;
        this.visType = eachVisSpec.getKey();
        this.visSpec = eachVisSpec.getValue();


        this.setLayout(new BorderLayout());


        this.add(createVisNameButton(), BorderLayout.CENTER);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.X_AXIS));
        optionsPanel.add(createRemoveAllButton());
        this.add(optionsPanel, BorderLayout.EAST);


        updateVisDisplay();
    }

    private Component createVisNameButton() {
        visName = new JButton(this.visSpec.icon); // will set label text at end
        //    of constructor creation, since it also displays the counter.

        visName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    contextMenu.madeSelection();
                    ADEGuiCallHelper guiCallHelper = (ADEGuiCallHelper) systemView.callComponent("guiCreateGUICallHelper",
                            systemView.registryAccessKey, componentID, true);
                    ADEGuiPanel panel = ADEGuiCreatorUtil.createGuiPanel(guiCallHelper, visSpec);
                    systemView.visualizationManager.addComponentVisualization(componentID, visType, panel);
                    updateVisDisplay();
                } catch (Exception componentFetchException) {
                    System.out.println("Could not create visualization " + componentFetchException);
                    componentFetchException.printStackTrace();
                }
            }
        });

        return visName;
    }

    private Component createRemoveAllButton() {
        removeAllButton = new JButton(IconFetcher.get16x16icon("edit-clear.png"));
        removeAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                contextMenu.madeSelection();
                systemView.visualizationManager.
                        removeAllComponentVisualizations(componentID, visType);
                updateVisDisplay(); // so that sets the count visually back to 0.
            }
        });
        return removeAllButton;
    }

    private void updateVisDisplay() {
        int visCount = systemView.visualizationManager.
                countComponentVisualizations(componentID, visType);
        boolean visCountGreaterThan0 = (visCount > 0);

        if (visCountGreaterThan0) {
            visName.setText(visType + "   (" + visCount + ")");
        } else {
            visName.setText(visType);
        }

        removeAllButton.setVisible(visCountGreaterThan0);
    }
}
