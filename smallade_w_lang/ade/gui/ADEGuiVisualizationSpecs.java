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
import java.io.Serializable;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.ImageIcon;

/**
 * A data structure that defines the component's visualizations. See
 * ADEComponentImpl's "getVisualizationSpecs()
 */
public class ADEGuiVisualizationSpecs implements Serializable {

    private static final long serialVersionUID = 1L;
    private static ImageIcon defaultIcon = IconFetcher.get16x16icon("window-new.png");
    private TreeMap<String, Item> items;

    public ADEGuiVisualizationSpecs() {
        items = new TreeMap<String, Item>();
    }

    /**
     * adds an entry for a component visualization
     *
     * @param visualizationName. The name should ideally be short and contain no
     * spaces. Examples include "camera", "lasers", "STM", etc. The name is used
     * in the component context menu of ADE's SystemView, and in specifying the
     * visualization by name on the command-line. The name must be UNIQUE within
     * a particular component's list of visualization.
     * @param classType. The visualization's class object (i.e.,
     * VisualizationX.class)
     * @param startupOption. Defines whether the GUI should always show, show on
     * a --GUI flag, or show by explicit request only (for resource-intensive
     * visualizations).
     * @param icon. Defines custom icon for the visualization.
     * @param additionalArguments. Any additional arguments that the
     * visualization may want to take, AFTER the required GUICallHelper, which
     * is handled automatically.
     */
    public void add(String visualizationName, Class<?> classType,
            StartupOption startupOption, ImageIcon icon,
            Object... additionalArguments) {
        visualizationName.trim(); // spaces would be a pain in specifying the 
        //     name via commandline, so at least trim any start and end spaces.

        if (visualizationName.length() == 0) {
            throw new Error("The visualization name may not be blank");
        }
        if (items.containsKey(visualizationName)) {
            throw new Error("The visualization name must be unique within a given "
                    + "component's list of visualizations");
        }

        // if still here, all good:
        items.put(visualizationName, new Item(
                classType, startupOption, icon, additionalArguments));
    }

    /**
     * adds an entry for a component visualization, with a default GUI startup
     * option (e.g., show on -g / --G with no arguments, or when explicitly
     * requested by name)
     *
     * @param visualizationName. The name should ideally be short and contain no
     * spaces. Examples include "camera", "lasers", "STM", etc. The name is used
     * in the component context menu of ADE's SystemView, and in specifying the
     * visualization by name on the command-line. The name must be UNIQUE within
     * a particular component's list of visualization.
     * @param classType. The visualization's class object (i.e.,
     * VisualizationX.class)
     * @param additionalArguments. Any additional arguments that the
     * visualization may want to take, AFTER the required GUICallHelper, which
     * is handled automatically.
     */
    public void add(String visualizationName, Class<?> classType,
            Object... additionalArguments) {
        add(visualizationName, classType, StartupOption.SHOW_ON_GUI_FLAG,
                defaultIcon, additionalArguments);
    }

    /**
     * ********** A FEW COLLECTION ACCESSOR FUNCTIONS *************
     */
    public int size() {
        return items.size();
    }

    public Collection<String> keySet() {
        return items.keySet();
    }

    public Set<Entry<String, ADEGuiVisualizationSpecs.Item>> entrySet() {
        return items.entrySet();
    }

    public Collection<ADEGuiVisualizationSpecs.Item> values() {
        return items.values();
    }

    public ADEGuiVisualizationSpecs.Item get(String visualizationName) {
        return items.get(visualizationName);
    }

    /**
     * A class responsible for the individual GUI specs of a particular
     * visualization item (an individual visualization)
     */
    public class Item implements Serializable {

        private static final long serialVersionUID = 1L;
        public Class<?> classType;
        public StartupOption startupOption;
        public ImageIcon icon;
        public Object[] additionalArguments;

        /**
         * constructor for ADEGuiVisualizationSpecs.
         *
         * @param classType. The visualization's class object (i.e.,
         * VisualizationX.class)
         * @param startupOption. Defines whether the GUI should always show,
         * show only on a --GUI flag, or show by explicit request only (for
         * resource-intensive visualizations).
         * @param additionalArguments. Any additional arguments that the
         * visualization may want to take, AFTER the required ADEGuiCallHelper,
         * which is handled automatically for you.
         */
        private Item(Class<?> classType, StartupOption startupOption,
                ImageIcon icon, Object... additionalArguments) {
            this.classType = classType;
            this.startupOption = startupOption;
            this.icon = icon;
            this.additionalArguments = additionalArguments;
        }
    }

    public enum StartupOption {

        ALWAYS_SHOW, // show the visualization even if the -G flag is NOT specified.
        //    essentially, ALWAYS_SHOW means there's no point to run the component without the GUI.

        SHOW_ON_GUI_FLAG, // show the visualization if the -g or --gui flag is specified,
        //    and no particular visualizations are requested.  This is the "normal" case.

        SHOW_BY_EXPLICIT_REQUEST_ONLY // show the visualization ONLY if it is explicitly specified
        //    by name.  Use this for visualizations that are not used often, or are 
        //    resource-intensive.
    }
}
