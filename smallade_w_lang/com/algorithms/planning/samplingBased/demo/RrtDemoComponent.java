/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 * @file com/algorithms/planning/rrt/demo/RrtDemoComponent.java
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package com.algorithms.planning.samplingBased.demo;

import ade.ADEComponent;
import java.rmi.RemoteException;

/**
 * 
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface RrtDemoComponent extends ADEComponent, RrtVisualizationDataProvider {}