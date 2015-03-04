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

import ade.gui.ADEGuiInternalFrame;
import ade.gui.ADEGuiPanel;
import ade.gui.SystemViewStatusData;
import ade.gui.sysview.windows.Lambdas;
import ade.gui.sysview.windows.Lambdas.WindowIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JInternalFrame;

/**
 * manages the handling (creating, counting, removing, etc) of visualizations
 * for SystemView
 */
public class VisualizationManager {

    // the "crux" of the manager:  a map of COMPONENT NAMES to a 
    //      map of visualization types and a collection of the actual visualizations
    private final HashMap<String, HashMap<String, HashSet<ADEGuiInternalFrame>>> componentIDtoVisualizationsMap;
    // equally important, a hashset of the the visualiations (for efficiency when
    //      operating with all visualizations, in particular when updating).
    private final HashSet<ADEGuiInternalFrame> allVisualizations;
    private final ADESystemView sysView;
    private final Lambdas.WindowIterator visualizationIteratorLambda;

    public VisualizationManager(ADESystemView sysView) {
        this.sysView = sysView;
        componentIDtoVisualizationsMap = new HashMap<String, HashMap<String, HashSet<ADEGuiInternalFrame>>>();
        allVisualizations = new HashSet<ADEGuiInternalFrame>();

        this.visualizationIteratorLambda = new Lambdas.WindowIterator() {
            @Override
            public Iterator<? extends JInternalFrame> getWindows() {
                return getWindowIterator();
            }
        };
    }

    public synchronized int countComponentVisualizations(String componentID, String visType) {
        if (!componentIDtoVisualizationsMap.containsKey(componentID)) {
            return 0;
        }

        HashMap<String, HashSet<ADEGuiInternalFrame>> componentVisualizations =
                componentIDtoVisualizationsMap.get(componentID);
        if (!componentVisualizations.containsKey(visType)) {
            return 0;
        }

        HashSet<ADEGuiInternalFrame> desiredVisTypeSet = componentVisualizations.get(visType);
        return desiredVisTypeSet.size();
    }

    public synchronized void addComponentVisualization(String componentID, String visType, ADEGuiPanel guiPanel) {
        // first, create frame, which will also take care of closing listeners, etc.
        ADEGuiInternalFrame guiFrame = new ADEGuiInternalFrame(guiPanel, this, componentID, visType);
        sysView.addWindow(guiFrame);


        // having created frame, register it with the visualization map hashset:

        if (!componentIDtoVisualizationsMap.containsKey(componentID)) {
            componentIDtoVisualizationsMap.put(componentID, new HashMap<String, HashSet<ADEGuiInternalFrame>>());
        }

        HashMap<String, HashSet<ADEGuiInternalFrame>> componentVisualizations =
                componentIDtoVisualizationsMap.get(componentID);
        if (!componentVisualizations.containsKey(visType)) {
            componentVisualizations.put(visType, new HashSet<ADEGuiInternalFrame>());
        }

        HashSet<ADEGuiInternalFrame> desiredVisTypeSet = componentVisualizations.get(visType);
        desiredVisTypeSet.add(guiFrame);


        // also add to allVisualizations
        allVisualizations.add(guiFrame);
    }

    public synchronized void removeComponentVisualization(
            ADEGuiInternalFrame guiFrame, String componentID, String visType) {
        // can assume that the super-structure of name and type already exists.  The question is,
        //     rather, how much can remove...
        HashMap<String, HashSet<ADEGuiInternalFrame>> componentVisualizations =
                componentIDtoVisualizationsMap.get(componentID);
        HashSet<ADEGuiInternalFrame> desiredVisTypeSet = componentVisualizations.get(visType);
        desiredVisTypeSet.remove(guiFrame);

        // check if that cleared the hashset:
        if (desiredVisTypeSet.size() == 0) {
            componentVisualizations.remove(desiredVisTypeSet);

            // check if that, in turn, was the only member of componentVisualizations
            if (componentVisualizations.size() == 0) {
                componentIDtoVisualizationsMap.remove(componentID);
            }
        }

        // also remove from allVisualizations
        allVisualizations.remove(guiFrame);
    }

    public synchronized void removeAllComponentVisualizations(String componentID, String visType) {
        if (!componentIDtoVisualizationsMap.containsKey(componentID)) {
            return;
        }

        HashMap<String, HashSet<ADEGuiInternalFrame>> componentVisualizations =
                componentIDtoVisualizationsMap.get(componentID);
        if (!componentVisualizations.containsKey(visType)) {
            return;
        }

        HashSet<ADEGuiInternalFrame> desiredVisTypeSet = componentVisualizations.get(visType);

        // disposing a window will immediately cause it to turn around and remove itself from
        //     the hashset.  So, to avoid concurrency modification errors, using a "while not empty" loop
        while (!desiredVisTypeSet.isEmpty()) {
            desiredVisTypeSet.iterator().next().dispose();
            // again, note that this will ultimately call the removeVisualization method,
            //     so the visualization will be removed both from visMap and from allVisualizations.
        }

        // remove the entire hashmap of this component's visTypes of this type, since it is now
        //     empty anyway, and leaving it feels like leaving garbage behind.
        componentVisualizations.remove(visType);
    }

    public synchronized Iterator<ADEGuiInternalFrame> getWindowIterator() {
        return allVisualizations.iterator();
    }

    public synchronized void closeAllVisualizations() {
        // disposing a window will immediately cause it to turn around and remove itself from
        //     the hashset.  So, to avoid concurrency modification errors, using a "while not empty" loop
        while (!allVisualizations.isEmpty()) {
            allVisualizations.iterator().next().dispose();
        }
        // and now all that remains is to clear the entire visMap,
        //     which has the "skeleton" of the entire structure, but no 
        //     more visualizations.
        componentIDtoVisualizationsMap.clear();
    }

    public synchronized void removeVisualizationsForNoLongerListedComponents(
            SystemViewStatusData newStatus) {
        ArrayList<String> componentGUIsToRemove = new ArrayList<String>();
        for (String eachCurrentVisComponentID : componentIDtoVisualizationsMap.keySet()) {
            if (!newStatus.componentIDtoInfoMap.containsKey(eachCurrentVisComponentID)) {
                componentGUIsToRemove.add(eachCurrentVisComponentID);
            }
        }

        HashSet<ADEGuiInternalFrame> guisToRemove = new HashSet<ADEGuiInternalFrame>();
        for (String componentID : componentGUIsToRemove) {
            HashMap<String, HashSet<ADEGuiInternalFrame>> componentVisualizations =
                    componentIDtoVisualizationsMap.get(componentID);
            for (HashSet<ADEGuiInternalFrame> eachHashset : componentVisualizations.values()) {
                for (ADEGuiInternalFrame eachGUI : eachHashset) {
                    guisToRemove.add(eachGUI);
                }
            }
        }

        for (ADEGuiInternalFrame eachGUI : guisToRemove) {
            eachGUI.dispose();
        }

        // finally, remove the component names from the hashmap (it is already empty, courtesy of 
        //    visualization disposal)
        for (String eachComponentID : componentGUIsToRemove) {
            componentIDtoVisualizationsMap.remove(eachComponentID);
        }
    }

    public WindowIterator getVisualizationIteratorLambda() {
        return visualizationIteratorLambda;
    }
}
