/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 * 
 * HostMonitorComponent.java
 */
package com.hostmonitor;

import ade.*;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.util.*;

/**
Defines the interface for the {@link com.hostmonitor.HostMonitorComponentImpl
HostMonitorComponentImpl} class for Java 1.5.

@author Jim Kramer
 */
public interface HostMonitorComponent extends ADEComponent {

    // HostMonitor
    /** Returns the statistics of the hosts being monitored.
     * @return An {@link java.util.ArrayList ArrayList} of {@link
     * ade.ADEMiniHostStats ADEMiniHostStats}
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public ArrayList<ADEMiniHostStats> getHostStats(Object credentials)
            throws RemoteException, AccessControlException;

    /** Adds a host to the list.
     * @param host an {@link ade.ADEHostInfo ADEHostInfo} structure
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @return <tt>true</tt> if the host was not already in the list and
     * was successfully added, <tt>false</tt> otherwise
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public boolean addHost(ADEHostInfo host, Object credentials)
            throws RemoteException, AccessControlException;

    /** Removes a host from the list.
     * @param host the name or IP address of a host
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @return <tt>true</tt> if the host existed and was removed,
     * <tt>false</tt> otherwise
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public boolean removeHost(String host, Object credentials)
            throws RemoteException, AccessControlException;

    // HostMonitorSensorComponent
    /** Parses a file containing host information and adds the hosts
     * to the host list.
     * @param file the name of a properly formatted hosts configuration file
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @return the number of hosts loaded
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public int loadHostFile(String file, Object credentials)
            throws RemoteException, AccessControlException;

    /** Returns the list of hosts being monitored.
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @return An array of host addresses
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public ArrayList<String> getHostList(Object credentials)
            throws RemoteException, AccessControlException;

    /** Starts the update for all hosts.
     * @param credentials determines whether the caller is allowed
     * to access the host monitor
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void startHostChecking(Object credentials)
            throws RemoteException, AccessControlException;

    /** Starts the update for a host.
     * @param host the name or IP address of a host
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void startHostChecking(String host, Object credentials)
            throws RemoteException, AccessControlException;

    /** Stops the update for all hosts.
     * @param credentials determines whether the caller is allowed
     * to access the host monitor
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void stopHostChecking(Object credentials)
            throws RemoteException, AccessControlException;

    /** Stops the update for a host.
     * @param host the name or IP address of a host
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void stopHostChecking(String host, Object credentials)
            throws RemoteException, AccessControlException;

    /** Changes the update rate (aka, the period) of host checking.
     * @param host the name or IP address of a host
     * @param p The new update rate
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void setHostCheckingPeriod(String host, long p, Object credentials)
            throws RemoteException, AccessControlException;
}
