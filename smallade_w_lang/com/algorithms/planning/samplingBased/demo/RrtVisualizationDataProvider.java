/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 * @file com/algorithms/planning/rrt/demo/RrtVisualizationDataProvider.java
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package com.algorithms.planning.samplingBased.demo;

import com.algorithms.planning.Path;
import com.algorithms.planning.StateContainer;
import java.rmi.RemoteException;
import scala.Option;

/**
 * Your component must extend this interface if you want to use the RrtDemoComponentGui
 * as a visualization.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface RrtVisualizationDataProvider {
  public StateContainer getData() throws RemoteException;
  public Option<Path> getSolution() throws RemoteException;
  public RrtVisualizationParams getVisualizationParams() throws RemoteException;
}