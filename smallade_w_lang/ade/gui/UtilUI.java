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

import ade.gui.icons.IconFetcher;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

public class UtilUI {

    /**
     * creates a JScrollPane with a text message inside, which can then, in
     * turn, be used as input to JOptionPane (showMessageDialog,
     * showConfirmDialog, etc). For example: JOptionPane.showMessageDialog(null,
     * messageInScrollPane(s)); This method accepts the text only and creates a
     * default-sized scroll-pane; there is also another method which accepts a
     * particular dimension as an argument.
     *
     * @param text
     * @return the scroll pane, formatted and ready to go.
     */
    public static JScrollPane messageInScrollPane(String text) {
        return messageInScrollPane(text, new Dimension(450, 300));
    }

    /**
     * creates a JScrollPane with a text message inside, which can then, in
     * turn, be used as input to JOptionPane (showMessageDialog,
     * showConfirmDialog, etc). For example: JOptionPane.showMessageDialog(null,
     * messageInScrollPane(s));
     *
     * @param text
     * @param scrollPaneDimension
     * @return the scroll pane, formatted and ready to go.
     */
    public static JScrollPane messageInScrollPane(String text,
            Dimension scrollPaneDimension) {
        return messageInScrollPane(text, scrollPaneDimension, null);
    }

    /**
     * creates a JScrollPane with a text message inside, which can then, in
     * turn, be used as input to JOptionPane (showMessageDialog,
     * showConfirmDialog, etc). For example: JOptionPane.showMessageDialog(null,
     * messageInScrollPane(s));
     *
     * @param text
     * @param scrollPaneDimension
     * @param font
     * @return the scroll pane, formatted and ready to go.
     */
    public static JScrollPane messageInScrollPane(String text,
            Dimension scrollPaneDimension, Font font) {
        JTextArea textArea = new JTextArea(text);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setMargin(new Insets(3, 3, 3, 3));
        textArea.setWrapStyleWord(true);
        if (font != null) {
            textArea.setFont(font);
        }

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(scrollPaneDimension);
        return scrollPane;
    }

    public static Component createStretchablePanelForComponent(Component moreInfo, int leftRightMargin) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(moreInfo, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(0, leftRightMargin, 0, leftRightMargin));
        return panel;
    }

    public static Component createStretchablePanelForComponent(Component moreInfo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(moreInfo, BorderLayout.CENTER);
        return panel;
    }

    public static JLabel createClickableLabel(String icon16x16name, final ActionListener action) {
        JLabel label = new JLabel(IconFetcher.get16x16icon(icon16x16name));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.actionPerformed(null);
            }
        });
        return label;
    }

    /**
     * creates a file chooser with a filter for a particular extension
     *
     * @param currentDirectoryPath (optional; there is also a version without
     * this arg).
     * @param acceptedExtension (with NO period. e.g., just "txt")
     * @param description
     * @return jFileChooser
     */
    public static JFileChooser createCustomFileChooser(
            String currentDirectoryPath,
            final String acceptedExtension, final String description) {
        if (currentDirectoryPath == null) {
            currentDirectoryPath = System.getProperty("user.dir");
        }
        JFileChooser fc = new JFileChooser(currentDirectoryPath);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(
                new FileFilter() {
                    //Accept all directories and EXTENSION files
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }

                        String extension = GetExtension(f);
                        if ((extension != null) && (extension.equals(acceptedExtension))) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    private String GetExtension(File f) {
                        String ext = null;
                        String s = f.getName();
                        int i = s.lastIndexOf('.');

                        if (i > 0 && i < s.length() - 1) {
                            ext = s.substring(i + 1).toLowerCase();
                        }
                        return ext;
                    }

                    //The description of this filter
                    public String getDescription() {
                        return description;
                    }
                });

        return fc;
    }

    /**
     * creates a file chooser with a filter for a particular extension
     *
     * @param acceptedExtension (with NO period. e.g., just "txt")
     * @param description
     * @return jFileChooser
     */
    public static JFileChooser createCustomFileChooser(
            final String acceptedExtension, final String description) {
        return createCustomFileChooser(null, acceptedExtension, description);
    }

    /**
     * gets the file from a file chooser, appending the extension, if not
     * already specified
     */
    public static File getFileBasedOnFileChooserWithExtensionAppended(
            JFileChooser fc, String acceptedExtension) {
        String name = fc.getSelectedFile().getAbsolutePath();
        if (!(name.substring(name.length() - acceptedExtension.length() - 1)
                .equals("." + acceptedExtension))) {
            name += "." + acceptedExtension;
        }

        return new File(name);
    }

    public static ArrayList<Component> getAllComponents(final Container container) {
        Component[] comps = container.getComponents();
        ArrayList<Component> compList = new ArrayList<Component>();
        for (Component comp : comps) {
            compList.add(comp);
            if (comp instanceof Container) {
                compList.addAll(getAllComponents((Container) comp));
            }
        }
        return compList;
    }
}
