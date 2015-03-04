/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package ade;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * SuperADEComponentImpl is a convenience wrapper around ADEComponentImpl to take
 * care of some tasks that tend to be common across components, namely:
 * 
 * * Managing connections to other components.
 * * Initializing logging.
 * * Wrapping call() to take care of type casting.
 * * Boilerplate.
 *
 * To use it simply extend it in your component impl and override whatever methods
 * you need.  In order to ensure that the necessary hooks (for connection management)
 * are put in place some functionality had to be renamed, but beyond a few cosmetic
 * renamings everything that worked when inheriting from ADEComponentImpl should
 * still work.
 *
 * The new functionality you should be aware of is:
 *
 * * init - Guaranteed to run before any other code in your class (even the constructor), good for providing default values.
 * * connectToComponent - a simple, fast, reliable way to establish a connection to another component, takes all the thinking out
 * * requiredConnectionsPresent - returns true iff all of your required components are online and connected
 * * readyUpdate - just like updateComponent except that it is only called when your component is ready.  NOTE: you can use either readyUpdate or updateComponent, but not both.  If you need both use updateComponent and wrap part of the code in an if (localServicesReady())
 * * call - Connection objects (returned from connectToComponent) have a call method, make sure to pass it the correct return type.
 * * isReady - Connection objects can be queried to see if they are ready.  If they're required and you're accessing them from code called by readyUpdate you need not worry about this, in this case it's implied that they're ready.
 * 
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public abstract class SuperADEComponentImpl extends ADEComponentImpl implements SuperADEComponent {
  private boolean initialized;
  protected final UUID instanceId = UUID.randomUUID();
  protected Log log;
  String[][] componentConstraints;
  ConcurrentLinkedQueue<Connection> trackedConnections;

  protected abstract void init();  // this will be called FIRST (well, technically second, statics variables are initialized before this is called)
  protected boolean parseArgs(String[] args) {return true;}  // renaming of parseadditionalargs
  protected void readyUpdate() {}  // like updateComponent, but only called when localServicesReady
  protected void alertComponentJoined(String newComponentKey) {}  // renaming of notifyComponentJoined
  protected void alertComponentConnected(String componentKey, Object ref, String[][] constraints) {}  // renaming of componentConnectReact
  protected void alertComponentDisconnected(String componentKey, String[][] constraints) {}  // renaming of componentDownReact

  public SuperADEComponentImpl() throws RemoteException {
    super();
    initWrapper();
  }

  private synchronized void initWrapper() {
    if (initialized) {
      return;
    }
    initialized = true;
    log = LogFactory.getLog(this.getClass());
    componentConstraints = new String[0][];
    trackedConnections = new ConcurrentLinkedQueue<Connection>();
    init();
    log.debug("Done initializing.");
  }

  /**
   * Are you connected to every component that you've requested (and not explicitly marked as not required).
   * @return True iff all requested required connections are up.
   */
  protected final boolean requiredConnectionsPresent() {
    for (Connection conn : trackedConnections) {
      if (conn.required && !conn.isReady()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Create a Connection object that handles the hard work of becoming and staying
   * connected to another component.
   *
   * To create a Connection that is not required, simply create one and set its
   * required property to false.
   * @param type The (qualified) type of component to which you'd like to connect.
   * @return A connection object for you to interact with.
   */
  final protected synchronized Connection connectToComponent(String type) {
    log.debug("Registering interest in a " + type);
    addNotification(new String[]{"type", type});
    Connection conn = new Connection(this, type, null);
    trackedConnections.add(conn);
    return conn;
  }

  /**
   * Create a Connection object that handles the hard work of becoming and staying
   * connected to another component.
   *
   * To create a Connection that is not required, simply create one and set its
   * required property to false.
   * @param type The (qualified) type of component to which you'd like to connect.
   * @param name The name of the component to which you'd like to connect.
   * @return A connection object for you to interact with.
   */
  final protected synchronized Connection connectToComponent(String type, String name) {
    log.debug("Registering interest in a " + type + " called " + name);
    addNotification(new String[]{"name", name});
    Connection conn = new Connection(this, type, name, null);
    trackedConnections.add(conn);
    return conn;
  }

  /**
   * Create a Connection object that handles the hard work of becoming and staying
   * connected to another component.
   *
   * To create a Connection that is not required, simply create one and set its
   * required property to false.
   * @param type The (qualified) type of component to which you'd like to connect.
   * @param setup A callback to be called first for each connection to a distince component instance.
   * @return A connection object for you to interact with.
   */
  final protected synchronized Connection connectToComponent(String type, InitialSetupCallback setup) {
    log.debug("Registering interest in a " + type);
    addNotification(new String[]{"type", type});
    Connection conn = new Connection(this, type, setup);
    trackedConnections.add(conn);
    return conn;
  }

  /**
   * Create a Connection object that handles the hard work of becoming and staying
   * connected to another component.
   *
   * To create a Connection that is not required, simply create one and set its
   * required property to false.
   * @param type The (qualified) type of component to which you'd like to connect.
   * @param name The name of the component to which you'd like to connect.
   * @param setup A callback to be called first for each connection to a distince component instance.
   * @return A connection object for you to interact with.
   */
  final protected synchronized Connection connectToComponent(String type, String name, InitialSetupCallback setup) {
    log.debug("Registering interest in a " + type + " called " + name);
    addNotification(new String[]{"name", name});
    Connection conn = new Connection(this, type, name, setup);
    trackedConnections.add(conn);
    return conn;
  }

  /**
   * Registers for notifications relating to component events that match the provided constraint.
   *
   * You don't really have to worry about ever calling this method unless you're
   * overriding alertComponentConnected or alertComponentDisconnected to use notifications
   * for something unusual.
   * @param  constraint The constraint for the registry to use as a filter of when to noify you.
   */
  final protected synchronized void addNotification(String[] constraint) {
    String[][] constraints = new String[componentConstraints.length+1][0];
    System.arraycopy(componentConstraints, 0, constraints, 0, componentConstraints.length);
    constraints[componentConstraints.length] = constraint;
    componentConstraints = constraints;
    registerNewComponentNotification(componentConstraints, true);
  }

  @Override
  protected void clientConnectReact(String user) {
    log.debug("Got connection from " + user);
  }

  @Override
  protected boolean clientDownReact(String user) {
    log.debug("Lost connection from " + user);
    return false;
  }

  @Override
  final public void notifyComponentJoined(final String newComponentKey) {
    log.trace(newComponentKey + " joined");
    String keyType = getTypeFromID(newComponentKey);
    for (Connection conn : trackedConnections) {
      if (conn.ref == null && keyIsOfType(keyType, conn.type)) {
        log.debug("Attempting to get connection to " + newComponentKey);
        conn.connect();
        break;
      }
    }
    alertComponentJoined(newComponentKey);
  }

  @Override
  final protected void componentDownReact(String componentkey, String[][] constraints) {
    log.debug("Lost connection to " + componentkey);
    for (Connection conn : trackedConnections) {
      if (componentkey.equals(getRefID(conn.ref))) {
        conn.ref = null;
      }
    }
    alertComponentDisconnected(componentkey, constraints);
  }

  @Override
  final protected void componentConnectReact(final String componentkey, final Object ref, final String[][] constraints) {
    log.debug("Established a connection to " + componentkey);
    String keyType = getTypeFromID(componentkey);
    final SuperADEComponentImpl thisSuperADEComponentImpl = this;
    for (final Connection conn : trackedConnections) {
      if (conn.ref == null && keyIsOfType(keyType, conn.type)) {
        // if we're going to assign the ref to this conn, spin up a new thread
        // in case the initial setup takes a long time
        new Thread(componentkey + " initial setup.") {
          @Override
          public void run() {
            try {
              conn.pausingThread = Thread.currentThread();
              conn.ref = ref;
              // if there's 1 time setup to do
              if (conn.setup != null) {
                UUID cid = UUID.fromString(call(ref, "getComponentInstanceId").toString());
                // if its our first time connecting
                if (!conn.setupHistory.contains(cid)) {
                  conn.setupHistory.add(cid);
                  conn.setup.onFirstConnectionTo(conn);
                }
              }
              
              thisSuperADEComponentImpl.alertComponentConnected(componentkey, ref, constraints);
            } catch (Exception e) {
              log.error("Error calling getComponentInstanceId, is the other component a SuperADEComponent? (it needs to be)", e);
            } finally {
              conn.pausingThread = null;
            }
          }
        }.start();
      }
    }
  }

  @Override
  protected boolean localrequestShutdown(Object credentials) {
    log.debug("localrequestShutdown ...?");
    return false;
  }

  @Override
  protected void localshutdown() {
    log.debug("locally shutting down");
  }

  @Override
  protected void updateComponent() {
    if (localServicesReady()) {
      readyUpdate();
    } else {
      log.trace("Not ready to go, skipping readyUpdate, missing components are: ");
      for (Connection c : trackedConnections) {
        if (c.required && !c.isReady()) {
          log.trace(c.type);
        }
      }
    }
  }

  @Override
  protected void updateFromLog(String logEntry) {
  }

  @Override
  protected boolean localServicesReady() {
    return requiredConnectionsPresent();
  }

  @Override
  final protected boolean parseadditionalargs(String[] args) {
    initWrapper();
    return parseArgs(args);
  }

  private boolean keyIsOfType(String componentKeyType, String type) {
    if (componentKeyType.equals(type)) {
      return true;
    }
    try {
      Class<?> comp = Class.forName(componentKeyType);
      Class<?> t = Class.forName(type);
      return t.isAssignableFrom(comp);
    } catch (ClassNotFoundException cnf) {
      log.debug("Couldn't find class??  Oh well, that just means not equal.", cnf);
      return false;
    }
  }

  @Override
  public UUID getComponentInstanceId() {
    return instanceId;
  }
}