package ade;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Connection is a convenience wrapper around a heartbeat object to make dealing
 * with other components more friendly.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class Connection {
  SuperADEComponentImpl impl;
  String type;
  String name;
  public boolean required = true;
  Object ref;
  ArrayList<UUID> setupHistory = new ArrayList<UUID>();
  InitialSetupCallback  setup;
  Thread pausingThread = null;

  Connection(SuperADEComponentImpl componentImpl, String type, InitialSetupCallback initialSetup) {
    this.impl = componentImpl;
    this.type = type;
    this.name = null;
    this.setup = initialSetup;
    constructorCommon();
  }

  Connection(SuperADEComponentImpl componentImpl, String type, String name, InitialSetupCallback initialSetup) {
    this.impl = componentImpl;
    this.type = type;
    this.name = name;
    this.setup = initialSetup;
    constructorCommon();
  }

  private void constructorCommon() {
    List<String> currentComponents = impl.getComponentList(new String[][]{{"type", type}});
    if (currentComponents != null && currentComponents.size() > 0) {
      connect();
    }
  }

  final void connect() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        if (name == null) {
          impl.getClient(type, 0);
        } else {
          impl.getClient(type, name, 0);
        }
      }
    }, String.format("Connecting to %s worker.", type)).start();
  }

  /**
   * @return True iff the connection is up and ready for calls.
   */
  public boolean isReady() {
    return ref != null && (pausingThread == null || pausingThread == Thread.currentThread());
  }

  /**
   * Call is a wrapper for ADEComponentImpl's call, it adds two things, 1) a more
   * OO interaction style and 2) Lots of error handling.
   *
   * @param <T> The return type of the method on the remote component.
   * @param methodName The name of the method to call.
   * @param type The return type of the method on the remote component.
   * @param args Any arguments you'd like to send to the remote method.
   * @return Whatever the remote method returns (or null if something has gone wrong).
   */
  public <T> T call(String methodName, Class<T> type, Object... args) {
    if (!isReady()) {
      impl.log.error("Cannot call component while not connected to it!");
      return null;
    }
    try {
      if (type == void.class) {
        impl.call(ref, methodName, args);
      } else {
        Object res = impl.call(ref, methodName, args);
        if (type.isInstance(res)) {
          return type.cast(res);
        } else if (res == null) {
          return null;
        } else {
          impl.log.error(String.format("Wrong type supplied to wrappedCall. %s should have been %s.", type, res.getClass()));
        }
      }
    } catch (Exception ex) {
      if (impl.log.isDebugEnabled()) {
        impl.log.debug(String.format("Couldn't call %s on %s.  Perhaps the arguments were wrong?", methodName, impl.getRefID(ref)), ex);
      } else {
        impl.log.error(String.format("Couldn't call %s on %s.  Perhaps the arguments were wrong?", methodName, impl.getRefID(ref)));
      }
    }
    return null;
  }
}