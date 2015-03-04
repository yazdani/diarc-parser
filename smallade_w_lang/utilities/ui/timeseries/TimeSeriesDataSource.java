/*
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package utilities.ui.timeseries;

import org.jfree.data.time.TimeSeriesCollection;

/**
 * This is an interface to allow the TimeSeriesMonitor to be passed a continuous
 * stream of data.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface TimeSeriesDataSource {
  public TimeSeriesCollection getData();
}
