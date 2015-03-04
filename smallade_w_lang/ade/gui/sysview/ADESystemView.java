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
package ade.gui.sysview;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import ade.gui.InfoRequestSpecs;
import ade.gui.SystemViewAccess;
import ade.gui.SystemViewStatusData;
import ade.gui.sysview.graph.ComponentGraphWindow;
import ade.gui.sysview.menu.MenuBar;
import ade.gui.sysview.table.ComponentTableWindow;
import ade.gui.sysview.windows.OtherWindowType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import prefuse.Display;
import utilities.Pair;

public class ADESystemView extends ADEGuiPanel {

    private static final long serialVersionUID = 1L;
    public static Color BACKGROUND_COLOR = new Color(169, 192, 255);
    // info to store:
    public String registryName;
    public SystemViewAccess registryAccessKey; // public so gui components can also use 
    //    it for their calls.
    public VisualizationManager visualizationManager; // manages all of the GUI panels
    // gui elements
    private JDesktopPane desktopPane;
    public MenuBar menuBar;
    // "special" windows:
    public ComponentTableWindow componentTable;
    public ComponentGraphWindow componentGraph;
    public ArrayList<String> cachedClasspathComponentNames = new ArrayList<String>();
    //     for component launcher window, so that once indexed once, 
    //     don't have to re-index the next time the window is brought up.
    //     initialize to empty array to avoid NullPointerException handling.
    // a map of <window-type, component name> pair, to a particular window.  That way,
    //    context menus can know whether a window of particular type (output viewer,
    //    info window), is open.  Note that these windows do not have any automatic
    //    shutdown performed for them (e.g., when a component is closed, because might
    //    well want to keep their information on record.
    public HashMap<Pair<OtherWindowType, String>, JInternalFrame> otherWindows =
            new HashMap<Pair<OtherWindowType, String>, JInternalFrame>();

    public ADESystemView(ADEGuiCallHelper guiCallHelper,
            String registryName, SystemViewAccess registryAccessKey) {
        super(guiCallHelper, 1000);

        this.registryName = registryName;
        this.registryAccessKey = registryAccessKey;

        this.visualizationManager = new VisualizationManager(this);

        // turn off prefuse logging for everything but severe errors. 
        String prefusePackageName = Display.class.getPackage().getName();
        Logger.getLogger(prefusePackageName).setLevel(Level.SEVERE);

        initPanel();

        addResizeListener();
    }

