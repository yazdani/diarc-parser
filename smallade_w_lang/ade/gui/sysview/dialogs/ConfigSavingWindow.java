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
package ade.gui.sysview.dialogs;

import ade.ADEComponentInfo;
import ade.ADERegistry;
import ade.gui.InfoRequestSpecs;
import ade.gui.SystemViewComponentInfo;
import ade.gui.SystemViewStatusData;
import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import ade.gui.sysview.menu.ConfigMenu;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import org.apache.commons.io.IOUtils;

public class ConfigSavingWindow extends JPanel {

    private static final long serialVersionUID = 1L;
    private ConfigSavingWindow meTheWindow = this;
    private ADESystemView sysView;
    private ConfigMenu configMenu;
    private JComboBox registryComboBox;
    private SystemViewStatusData systemStatus;
    private JTextArea textAreaInfo;

    /**
     * Create the panel.
     *
     * @param sysView
     * @param meTheMenu
     */
    public ConfigSavingWindow(ADESystemView sysView, ConfigMenu configMenu) {
        this.sysView = sysView;
        this.configMenu = configMenu;

        setLayout(new BorderLayout(0, 0));

        JPanel topPanel = new JPanel();
        add(topPanel, BorderLayout.NORTH);
        topPanel.setLayout(new BorderLayout(0, 0));

        JButton btnRefresh = new JButton("Display");
        btnRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayRegistryConfigInfo();
            }
        });
        topPanel.add(btnRefresh, BorderLayout.EAST);

        registryComboBox = new JComboBox();
        topPanel.add(registryComboBox, BorderLayout.CENTER);

        JPanel topTopPanel = new JPanel();
        topPanel.add(topTopPanel, BorderLayout.NORTH);
        topTopPanel.setLayout(new BorderLayout(0, 0));

        JLabel lblHeader = new JLabel("Generate config file for components on which registry?");
        lblHeader.setBorder(new EmptyBorder(3, 5, 1, 5));
        topTopPanel.add(lblHeader);

        JButton btnHelp = new JButton(IconFetcher.get16x16icon("help-browser.png"));
        btnHelp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                showHelp();
            }
        });
        topTopPanel.add(btnHelp, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel();
        add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.setLayout(new BorderLayout(0, 0));

        JLabel lblNote = new JLabel("<html>Note that you may need to put \"pause 1000\""
                + "commands (1 second) between components, and/or re-order them.</html>");
        lblNote.setBorder(new EmptyBorder(2, 5, 2, 5));
        bottomPanel.add(lblNote);

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setBorder(new EmptyBorder(1, 5, 1, 0));
        bottomPanel.add(bottomButtonPanel, BorderLayout.SOUTH);
        bottomButtonPanel.setLayout(new BorderLayout(0, 0));

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                meTheWindow.setVisible(false);
            }
        });
        bottomButtonPanel.add(btnClose, BorderLayout.EAST);

        JPanel bottomButtonPanelInner = new JPanel();
        bottomButtonPanel.add(bottomButtonPanelInner, BorderLayout.WEST);
        bottomButtonPanelInner.setLayout(new BorderLayout(0, 0));

        JButton btnSave = new JButton("Save config file", IconFetcher.get16x16icon("media-floppy.png"));
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                promptToSaveConfigFile();
            }
        });
        bottomButtonPanelInner.add(btnSave);

        JButton btnCopyToClipboard = new JButton("Copy to Clipboard",
                IconFetcher.get16x16icon("accessories-text-editor.png"));
        btnCopyToClipboard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyToClipboard();
            }
        });
        bottomButtonPanelInner.add(btnCopyToClipboard, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        textAreaInfo = new JTextArea();
        textAreaInfo.setFont(new Font("Courier New", Font.PLAIN, 12));
        textAreaInfo.setLineWrap(true);
        scrollPane.setViewportView(textAreaInfo);



        // after gui elements all initialized:
        populateRegistryAndComponentInfoAndComboBox();

        registryComboBox.requestFocusInWindow();
    }

    private void promptToSaveConfigFile() {
        JFileChooser fileChooser = configMenu.createConfigFileChooser();
        int returnVal = fileChooser.showSaveDialog(sysView);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File file = UtilUI.getFileBasedOnFileChooserWithExtensionAppended(
                        fileChooser, ConfigMenu.EXTENSION);
                configMenu.setLastUsedConfigPathBasedOnFile(file);
                FileWriter fstream = new FileWriter(file.getAbsoluteFile());
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(textAreaInfo.getText());
                out.close();
            } catch (Exception e) {
                String message = "Could not write the config file due to the following "
                        + "exception:\n\n" + Util.stackTraceString(e);
                JOptionPane.showMessageDialog(this, UtilUI.messageInScrollPane(message),
                        "Error writing config file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copyToClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String allText = textAreaInfo.getText();
        StringSelection data = new StringSelection(allText);
        clipboard.setContents(data, data);
    }

    private void populateRegistryAndComponentInfoAndComboBox() {
        InfoRequestSpecs statusRequestSpecs = new InfoRequestSpecs();
        statusRequestSpecs.registryName = true;
        try {
            systemStatus = (SystemViewStatusData) sysView.callComponent(
                    "guiGetRegistryStatus", sysView.registryAccessKey, statusRequestSpecs, true);
            for (SystemViewComponentInfo eachComponent : systemStatus.componentIDtoInfoMap.values()) {
                if (eachComponent.type.equals(ADERegistry.class.getCanonicalName())) {
                    registryComboBox.addItem(eachComponent.name);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Could not obtain information about registries currently in the system.",
                    "Error loading Config-Saver", JOptionPane.ERROR_MESSAGE);
        }

        // try to select current registry:
        try {
            registryComboBox.setSelectedItem(sysView.registryName);
        } catch (Exception e) {
            // whatever.
        }

        if (registryComboBox.getItemCount() == 1) {
            displayRegistryConfigInfo();
        }
    }

    private void displayRegistryConfigInfo() {
        if (registryComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(null,
                    "Please select a registry first.",
                    "Could not populate config specs", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            ADEComponentInfo.writeConfigFormat(printWriter, null);
            printWriter.println("#");
            // for each component in entire system, check if is component and matches registry spec,
            //     and, if so, request info for it and write to print writer:
            for (SystemViewComponentInfo eachComponentViewInfo : systemStatus.componentIDtoInfoMap.values()) {
                if (!eachComponentViewInfo.type.equals(ADERegistry.class.getCanonicalName())) {
                    if (eachComponentViewInfo.registryName.equals(registryComboBox.getSelectedItem().toString())) {
                        String text = (String) sysView.callComponent("guiGetComponentConfigText",
                                sysView.registryAccessKey,
                                Util.getKey(eachComponentViewInfo.type, eachComponentViewInfo.name),
                                true);
                        printWriter.append(text);
                    }
                }
            }

            printWriter.flush();
            printWriter.close();
            stringWriter.flush();

            textAreaInfo.setText(stringWriter.toString());
            textAreaInfo.setSelectionStart(0);
            textAreaInfo.setSelectionEnd(0);

            stringWriter.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    UtilUI.messageInScrollPane("Could not load config info for registry, "
                    + "exception was as follows: \n\n" + e),
                    "Could not populate config specs", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    protected void showHelp() {
        String helpText;
        try {
            InputStream helpTextStream = this.getClass().getResourceAsStream("configFileHelpText.txt");
            helpText = IOUtils.toString(helpTextStream, "UTF-8");
        } catch (Exception e) {
            helpText = "Could not read cached help instructions for config file.  Please visit "
                    + "http://hri.cogs.indiana.edu/hrilab/index.php/ADE_Quick_Start#Registry_Configuration_Files";
        }
        JOptionPane.showMessageDialog(sysView, UtilUI.messageInScrollPane(helpText),
                "Help for writing a config file", JOptionPane.INFORMATION_MESSAGE);

    }
}
