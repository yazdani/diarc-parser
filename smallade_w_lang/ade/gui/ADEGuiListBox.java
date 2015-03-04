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
package ade.gui;

import ade.ADEComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import utilities.Util;

public class ADEGuiListBox extends JDialog {

    private static final long serialVersionUID = 1L;
    private ADEGuiListBox meTheWindow = this;
    private final JPanel checkboxPanel = new JPanel();
    private ArrayList<JCheckBox> checkboxes = new ArrayList<JCheckBox>();
    private ADEComponent component;
    private HashSet<String> guiParticularVisualizations;
    private ADEGuiVisualizationSpecs supportedVisualizations;
    private boolean persistentGUIs;

    /**
     * Create the dialog.
     *
     * @param component
     * @param guiParticularRequestedVisualizations: the particular visualization
     * names, if any, that were requested.
     * @param supportedVisualizations: visualizations supported by the component
     */
    public ADEGuiListBox(ADEComponent component,
            HashSet<String> guiParticularVisualizations,
            ADEGuiVisualizationSpecs supportedVisualizations,
            boolean persistentGUIs) {

        this.component = component;
        this.guiParticularVisualizations = guiParticularVisualizations;
        this.supportedVisualizations = supportedVisualizations;
        this.persistentGUIs = persistentGUIs;


        // remove the question mark from the --gui parameters, as that was there
        //     only to show this dialog.
        guiParticularVisualizations.remove("-?");


        // try to set the title of the dialog to the component name
        try {
            this.setTitle(component.getName());
        } catch (RemoteException e1) {
            // this should never happen; but if it did, oh well.
        }

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 450, 220);
        getContentPane().setLayout(new BorderLayout());
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        createGUIframes();
                    }
                });
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        meTheWindow.dispose();
                    }
                });
                buttonPane.add(cancelButton);
            }
        }
        checkboxPanel.setBackground(Color.WHITE);
        checkboxPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


        JLabel lblPleaseSelectThe = new JLabel("Which standalone visualizations would you like to display?");
        lblPleaseSelectThe.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        getContentPane().add(lblPleaseSelectThe, BorderLayout.NORTH);


        this.checkboxPanel.setLayout(new BoxLayout(this.checkboxPanel, BoxLayout.Y_AXIS));

        for (Entry<String, ADEGuiVisualizationSpecs.Item> eachVis : supportedVisualizations.entrySet()) {
            JCheckBox eachCheckbox = new JCheckBox(eachVis.getKey());
            if (eachVis.getValue().startupOption == ADEGuiVisualizationSpecs.StartupOption.ALWAYS_SHOW) {
                eachCheckbox.setSelected(true);
                eachCheckbox.setEnabled(false); // if always show, should not have option to have it not show...
            } else {// otherwise, check only if explicitly requested
                if (Util.containsIgnoreCase(guiParticularVisualizations, eachVis.getKey())) {
                    eachCheckbox.setSelected(true);
                }
            }

            checkboxes.add(eachCheckbox);
            checkboxPanel.add(eachCheckbox);
        }


        this.getContentPane().add(new JScrollPane(this.checkboxPanel), BorderLayout.CENTER);


        // once component is shown, alert user if any requested visualizations were invalid:
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                ArrayList<String> invalidRequests = new ArrayList<String>();
                for (String eachRequest : meTheWindow.guiParticularVisualizations) {
                    if (!Util.containsIgnoreCase(meTheWindow.supportedVisualizations.keySet(), eachRequest)) {
                        invalidRequests.add(eachRequest);
                    }
                }
                if (invalidRequests.size() > 0) {
                    JOptionPane.showMessageDialog(meTheWindow,
                            "The following specified visualizations could not be found:  "
                            + Util.join(invalidRequests, ", "),
                            "One or more invalid visualization request", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void createGUIframes() {
        // want to clear and properly set the guiParticularRequestedVisualizations,
        //     since that, in turn, is tied to myInfo.guiParticularVisualizations,
        //     and want that to be properly populated in case want to save the running component's
        //     configuration.
        guiParticularVisualizations.clear();

        HashSet<ADEGuiVisualizationSpecs.Item> guisToCreate =
                new HashSet<ADEGuiVisualizationSpecs.Item>();

        for (JCheckBox eachCheckbox : checkboxes) {
            if (eachCheckbox.isSelected()) {
                String visualizationName = eachCheckbox.getText();
                guiParticularVisualizations.add(visualizationName);
                guisToCreate.add(supportedVisualizations.get(visualizationName));
            }
        }

        meTheWindow.dispose();
        ADEGuiExternalFrame.createExternalGUIFrames(component, guisToCreate, persistentGUIs);
    }
}