    private void addResizeListener() {
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension currentSize = desktopPane.getSize();
                for (Component eachComponent : desktopPane.getComponents()) {
                    fitComponentIntoDestopPaneBounds(eachComponent, currentSize);
                }
            }
        });
    }

    private void fitComponentIntoDestopPaneBounds(
            Component component, Dimension desktopSize) {
        // only resize for reasonable window sizes -- in particular,
        //    prevents problems on initial launch, when window might be
        //    tiny for the first fraction of a second.
        if ((desktopSize.height < 100) || (desktopSize.width < 100)) {
            return;
        }


        // size first
        if (component.getWidth() > desktopSize.width) {
            component.setSize(desktopSize.width, component.getHeight());
        }
        if (component.getHeight() > desktopSize.height) {
            component.setSize(component.getWidth(), desktopSize.height);
        }


        // then placement

        // ensure top left is fine:
        if (component.getBounds().getMinX() < 0) {
            component.setLocation(0, component.getY());
        }
        if (component.getBounds().getMinY() < 0) {
            component.setLocation(component.getX(), 0);
        }


        if (component.getBounds().getMaxX() > desktopSize.width) {
            component.setLocation(desktopSize.width - component.getWidth(), component.getY());
        }
        if (component.getBounds().getMaxY() > desktopSize.height) {
            component.setLocation(component.getX(), desktopSize.height - component.getHeight());
        }
    }

    @Override
    public void isLoadedCallback(boolean isInternalWindow) {
        // load with showing component table and component graph.

        int windowWidth = getInitSize(isInternalWindow).width;
        int windowHeight = getInitSize(isInternalWindow).height;
        int windowThirdHeight = (int) (windowHeight / 3.0);

        ComponentTableWindow.showTable(this, true);
        // and lay it out at the very top 1/3 of the screen:
        this.componentTable.setSize(windowWidth, windowThirdHeight);

        ComponentGraphWindow.showGraph(this, true);
        // and lay it out at the bottom top 1/2 of the screen
        //    (don't want to reach fully down, the very bottom is where
        //     minimized windows go)
        this.componentGraph.setLocation(0, windowThirdHeight);

        // don't want the graph to reach all the way down to the bottom of 
        //     the windows, as that's where minimized windows go.
        int graphSizeHeight = (int) Math.min(windowHeight * 0.55, windowWidth);
        // want the width to be at least that of height (e.g., at least square,
        //     but ideally a little longer, so that can fit long component names better)
        int graphSizeWidth = (int) Math.max(windowWidth * 0.7, graphSizeHeight);

        this.componentGraph.setSize(graphSizeWidth, graphSizeHeight);

    }

    @Override
    public void refreshGui() {
        try {
            boolean showingTable = windowIsShowing(this.componentTable);
            boolean showingGraph = windowIsShowing(this.componentGraph);

            InfoRequestSpecs statusRequestSpecs = new InfoRequestSpecs();
            statusRequestSpecs.host = (showingTable);
            statusRequestSpecs.registryName = (showingTable || showingGraph);
            statusRequestSpecs.clients = (showingGraph);

            SystemViewStatusData newStatus = (SystemViewStatusData) callComponent(
                    "guiGetRegistryStatus", registryAccessKey, statusRequestSpecs, true);


            if (showingTable) {
                componentTable.refreshGUI(newStatus);
            }
            if (showingGraph) {
                componentGraph.refreshGUI(newStatus);
            }


            visualizationManager.removeVisualizationsForNoLongerListedComponents(newStatus);

        } catch (Exception e) {
            System.err.println("Could not obtain registry status info for " + registryName
                    + ".  If the problem persists, this will not end well.");
            e.printStackTrace();
        }
    }

    private boolean windowIsShowing(JInternalFrame window) {
        if (window == null) {
            return false;
        } else {
            return window.isVisible() && (!window.isIcon()) && (!window.isClosed());
        }
    }

    private void initPanel() {
        this.setLayout(new BorderLayout());

        this.menuBar = new MenuBar(this);
        this.add(menuBar, BorderLayout.NORTH);

        initDesktopPane();
    }

    private void initDesktopPane() {
        this.desktopPane = new JDesktopPane();
        this.desktopPane.setBackground(BACKGROUND_COLOR);
        this.add(this.desktopPane, BorderLayout.CENTER);
    }

    @Override
    public Dimension getInitSize(boolean isInternalWindow) {
        if (isInternalWindow) {
            return new Dimension(400, 300);
        } else {
            return new Dimension(900, 750);
        }
    }

    /**
     * adds window to desktop pane, sets it to visible, fits it inside the
     * system view frame, and activates it.
     */
    public void addWindow(JInternalFrame window) {
        // try to prevent windows from stacking on top of each other.
        Point currentLocation = new Point(0, 0);
        final int WINDOW_OFFSET_AMOUNT = 20;

        while (!windowLocationIsFree(currentLocation)) {
            currentLocation.x += WINDOW_OFFSET_AMOUNT;
            currentLocation.y += WINDOW_OFFSET_AMOUNT;
        }


        this.desktopPane.add(window);
        window.setLocation(currentLocation);

        // show the window -- which will also ensure it didn't jump out of bounds.
        showWindow(window);
    }

    private boolean windowLocationIsFree(Point currentLocation) {
        for (Component each : this.desktopPane.getComponents()) {
            if (each.getLocation().equals(currentLocation)) {
                return false;
            }
        }
        // if no matches, return true
        return true;
    }

    /**
     * sets a window to visible, fits it inside the system view frame, and
     * activates it.
     */
    public void showWindow(JInternalFrame window) {
        window.setVisible(true);

        // try to de-iconofy window
        try {
            window.setIcon(false);
        } catch (PropertyVetoException e) {
            // oh well.  if the iconifiable property is vetoed,
            //    then wouldn't have been iconified anyway, so 
            //    it's a rather moot point.
        }

        fitComponentIntoDestopPaneBounds(window, desktopPane.getSize());
        this.desktopPane.getDesktopManager().activateFrame(window);
    }
}
