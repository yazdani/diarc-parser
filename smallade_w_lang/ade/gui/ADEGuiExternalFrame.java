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
import ade.gui.ADEGuiPanel.VisibilityDeterminer;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JFrame;
import utilities.Util;

public class ADEGuiExternalFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private ADEGuiExternalFrame meTheFrame = this;
    private boolean myFirstSetVisibleCall = true; // assume that still hasn't
    //      called it; ie: will be first call
    private ADEGuiPanel myGUIPanel;

    /**
     * Static method that INITIATES the creation of external GUI frames based on
     * an ADEComponent and passed-in String parameters (set) for which particular
     * visualizations (if any explicitly specified) to create on startup.
     *
     * @param persistentGUI
     */
    public static void createExternalGUIFrames(
            final ADEComponent component,
            final boolean guiRequested,
            final HashSet<String> guiRequestedVisualizations,
            final boolean persistentGUI) {

        // When the GUIs are created, they'll often request stuff immediately.  If the creation
        //    happens to be before the component has finished initializing, it will throw some
        //    errors that would alarm the user.  As such, the REQUEST to create gui windows is
        //    created here, but the execution of it will only happen after a short while.

        Thread guiCreator = new Thread() {
            @Override
            public void run() {
                // the idea is that the gui should wait a few seconds before even attempting to start, 
                //     and then ONLY start if component is ready.  If component not ready, sleep a few more
                //     seconds and try again...
                boolean ready = false;
                while (!ready) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e1) {
                        // can't sleep, what can you do...
                    }
                    try {
                        ready = component.servicesReady(component);
                    } catch (Exception e) {
                        System.err.println("Visualization creator could not determine component ready status, "
                                + "assuming false and continuing to wait...");
                    }
                }


                try {
                    ADEGuiVisualizationSpecs possibleVisualizationSpecs = component.getVisualizationSpecs();

                    // if no visualizations to possibly show, inform user if gui was requested,
                    //     and exit regardless
                    if ((possibleVisualizationSpecs == null || possibleVisualizationSpecs.size() == 0)) {
                        if (guiRequested) {
                            System.err.println("You have requested exteranl GUI visualizations to "
                                    + "appear, but no visualizations are available for this component."
                                    + "\nOperation will resume as normal, but without the GUI.");
                        }
                        return; // done, no GUIs to launch, so quit.
                    }


                    // if haven't quit, there must be GUIs that ARE possible visualizations for the component.

                    // first see if the user wanted help choosing GUIs (e.g., passing "--gui -?")
                    if (guiRequested) {
                        if (guiRequestedVisualizations.contains("-?")) {
                            // show a window for choosing GUIs, and have IT manipulate the action from here on out...
                            new ADEGuiListBox(component, guiRequestedVisualizations,
                                    possibleVisualizationSpecs, persistentGUI).setVisible(true);

                            return; // again, since the ADEGuiListBox will take care of things from
                            //    here, just quit this particular thread.
                        }
                    }


                    // if haven't quit here either, then the user did not want to choose for his/herself
                    //     what visualizations to show, so should determine them automatically and 
                    //     then show them.

                    HashSet<ADEGuiVisualizationSpecs.Item> guisToCreate =
                            new HashSet<ADEGuiVisualizationSpecs.Item>();

                    // regardless of --GUI flag, add any GUIs that are defined to always show:
                    for (ADEGuiVisualizationSpecs.Item eachVisItem : possibleVisualizationSpecs.values()) {
                        if (eachVisItem.startupOption == ADEGuiVisualizationSpecs.StartupOption.ALWAYS_SHOW) {
                            guisToCreate.add(eachVisItem);
                        }
                    }

                    // if user passed --GUI flag AND NO explicit requests, add any GUIs that are "default",
                    //     e.g., SHOW_ON_GUI_FLAG.
                    if (guiRequested && (guiRequestedVisualizations.size() == 0)) {
                        for (ADEGuiVisualizationSpecs.Item eachVisItem : possibleVisualizationSpecs.values()) {
                            if (eachVisItem.startupOption == ADEGuiVisualizationSpecs.StartupOption.SHOW_ON_GUI_FLAG) {
                                guisToCreate.add(eachVisItem);
                            }
                        }
                    }

                    // finally, add any specifically-requested GUIs
                    for (String eachVis : guiRequestedVisualizations) {
                        ADEGuiVisualizationSpecs.Item matchingVisualizationIfAny =
                                Util.getIgnoreCase(possibleVisualizationSpecs.entrySet(), eachVis);
                        if (matchingVisualizationIfAny == null) {
                            System.err.println("Unavailable visulization \"" + eachVis + "\"");
                        } else {
                            guisToCreate.add(matchingVisualizationIfAny);
                        }
                    }

                    createExternalGUIFrames(component, guisToCreate, persistentGUI);


                } catch (Exception ex) {
                    System.err.println("Error(s) trying to instantiate external GUI frames "
                            + "for the component.\n" + ex);
                    ex.printStackTrace();
                    System.out.println("\nOperation will resume as normal, but without the GUI.");
                }
            }
        };

        guiCreator.start();
    }

    /**
     * Static method that proceeds with the creation of external GUI frames,
     * once it knows which particular GUIs to create.
     */
    protected static void createExternalGUIFrames(final ADEComponent component,
            final HashSet<ADEGuiVisualizationSpecs.Item> guisToCreate,
            boolean persistentGUI) {

        ArrayList<ADEGuiExternalFrame> externalGUIFrames = new ArrayList<ADEGuiExternalFrame>();

        for (ADEGuiVisualizationSpecs.Item eachGuiToCreate : guisToCreate) {
            ADEGuiPanel newPanel = ADEGuiCreatorUtil.createGuiPanel(
                    ADEGuiCreatorUtil.createCallHelper(component), eachGuiToCreate);
            if (newPanel != null) {
                ADEGuiExternalFrame newFrame = new ADEGuiExternalFrame(newPanel);
                externalGUIFrames.add(newFrame);
            }
        }


        // having created the frames, perform some common operations on each:
        //    namely, add window listener for when frame is closed, and set visible
        if (externalGUIFrames.size() > 0) {
            // compensate for any insets imposed by operating system (start menu,
            //      OS X menu bar and dock, etc)
            Toolkit kit = Toolkit.getDefaultToolkit();
            GraphicsConfiguration config = externalGUIFrames.get(0).getGraphicsConfiguration();
            Insets insets = kit.getScreenInsets(config);
            int frameX = insets.left;
            int frameY = insets.top;
            int offsetFramesFromEachOther = 25;

            for (final ADEGuiExternalFrame eachFrame : externalGUIFrames) {
                if (persistentGUI) {
                    eachFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                } else {
                    eachFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                }


                eachFrame.setVisible(true);
                eachFrame.setLocation(frameX, frameY);

                frameX = frameX + offsetFramesFromEachOther;
                frameY = frameY + offsetFramesFromEachOther;
            }
        }
    }

    public ADEGuiExternalFrame(final ADEGuiPanel guiPanel) {
        this.myGUIPanel = guiPanel;
        this.setContentPane(myGUIPanel);

        myGUIPanel.setVisibilityDeterminer(new VisibilityDeterminer() {
            @Override
            public boolean isVisible() {
                return (meTheFrame.isVisible());
            }
        });
    }

    public Dimension getSizeWithInsets(Dimension innerSize) {
        return new Dimension(innerSize.width + this.getInsets().left + this.getInsets().right,
                innerSize.height + this.getInsets().top + this.getInsets().bottom);
    }

    @Override
    public Dimension getPreferredSize() {
        return getSizeWithInsets(this.getContentPane().getPreferredSize());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        // check if this was the first call to show the frame:
        if (visible && myFirstSetVisibleCall) {
            this.setSize(getSizeWithInsets(myGUIPanel.getInitSize(false)));
            //     false = is NOT internal frame.
            //     know this because I *AM* the ADEGui EXTERNAL Frame class!

            this.setTitle(myGUIPanel.getInitTitle());
            myFirstSetVisibleCall = false;

            // notify GUI that it is loaded -- i.e., so it knows 
            //       to update itself and start the update timer.
            myGUIPanel.isLoaded(false); // false = is NOT internal frame.
            //     know this because I *AM* the ADEGui EXTERNAL Frame class!
        }
    }
}
