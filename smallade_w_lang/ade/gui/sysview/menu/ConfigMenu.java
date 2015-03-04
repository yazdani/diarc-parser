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
package ade.gui.sysview.menu;

import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import ade.gui.sysview.dialogs.ConfigSavingWindow;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class ConfigMenu extends JMenu {

    private static final long serialVersionUID = 1L;
    private ConfigMenu meTheMenu = this;
    private ADESystemView sysView;
    private String lastUsedConfigPath = null;
    public static final String EXTENSION = "config";

    public ConfigMenu(ADESystemView sysView) {
        super("Configuration");

        this.sysView = sysView;

        this.add(createLoadConfigMenuItem());
        this.add(createSaveConfigMenuItem());
    }

    private JMenuItem createLoadConfigMenuItem() {
        JMenuItem loadConfigMenuItem = new JMenuItem("Load configuration",
                IconFetcher.get16x16icon("document-open.png"));
        loadConfigMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                promptToLoadConfig();
            }
        });
        return loadConfigMenuItem;
    }

    private void promptToLoadConfig() {
        JFileChooser fileChooser = createConfigFileChooser();
        int returnVal = fileChooser.showOpenDialog(sysView);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File file = UtilUI.getFileBasedOnFileChooserWithExtensionAppended(
                        fileChooser, EXTENSION);
                setLastUsedConfigPathBasedOnFile(file);
                sysView.callComponent("guiLoadConfig", sysView.registryAccessKey, file.getAbsolutePath());
            } catch (Exception e) {
                String message = "Could not open the config file due to the following "
                        + "exception:\n\n" + Util.stackTraceString(e);
                JOptionPane.showMessageDialog(this, UtilUI.messageInScrollPane(message),
                        "Error opening config file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setLastUsedConfigPathBasedOnFile(File file) {
        lastUsedConfigPath = file.getAbsolutePath();
    }

    public JFileChooser createConfigFileChooser() {
        return UtilUI.createCustomFileChooser(
                lastUsedConfigPath, EXTENSION, "ADE config file (." + EXTENSION + ")");
    }

    private JMenuItem createSaveConfigMenuItem() {
        JMenuItem saveConfigMenuItem = new JMenuItem("Save current configuration",
                IconFetcher.get16x16icon("media-floppy.png"));
        saveConfigMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConfigSavingWindow configSaver = new ConfigSavingWindow(sysView, meTheMenu);
                sysView.createWindowForHelperPanel(configSaver, new Dimension(500, 400),
                        true, true, "Save current configuration", JFrame.DISPOSE_ON_CLOSE);
            }
        });
        return saveConfigMenuItem;
    }
}
