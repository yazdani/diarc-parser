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

import ade.ADEComponent;
import ade.ADERegistry;
import ade.gui.InfoRequestSpecs;
import ade.gui.SystemViewComponentInfo;
import ade.gui.Util;
import ade.gui.UtilUI;
import ade.gui.icons.IconFetcher;
import ade.gui.sysview.ADESystemView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

public class ComponentLauncherWindow extends JPanel {

    private static final long serialVersionUID = 1L;
    public static final String DOT_CLASS_SUFFIX = ".class";
    public static final int DOT_CLASS_SUFFIX_LENGTH = DOT_CLASS_SUFFIX.length();
    public static final String IMPL_SUFFIX = "Impl";
    public static final int IMPL_SUFFIX_LENGTH = IMPL_SUFFIX.length();
    public static final String IMPL_DOT_CLASS_SUFFIX = IMPL_SUFFIX + DOT_CLASS_SUFFIX;
    public static final Class<?> ADE_COMPONENT_INTERFACE = ADEComponent.class;
    private ADESystemView sysView;
    private ComponentLauncherWindow meTheLauncher = this;
    private ArrayList<String> allComponentNamesSorted;
    private final JPanel step1Panel = new JPanel();
    private JList componentList;
    private DefaultListModel componentListModel;
    private JTextField nameSearchField;
    private JTable optionsTable;
    private DefaultTableModel optionsTableModel;
    private JTabbedPane tabbedPane;
    private JPanel panelComponentStartedSuccessfullyMessage;
    private JCheckBox checkboxShowAllConfigFields;
    private JToggleButton buttonRefresh;
    private SystemViewComponentInfo registryInfo;
    private JLabel step2LabelChosenComponent;

    protected class TableRowHeaderItem {

        public String label;
        public boolean visibleByDefault;
        public String defaultValue;

        public TableRowHeaderItem(String label, boolean visibleByDefault) {
            this(label, visibleByDefault, "");
        }

