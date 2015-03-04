/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 * @file com/82/InitialSetupCallback.java
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package ade;

/**
 * This is an interface for setup code that needs to happen exactly
 * once per component instance.
 *
 * For example, if there was a component A that provided callbacks under
 * some circumstances you would want to first register your component B
 * to receive callbacks from A, but if the connection between
 * the two stuttered you wouldn't want to register a second time once B
 * reconnected with A.  If, on the other hand, A crashes and is restarted
 * you would want to reregister for callbacks.
 *
 * Providing an object of this type to the connectToComponent method
 * solves that problem for you automatically.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface InitialSetupCallback {
  public void onFirstConnectionTo(Connection conn);
}