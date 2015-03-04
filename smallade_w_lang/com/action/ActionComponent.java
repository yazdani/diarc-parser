/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * ActionComponent.java
 *
 * Last update: April 2010
 *
 * @author Paul Schermerhorn
 *
 */
package com.action;

import ade.*;

/** <code>ActionComponent</code> periodically calls the method in the derived
 * ActionComponentArch class.  This is just a greatly simplified version of the
 * GoalManager, providing the user with the infrastructure (e.g., remote
 * reference setup) of the GoalManager but without starting any 
 * ActionInterpreter.  Instead, the user specifies the actions in the
 * runArchitecture() method.  Some actions are defined here (i.e., methods
 * wrapping remote calls to VelocityComponent and LRFComponent); see the default
 * architecture in DefaultActionComponentArch.java for examples of other remote
 * calls.
 */
public interface ActionComponent extends ADEComponent {
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
