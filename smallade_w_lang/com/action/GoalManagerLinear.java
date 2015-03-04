/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * GoalManagerLinear.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */
package com.action;

/** 
 * <code>GoalManagerLinear</code> performs no reprioritization; new goals
 * are pushed onto a stack and action selection is linear, stepping through the
 * events in the provided script until complete.
 */
public interface GoalManagerLinear extends GoalManager {
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