        public TableRowHeaderItem(String label, boolean visibleByDefault, String defaultValue) {
            this.label = label;
            this.visibleByDefault = visibleByDefault;
            this.defaultValue = defaultValue;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Create the dialog.
     *
     * @param sysView
     */
    public ComponentLauncherWindow(ADESystemView sysView) {
        this.sysView = sysView;

        setBounds(100, 100, 450, 300);
        this.setLayout(new BorderLayout());
        {
            componentListModel = new DefaultListModel();
        }
        {
            tabbedPane = new JTabbedPane(JTabbedPane.TOP);
            tabbedPane.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    tabbedPaneStateChanged();
                }
            });
            add(tabbedPane, BorderLayout.CENTER);
            tabbedPane.addTab("Step 1: Pick Component", null, step1Panel, null);
            step1Panel.setBorder(new EmptyBorder(5, 5, 5, 5));
            step1Panel.setLayout(new BorderLayout(0, 0));
            {
                JPanel panel = new JPanel();
                step1Panel.add(panel, BorderLayout.NORTH);
                panel.setLayout(new BorderLayout(0, 0));
                {
                    JLabel lblType = new JLabel("Type: ");
                    panel.add(lblType, BorderLayout.WEST);
                }
                {
                    nameSearchField = new JTextField();
                    nameSearchField.getDocument().addDocumentListener(new DocumentListener() {
                        @Override
                        public void removeUpdate(DocumentEvent e) {
                            searchFieldChanged();
                        }

                        @Override
                        public void insertUpdate(DocumentEvent e) {
                            searchFieldChanged();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                            searchFieldChanged();
                        }
                    });

                    panel.add(nameSearchField, BorderLayout.CENTER);
                    nameSearchField.setColumns(10);
                }
                {
                    JPanel panel_1 = new JPanel();
                    panel.add(panel_1, BorderLayout.EAST);
                    panel_1.setLayout(new GridLayout(0, 2, 6, 0));
                    {
                        buttonRefresh = new JToggleButton(IconFetcher.get16x16icon("view-refresh.png"));
                        panel_1.add(buttonRefresh);
                        {
                            JButton buttonInfo = new JButton(IconFetcher.get16x16icon("help-browser.png"));
                            buttonInfo.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    String helpText = "Please enter the fully-qualified name of the component type "
                                            + "\n(e.g., \"com.adesim.ADESimEnvironmentComponent\") into the type text field, "
                                            + "\nor select a type from the list below.  "
                                            + "\n\nThe available types will automatically filter as you type in the text field."
                                            + "\n\nAlso, please note that only compiled ADE components within "
                                            + "\nthe current classpath will be shown.";
                                    JOptionPane.showMessageDialog(meTheLauncher, helpText,
                                            "Help for choosing a component type", JOptionPane.INFORMATION_MESSAGE);
                                }
                            });
                            panel_1.add(buttonInfo);
                        }
                        buttonRefresh.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                reIndexComponentPossiblities();
                            }
                        });
                    }
                }
            }
            componentList = new JList(componentListModel);
            // on double-clicking an item in the componentList, advance to next step in Wizard:
            componentList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = componentList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            switchToStep2();
                        }
                    }
                }
            });
            componentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(componentList);
            step1Panel.add(scrollPane, BorderLayout.CENTER);
            {
                JPanel panelBottom = new JPanel();
                panelBottom.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));
                step1Panel.add(panelBottom, BorderLayout.SOUTH);
                panelBottom.setLayout(new BorderLayout(0, 0));
                {
                    JPanel panelRightButtons = new JPanel();
                    panelBottom.add(panelRightButtons, BorderLayout.CENTER);
                    panelRightButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));
                    {
                        JButton btnNext = new JButton("Next", IconFetcher.get16x16icon("start-here.png"));
                        btnNext.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                switchToStep2();
                            }
                        });
                        btnNext.setFont(new Font("Lucida Grande", Font.BOLD, 13));
                        panelRightButtons.add(btnNext);
                    }
                    {
                        JButton buttonCancel = new JButton(IconFetcher.get16x16icon("process-stop.png"));
                        buttonCancel.setText("Cancel");
                        buttonCancel.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
                        buttonCancel.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                meTheLauncher.setVisible(false);
                            }
                        });
                        panelRightButtons.add(buttonCancel);
                    }
                }

                JPanel panelLeftButtons = new JPanel();
                panelBottom.add(panelLeftButtons, BorderLayout.WEST);
                JButton buttonClearSelection = new JButton("Clear selection", IconFetcher.get16x16icon("edit-clear.png"));
                buttonClearSelection.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        componentList.clearSelection();
                    }
                });
                panelLeftButtons.add(buttonClearSelection);
            }

            {
                JPanel step2Panel = new JPanel();
                tabbedPane.addTab("Step 2: Choose Options", null, step2Panel, null);
                step2Panel.setLayout(new BorderLayout(0, 0));
                {
                    JPanel panel_1 = new JPanel();
                    step2Panel.add(panel_1, BorderLayout.CENTER);
                    panel_1.setLayout(new BorderLayout(0, 0));
                    {
                        JScrollPane scrollPane_1 = new JScrollPane();
                        panel_1.add(scrollPane_1, BorderLayout.CENTER);
                        {
                            optionsTableModel = new DefaultTableModel() {
                                private static final long serialVersionUID = 1L;

                                @Override
                                // first column is NOT editable
                                public boolean isCellEditable(int row, int column) {
                                    return (column > 0);
                                }
                            ;
                            };
							optionsTable = new JTable(optionsTableModel);
                            optionsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
                            optionsTable.setShowGrid(true);
                            scrollPane_1.setViewportView(optionsTable);
                        }
                    }
                    {
                        JPanel panel_2 = new JPanel();
                        panel_1.add(panel_2, BorderLayout.SOUTH);
                        panel_2.setLayout(new BorderLayout(0, 0));
                        {
                            checkboxShowAllConfigFields = new JCheckBox("Show all config fields, not just the common ones.");
                            checkboxShowAllConfigFields.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    optionsTableModel.fireTableDataChanged();
                                }
                            });
                            panel_2.add(checkboxShowAllConfigFields);
                        }
                    }
                    {
                        step2LabelChosenComponent = new JLabel("Chosen component:");
                        step2LabelChosenComponent.setBorder(BorderFactory.createEmptyBorder(0, 2, 5, 2));
                        panel_1.add(step2LabelChosenComponent, BorderLayout.NORTH);
                    }
                    {
                        {
                            {
                                JPanel step2PanelSouth = new JPanel();
                                step2PanelSouth.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));
                                step2Panel.add(step2PanelSouth, BorderLayout.SOUTH);
                                step2PanelSouth.setLayout(new BorderLayout(0, 0));
                                JButton btnHelp = new JButton(IconFetcher.get16x16icon("help-browser.png"));
                                btnHelp.setText("Help ");
                                step2PanelSouth.add(btnHelp, BorderLayout.WEST);
                                {
                                    JPanel panel = new JPanel();
                                    step2PanelSouth.add(panel, BorderLayout.CENTER);
                                    panel.setLayout(new BorderLayout(0, 0));
                                    JPanel panelStep2Buttons = new JPanel();
                                    panel.add(panelStep2Buttons);
                                    panelStep2Buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
                                    {
                                        JButton btnRunComponent = new JButton("Run component!", IconFetcher.get16x16icon("start-here.png"));
                                        btnRunComponent.setFont(new Font("Lucida Grande", Font.BOLD, 13));
                                        btnRunComponent.addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                meTheLauncher.runComponent();
                                            }
                                        });
                                        panelStep2Buttons.add(btnRunComponent);
                                    }
                                    {
                                        JButton buttonStep2Cancel = new JButton("Cancel", IconFetcher.get16x16icon("process-stop.png"));
                                        buttonStep2Cancel.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
                                        buttonStep2Cancel.addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                meTheLauncher.setVisible(false);
                                            }
                                        });
                                        panelStep2Buttons.add(buttonStep2Cancel);
                                    }
                                    {
                                        panelComponentStartedSuccessfullyMessage = new JPanel();
                                        panelComponentStartedSuccessfullyMessage.setVisible(false);
                                        panel.add(panelComponentStartedSuccessfullyMessage, BorderLayout.SOUTH);
                                        panelComponentStartedSuccessfullyMessage.setLayout(new BorderLayout(0, 0));
                                        {
                                            JLabel lblComponentStartedSuccessfully = new JLabel(
                                                    "<html>A request to start the registy has been sent to the component.  To view "
                                                    + "component output, watch the Registry console window.  "
                                                    + "You may now close this window, or keep it open to launch more components.</html>");
                                            lblComponentStartedSuccessfully.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
                                            panelComponentStartedSuccessfullyMessage.add(lblComponentStartedSuccessfully);
                                            lblComponentStartedSuccessfully.setFont(new Font("Lucida Grande", Font.ITALIC, 11));
                                            lblComponentStartedSuccessfully.setForeground(new Color(0, 128, 0));
                                        }
                                        {
                                            JButton btnX = new JButton("X");
                                            btnX.setMaximumSize(new Dimension(30, 29));
                                            btnX.setMinimumSize(new Dimension(30, 29));
                                            btnX.addActionListener(new ActionListener() {
                                                public void actionPerformed(ActionEvent arg0) {
                                                    panelComponentStartedSuccessfullyMessage.setVisible(false);
                                                }
                                            });
                                            btnX.setPreferredSize(btnX.getMinimumSize());
                                            panelComponentStartedSuccessfullyMessage.add(btnX, BorderLayout.EAST);
                                        }
                                    }
                                }
                                btnHelp.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent arg0) {
                                        showHelpForComponentConfig();
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }


        initializeRegistryInfo();

        // initialize component list (either by re-indexing, or grabbing cached SystemView copy)
        allComponentNamesSorted = sysView.cachedClasspathComponentNames;
        if (allComponentNamesSorted.size() == 0) {
            componentList.setEnabled(false);
            componentListModel.addElement("<html>Populating a list of available compiled components,"
                    + "<br>based on the current classpath. <i>(This may take a moment...)</i></html>");
            reIndexComponentPossiblities();
        } else {
            searchFieldChanged(); // update the list
        }


        initializeConfigFileOptionPossibilities();


        // switch focus to name search field on showing the window.
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        nameSearchField.requestFocusInWindow();
                    }
                });
            }
        });
    }

    protected void tabbedPaneStateChanged() {
        // if switched back to step 1, aka panel index 0:
        if (tabbedPane.getSelectedIndex() == 0) {
            // hide the "component started successfully" message,
            //     as presumably came back to start another!

            if (panelComponentStartedSuccessfullyMessage != null) {
                // prevents GUI null-point exception on start
                panelComponentStartedSuccessfullyMessage.setVisible(false);
            }
        } // else if on step 2, aka panel index 1
        else if (tabbedPane.getSelectedIndex() == 1) {
            step2LabelChosenComponent.setText("<html>Chosen component: <b>"
                    + getChosenComponentName() + "</b></html>");
        }
    }

    /**
     * return the name of a selected listbox value, if any; or, if not, the
     * textfield name
     */
    private String getChosenComponentName() {
        if (componentList.getSelectedValue() == null) {
            return nameSearchField.getText().trim(); // try just the name field.
        } else {
            return componentList.getSelectedValue().toString();
        }
    }

    protected void switchToStep2() {
        tabbedPane.setSelectedIndex(1); // go to step 2, aka panel index 1.
    }

    private void initializeRegistryInfo() {
        InfoRequestSpecs registryInfoRequest = new InfoRequestSpecs();
        registryInfoRequest.startDirectory = true;
        registryInfoRequest.host = true;

        try {
            registryInfo = (SystemViewComponentInfo) sysView.callComponent(
                    "guiGetComponentStatus", sysView.registryAccessKey,
                    Util.getKey(ADERegistry.class.getName(), sysView.registryName),
                    registryInfoRequest, true);
        } catch (Exception e) {
            // fine, no big deal, a few fields won't get pre-populated:
            registryInfo = new SystemViewComponentInfo(); // blank one. 
        }
    }

    private ArrayList<TableRowHeaderItem> createConfigFileOptionsListExceptType(
            SystemViewComponentInfo registryInfo) {
        // Creates a list of table parameters, with their name, 
        //    whether or not to show by default, and default value.
        //    As far as leaving off "type" in the config file options list, the type is obviously
        //    carried over from step 1, and therefore should not be shown to the user and messed with.

        ArrayList<TableRowHeaderItem> options = new ArrayList<TableRowHeaderItem>();
        options.add(new TableRowHeaderItem("host", true, registryInfo.host));
        options.add(new TableRowHeaderItem("port", false));
        options.add(new TableRowHeaderItem("name", false));
        options.add(new TableRowHeaderItem("groups", false));
        options.add(new TableRowHeaderItem("conn", false));
        options.add(new TableRowHeaderItem("tmout", false));
        options.add(new TableRowHeaderItem("acc", false));
        options.add(new TableRowHeaderItem("startdirectory", true, registryInfo.startDirectory));
        options.add(new TableRowHeaderItem("userclasspath", true));
        options.add(new TableRowHeaderItem("javavmargs", true));
        options.add(new TableRowHeaderItem("componentargs", true));
        options.add(new TableRowHeaderItem("restarts", false));
        options.add(new TableRowHeaderItem("onlyonhosts", false));
        options.add(new TableRowHeaderItem("devices", false));
        options.add(new TableRowHeaderItem("configfile", false));
        options.add(new TableRowHeaderItem("credentials", false));
        options.add(new TableRowHeaderItem("debug", false));

        return options;
    }

    @SuppressWarnings("unchecked")
    private void initializeConfigFileOptionPossibilities() {
        optionsTableModel.addColumn("Parameter");
        optionsTableModel.addColumn("Value");

        for (TableRowHeaderItem eachOption : createConfigFileOptionsListExceptType(registryInfo)) {
            optionsTableModel.addRow(new Object[]{eachOption, eachOption.defaultValue});
        }

        RowFilter<?, ?> filter = new RowFilter<Object, Object>() {
            @Override
            public boolean include(RowFilter.Entry<? extends Object, ? extends Object> entry) {
                TableRowHeaderItem header = (TableRowHeaderItem) entry.getValue(0); // 0 = first column
                return (header.visibleByDefault || checkboxShowAllConfigFields.isSelected());
            }
        };
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(optionsTableModel);
        sorter.setRowFilter((RowFilter<? super DefaultTableModel, ? super Integer>) filter);
        optionsTable.setRowSorter(sorter);
    }

    protected void runComponent() {
        String componentNameToRun = getChosenComponentName();
        if (componentNameToRun.length() == 0) {
            tabbedPane.setSelectedIndex(0); // go to Step 1 tab.
            JOptionPane.showMessageDialog(meTheLauncher,
                    "Please select a component from the list on the \"Step 1\" tab, "
                    + "or type in the component name manually.",
                    "No component selected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // try to generate the config string:
        StringBuilder configBuilder = new StringBuilder("STARTCOMPONENT\n");

        // it looks like the table model can only be iterated when all elements are showing:
        RowSorter<? extends TableModel> formerSorter = optionsTable.getRowSorter();
        optionsTable.setRowSorter(null);

        configBuilder.append("type " + componentNameToRun + "\n");
        for (int i = 0; i < optionsTableModel.getRowCount(); i++) {
            String value = (String) optionsTable.getValueAt(i, 1);
            if (value != null) {
                value = value.trim();
                if (value.length() > 0) {
                    configBuilder.append(optionsTable.getValueAt(i, 0) + " " + value + "\n");
                }
            }
        }

        optionsTable.setRowSorter(formerSorter);

        configBuilder.append("ENDCOMPONENT");

        try {
            sysView.callComponent(
                    "guiRunComponent", sysView.registryAccessKey,
                    configBuilder.toString());
            panelComponentStartedSuccessfullyMessage.setVisible(true);
        } catch (Exception e) {
            String errorText = "Could not run the following configuration:\n\n"
                    + configBuilder.toString() + "\n\nDue to the following exception:\n\n"
                    + Util.stackTraceString(e);
            JOptionPane.showMessageDialog(meTheLauncher, UtilUI.messageInScrollPane(errorText),
                    "Could not run component", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void showHelpForComponentConfig() {
        StringBuilder helpTextBuilder = new StringBuilder(
                "See the text below, first for "
                + "USAGE information for this PARTICULAR COMPONENT, and then for general "
                + "instructions for filling out this table");
        helpTextBuilder.append("\n\n"
                + "<><><><><><><><><><><><><><><><><><>\n"
                + "===== COMPONENT USAGE INFORMATION =====\n"
                + "<><><><><><><><><><><><><><><><><><>\n\n");


        String componentUsageInfo;
        try {
            componentUsageInfo = (String) sysView.callComponent("guiGetComponentHelpText",
                    sysView.registryAccessKey, getChosenComponentName());
        } catch (Exception e1) {
            componentUsageInfo = "Could NOT obtain component usage information (you may"
                    + "try just runningthe component with --help for \"componentags\", and "
                    + "seeing the help information flit by on the Registry terminal window.)";
        }
        helpTextBuilder.append(componentUsageInfo);


        helpTextBuilder.append("\n\n\n"
                + "<><><><><><><><><><><><><><><><><><>\n"
                + "======= GENERAL INSTRUCTIONS =======\n"
                + "<><><><><><><><><><><><><><><><><><>\n"
                + "\n"
                + "The table on this screen allows you to specify various component parameters.  "
                + "With the exception of \"host\" and \"startdirectory\" "
                + "(which default to the registry host and start directory),"
                + "all fields are optional and will be filled in automatically with default values."
                + "Some less-often-used fields are initially hidden, but can be enabled by enabling "
                + "the \"Show all config fields\" checkbox."
                + "\n\n"
                + "A description of the config file format is below:\n\n");

        try {
            InputStream helpTextStream = this.getClass().getResourceAsStream("configFileHelpText.txt");
            helpTextBuilder.append(IOUtils.toString(helpTextStream, "UTF-8"));
        } catch (Exception e) {
            helpTextBuilder.append("Could not read cached help instructions for config file.  Please visit "
                    + "http://hri.cogs.indiana.edu/hrilab/index.php/ADE_Quick_Start#Registry_Configuration_Files");
        }


        JScrollPane scrollPane = UtilUI.messageInScrollPane(
                helpTextBuilder.toString(), new Dimension(600, 450),
                new Font("monospaced", Font.PLAIN, 11));

        JOptionPane.showMessageDialog(meTheLauncher, scrollPane,
                "Help for running a component", JOptionPane.INFORMATION_MESSAGE);
    }

    private synchronized void searchFieldChanged() {
        if (componentList.isEnabled()) {
            Object formerSelection = componentList.getSelectedValue();

            String name = nameSearchField.getText().trim();
            componentListModel.clear();
            if (name.length() == 0) {
                for (String eachName : allComponentNamesSorted) {
                    componentListModel.addElement(eachName);
                }
            } else {
                for (String eachName : allComponentNamesSorted) {
                    if (Util.wildCardMatch(eachName, name)) {
                        componentListModel.addElement(eachName);
                    }
                }
            }

            if (formerSelection != null) {
                if (componentListModel.contains(formerSelection)) {
                    componentList.setSelectedValue(formerSelection, true);
                }
            }
        }
    }

    private void reIndexComponentPossiblities() {
        SwingWorker<ArrayList<String>, Void> worker = new SwingWorker<ArrayList<String>, Void>() {
            @Override
            protected ArrayList<String> doInBackground() throws Exception {
                componentList.setEnabled(false);
                buttonRefresh.setEnabled(false);
                buttonRefresh.setSelected(true);

                HashSet<String> allMatchingComponentNamesSet = new HashSet<String>();
                for (String anElement : getClassPathSet()) {
                    try {
                        File anElementFile = new File(anElement);
                        if (anElementFile.isDirectory()) {
                            findAndAddComponentNamesInDirectory(anElementFile, allMatchingComponentNamesSet);
                        } else if (anElementFile.canRead()
                                && anElementFile.getAbsolutePath().toUpperCase().endsWith(".JAR")) {
                            // not converting to upper case so can tell if ends in ".JAR"
                            findAndAddComponentNamesInJar(anElementFile, allMatchingComponentNamesSet);
                        } else {
                            System.err.println("Could not iterate through classpath element "
                                    + Util.quotational(anElement) + "; the element is neither a "
                                    + "directory nor a jar file.");
                        }
                    } catch (IOException e) {
                        System.err.println("Exception while iterating over classpath element "
                                + Util.quotational(anElement) + ":" + e);
                    }
                }

                // remove ADEComponentImpl, since that one is abstract and so obviously won't use it:
                allMatchingComponentNamesSet.remove(ADEComponent.class.getCanonicalName());
                ArrayList<String> componentNamesSorted = new ArrayList<String>(allMatchingComponentNamesSet);
                Collections.sort(componentNamesSorted);

                return componentNamesSorted;
            }

            @Override
            protected void done() {
                try {
                    allComponentNamesSorted = get();
                    sysView.cachedClasspathComponentNames = allComponentNamesSorted;

                    componentList.setEnabled(true); // must set enabled before
                    //     calling searchFieldChanged
                    searchFieldChanged(); // update JList
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(meTheLauncher,
                            UtilUI.messageInScrollPane(Util.stackTraceString(e)),
                            "Could not index component names", JOptionPane.ERROR_MESSAGE);
                }

                // regardless of exception or not, set enabled back to true, 
                //    so can use the list (if it worked) or have the option to 
                //    attempt to re-index
                componentList.setEnabled(true);
                buttonRefresh.setEnabled(true);
                buttonRefresh.setSelected(false);
            }
        };

        worker.execute();
    }

    /**
     * returns a hashset of the class path, e.g., removing any possible
     * duplicates. order is ignored, but that's ok, because will be sorting the
     * list of names alphabetically, anyway. ADEComponentImpl, on the other hand,
     * does keep track of the order when trying, on its part, to reduce
     * classpath duplication. So really, this method is just in case, chances
     * are that the classpaths would not contain duplicates regardless.
     */
    private HashSet<String> getClassPathSet() {
        String classPath = System.getProperty("java.class.path");
        HashSet<String> classPathSet = new HashSet<String>();
        for (String eachPath : classPath.split(System.getProperty("path.separator"))) {
            classPathSet.add(eachPath);
        }
        return classPathSet;
    }

    private void findAndAddComponentNamesInDirectory(File directory,
            HashSet<String> matches) throws IOException {

        int curDirLength = directory.getCanonicalPath().length();

        Collection<File> matchingFiles = FileUtils.listFiles(directory,
                FileFilterUtils.suffixFileFilter(IMPL_DOT_CLASS_SUFFIX),
                FileFilterUtils.directoryFileFilter());

        for (File each : matchingFiles) {
            try {
                // remove the beginning path
                String implName = each.getCanonicalPath().substring(curDirLength + 1);
                //   + 1 so that skips the separator after the path.
                addToComponentMatchesIfImplStandsForATrulyValidADEComponent(implName, matches);
            } catch (IOException e) {
                // just ignore that component name
            }
        }
    }

    private HashSet<String> findAndAddComponentNamesInJar(File jar,
            HashSet<String> matches) throws IOException {

        JarFile jarFile = new JarFile(jar);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry aJarEntry = entries.nextElement();
            String aJarEntryName = aJarEntry.getName();
            if (aJarEntryName.endsWith(IMPL_DOT_CLASS_SUFFIX)) {
                addToComponentMatchesIfImplStandsForATrulyValidADEComponent(aJarEntryName, matches);
            }
        }

        return matches;
    }

    private void addToComponentMatchesIfImplStandsForATrulyValidADEComponent(
            String implName, HashSet<String> matches) {
        // replace file path separators with dots, and remove ".class";
        String dottedImplName = implName.substring(0, implName.length() - DOT_CLASS_SUFFIX_LENGTH).
                replace(File.separatorChar, '.');
        if (implNameStandsForATrulyValidADEcomponent(dottedImplName)) {
            matches.add(dottedImplName.substring(0, dottedImplName.length() - IMPL_SUFFIX_LENGTH));
        }
    }

    private boolean implNameStandsForATrulyValidADEcomponent(String dottedImplName) {
        try {
            Class<?> matchingClass = Class.forName(dottedImplName);
            if (ADE_COMPONENT_INTERFACE.isAssignableFrom(matchingClass)) {
                if (!Modifier.isAbstract(matchingClass.getModifiers())) {
                    return true;
                }
            }
        } catch (Error error) {
            // If class can't compile, it will throw an Error rather than 
            //      an exception.  Catch that, and just skip this class
            // for debugging:  System.out.println("Could not instantiate class for name " + canonicalName);
        } catch (Exception e) {
            // also skip any other exception
            // for debugging:  System.out.println("Could not instantiate class for name " + canonicalName);
        }

        // if still here:
        return false;
    }
}
