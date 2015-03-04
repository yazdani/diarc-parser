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

import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import ade.gui.sysview.graph.ComponentGraphWindow;
import ade.gui.sysview.table.ComponentTableWindow;
import ade.gui.sysview.windows.Lambdas;
import ade.gui.sysview.windows.Lambdas.WindowIterator;
import ade.gui.sysview.windows.OtherWindowType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import utilities.Pair;

public class WindowMenu extends JMenu {

    private static final long serialVersionUID = 1L;
    private ADESystemView sysView;
    public JCheckBoxMenuItem componentTableMenuItem;
    public JCheckBoxMenuItem componentGraphMenuItem;

    public WindowMenu(final ADESystemView sysView) {
        super("Window");

        this.sysView = sysView;

        this.add(createComponentTableMenuItem());
        this.add(createComponentGraphMenuItem());

        this.add(new JSeparator());


        this.add(createBringToFrontMenuItem(
                "Bring all visualizations to front", sysView.visualizationManager.getVisualizationIteratorLambda()));
        this.add(createMinimizeMenuItem(
                "Minimize all visualizations", sysView.visualizationManager.getVisualizationIteratorLambda()));
        this.add(createCloseMenuItem("Close all visualizations",
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        sysView.visualizationManager.closeAllVisualizations();
                    }
                }));



        this.add(new JSeparator());

        WindowIterator otherWindowsIterator = new Lambdas.WindowIterator() {
            @Override
            public Iterator<? extends JInternalFrame> getWindows() {
                return sysView.otherWindows.values().iterator();
            }
        };

        this.add(createBringToFrontMenuItem(
                "Bring all other windows to front", otherWindowsIterator));
        this.add(createMinimizeMenuItem(
                "Minimize all other windows", otherWindowsIterator));
        this.add(createCloseMenuItem("Close all other windows",
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // note:  must use a while not empty loop, so that 
                        //     don't cause a concurrent modification exception.
                        while (!sysView.otherWindows.isEmpty()) {
                            Entry<Pair<OtherWindowType, String>, JInternalFrame> firstEntry =
                                    sysView.otherWindows.entrySet().iterator().next();
                            firstEntry.getValue().dispose();

                            // in case closing the window does not automatically remove it
                            //     from sysView.otherWindows (windows such as OuputViewerWindow
                            //     and InfoViewerWindow do this automatically), remove the entry
                            //     manually:
                            sysView.otherWindows.remove(firstEntry.getKey());
                        }
                    }
                }));
    }

    private JMenuItem createCloseMenuItem(String title, ActionListener action) {
        JMenuItem menuItem = new JMenuItem(title,
                IconFetcher.get16x16icon("edit-clear.png"));
        menuItem.addActionListener(action);
        return menuItem;
    }

    private JMenuItem createMinimizeMenuItem(String title,
            final Lambdas.WindowIterator windowIteratorFetcher) {
        JMenuItem menuItem = new JMenuItem(title,
                IconFetcher.get16x16icon("go-bottom.png"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Iterator<? extends JInternalFrame> windowIterator =
                        windowIteratorFetcher.getWindows();
                while (windowIterator.hasNext()) {
                    JInternalFrame eachWindow = windowIterator.next();
                    try {
                        eachWindow.setIcon(true);
                    } catch (PropertyVetoException e1) {
                        // oh well...
                    }
                }
            }
        });
        return menuItem;
    }

    private JMenuItem createBringToFrontMenuItem(String title,
            final Lambdas.WindowIterator windowIteratorFetcher) {
        JMenuItem menuItem = new JMenuItem(title,
                IconFetcher.get16x16icon("go-top.png"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Iterator<? extends JInternalFrame> windowIterator =
                        windowIteratorFetcher.getWindows();
                while (windowIterator.hasNext()) {
                    JInternalFrame eachWindow = windowIterator.next();
                    try {
                        // bring out of being minimized, if it was.
                        eachWindow.setIcon(false);
                        // and bring to front:
                        eachWindow.toFront();
                    } catch (PropertyVetoException e1) {
                        // oh well...
                    }
                }
            }
        });
        return menuItem;
    }

    private JMenuItem createComponentGraphMenuItem() {
        componentGraphMenuItem = new JCheckBoxMenuItem("Component Graph", false);
        componentGraphMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComponentGraphWindow.showGraph(sysView, componentGraphMenuItem.isSelected());
            }
        });

        return componentGraphMenuItem;
    }

    private JMenuItem createComponentTableMenuItem() {
        componentTableMenuItem = new JCheckBoxMenuItem("Component Table", false);
        componentTableMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComponentTableWindow.showTable(sysView, componentTableMenuItem.isSelected());
            }
        });
        return componentTableMenuItem;
    }
}
