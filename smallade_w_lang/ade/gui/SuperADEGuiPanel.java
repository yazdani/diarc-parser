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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public abstract class SuperADEGuiPanel extends ADEGuiPanel {
  protected Log log = LogFactory.getLog(this.getClass());

  public SuperADEGuiPanel(ADEGuiCallHelper helper, int time) {
    super(helper, time);
  }

  /**
   * Call is a wrapper for ADEGuiPanel's call, it adds lots of error handling.
   *
   * @param <T> The return type of the method on the remote component.
   * @param methodName The name of the method to call.
   * @param type The return type of the method on the remote component.
   * @param args Any arguments you'd like to send to the remote method.
   * @return Whatever the remote method returns (or null if something has gone wrong).
   */
  protected <T> T call(String methodName, Class<T> type, Object... args) {
    try {
      if (type == void.class) {
        callComponent(methodName, args);
      } else {
        Object res = callComponent(methodName, args);
        if (type.isInstance(res)) {
          return type.cast(res);
        } else if (res == null) {
          return null;
        } else {
          log.error(String.format("Wrong type supplied to wrappedCall. %s should have been %s.", type, res.getClass()));
        }
      }
    } catch (Exception ex) {
      if (log.isDebugEnabled()) {
        log.debug(String.format("Couldn't call %s. ", methodName), ex);
      } else {
        log.error(String.format("Couldn't call %s. ", methodName), ex);
      }
    }
    return null;
  }
}
