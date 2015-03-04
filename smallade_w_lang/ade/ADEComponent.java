/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 *
 * Copyright 1997-2013 Matthias Scheutz and the HRILab Development Team
 * All rights reserved.  For information or questions, please contact
 * the director of the HRILab, Matthias Scheutz, at mscheutz@gmail.com
 * 
 * Redistribution and use of all files of the ADE package, in source and
 * binary forms with or without modification, are permitted provided that
 * (1) they retain the above copyright notice, this list of conditions
 * and the following disclaimer, and (2) redistributions in binary form
 * reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR ANY
 * OF THE CONTRIBUTORS TO THE ADE PROJECT BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.

 * Note: This license is equivalent to the FreeBSD license.
 */
package ade;

import ade.gui.ADEGuiVisualizationSpecs;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

/**
 * The basic ADEComponent interface for remote method calls.
 *
 */
public interface ADEComponent extends Remote {

    /** Set the debugging output level for this ADEComponent. The debug level
     * is an integer that generally ranges from 0 to 10, where higher numbers
     * increase verbosity. Numbers greater than 7 are usually reserved for
     * messages placed within loops (e.g., heartbeats, reapers, etc.).
     * @param i The debug level to set
     * @param credentials Confirming information
     * @throws RemoteException If an error occurs
     * @throws AccessControlException If the user does not have adequate
     * permissions
     */
    public void setDebugLevel(int i, Object credentials)
            throws RemoteException, AccessControlException;

    /**
     * Causes the Registry to return stdout/stderr information {@link ade.ADEComponent ADEComponent}.
     *
     * @param lines Number of lines to return from the end (0 == all)
     * @return ArrayList of String with the log data
     * @exception RemoteException if the remote invocation fails
     * @exception AccessControlException if the password is bad
     */
    public ArrayList reportLogs(int lines)
            throws RemoteException, AccessControlException;

    /**
     * Used to confirm remote accessibility.
     *
     * @return always returns <tt>true</tt>
     * @throws RemoteException if the remote call fails
     */
    public boolean isUp() throws RemoteException;

    /**
     * Returns whether a component is ready and able to provide its services. This
     * method calls the {@link ade.ADEComponentImpl#localServicesReady
     * localServicesReady} method to determine its return value, which can be
     * overridden by subclass if there are criteria that must be met before it
     * can provide its services (e.g., it must have connected with all it's
     * clients, it's data structures have been fully initialized, etc.).
     *
     * @param credentials Confirming information
     * @return <tt>true</tt> if the component is ready, <tt>false</tt> otherwise;
     * unless overridden by subclass, returns <tt>true</tt>
     * @throws RemoteException If an error occurs
     * @throws AccessControlException If the user does not have adequate
     * permissions
     */
    public boolean servicesReady(Object credentials)
            throws RemoteException, AccessControlException;

    /**
     * This method causes the ADEComponent to return an object that will
     * communicate with it; i.e., an object of a type that it serves: a camera,
     * a framegrabber, an agent, etc.
     *
     * @param uid the user's ID
     * @param credentials confirming information
     * @param the constraints under which the component was requested
     * @throws RemoteException if the connection fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public void requestConnectionRegistry(String uid, Object credentials, String[][] constraints)
            throws RemoteException, AccessControlException;

    /**
     * a request on behalf of a registry for user uid to call this component's method given by methodname on the args
     *
     * @param uid ID of the client requesting the accessible methods of this
     * @param credentials the registry's ref
     * @param methodname the name of the method to be called
     * @param args the arguments for the method
     * component
     * @return the results from the method invokation
     */
    public Object requestMethodCall(String uid, Object credentials, String methodname, Object[] args) 
	throws RemoteException, AccessControlException;


    /**
     * Set the ADEComponent's host information as known to the registry.
     *
     * @param hi a copy of the ADEHostInfo known to the ADERegistry
     * @param credentials confirming information
     * @exception AccessControlException if credentials are bad
     * @exception RemoteException if another error occurs.
     */
    public void setHostInfo(ADEHostInfo hi, Object credentials)
            throws AccessControlException, RemoteException;

    /**
     * Return the host information structure of this component.
     *
     * @param credentials confirming information
     * @return an {@link ade.ADEHostInfo ADEHostInfo} object
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ADEHostInfo requestHostInfo(Object credentials)
            throws AccessControlException, RemoteException;

    /**
     * Update a client's connection, letting the component know it is still alive.
     * If the client does not update as often as the component's reaper period, the
     * component will disconnect the user.
     *
     * @param uid The user's id
     * @return the state of the component (defined in {@link
     * ade.ADEGlobals.ComponentState ComponentState})
     * @exception RemoteException if the user is not connected
     */
    public ADEGlobals.ComponentState updateConnection(String uid)
            throws RemoteException;

    /**
     * Update the component's information from an external source (generally, an {@link ade.ADERegistryImpl ADERegistryImpl}).
     * This will supplement the component's knowledge about the rest of the system.
     *
     * @param list The update information list
     * @param credentials Access control object
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the credentials are not sufficient
     */
    public void updateComponentInfo(
            LinkedList<ADEMiniComponentInfo> list,
            Object credentials) throws RemoteException, AccessControlException;

    /**
     * Return the component's "capabilities". In general, this amounts to a "device
     * list"; that is, a description of the sensors and effectors that a
     * returned agent has. However, it can be used to give a more detailed
     * description of the component's function.
     */
    /*
     * Not sure we want to use this... public String requestCapabilityString()
     * throws RemoteException;
     */
    /**
     * Requests component shutdown. Proper credentials are required to succeed.
     *
     * @param credentials confirming information
     * @return <tt>true</tt> if approved, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean requestShutdown(Object credentials)
            throws RemoteException, AccessControlException;

    /**
     * Demands (unclean) component shutdown; no cleanup, finalize, etc. actions are
     * performed, just a {@link java.lang.Runtime#halt} call. This is really
     * here only for testing (i.e., simulated faiulre). <p> Proper credentials
     * are required to succeed (otherwise, the ADE infrastructure can be
     * circumvented by obtaining a remote reference through a {@link java.rmi.Naming#lookup}
     * or
     * {@link java.rmi.registry.Registry#lookup} call). Acceptable credentials
     * consist of one of the following: <ol> <li>This component (i.e., a reference
     * to <tt>this</tt>)</li> <li>The {@link ade.ADERegistry ADERegistry} with
     * which this component is registered</li> <li>A value of <tt>true</tt>
     * returned by overriding the {@link
     * ade.ADEComponentImpl#localrequestKill} method</li> </ol>
     *
     * @param credentials Security permissions
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions */
    public void killComponent(Object credentials)
            throws RemoteException, AccessControlException;

    /**
     * Return the information structure of this component.
     *
     * @param credentials confirming information
     * @return an {@link ade.ADEComponentInfo ADEComponentInfo} object
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ADEComponentInfo requestComponentInfo(Object credentials)
            throws RemoteException, AccessControlException;

    /**
     * The registry will call this method if a new component is joing and if the
     * notification has been turned
     *
     * @param newcomponentkey the key of the newly joined component
     * @param credentials the access credentials for the method
     */
    public void notifyComponentJoinedRegistry(String newcomponentkey,Object credentials)
            throws RemoteException;

    /**
     * Return an {@link java.util.ArrayList ArrayList} of methods the user is
     * allowed to call. Unless otherwise specified, the default behavior is to
     * allow access to <b>all</b> the remote methods defined in all implemented
     * interfaces <b>except</b>: <ul> <li>{@link ade.ADERegistry ADERegistry}:
     * only allows access to the
     * {@link ade.ADERegistry#updateStatus updateStatus} method</li> <li>{@link ade.ADEComponent ADEComponent}:
     * only allows access to the
     * {@link ade.ADEComponent#updateConnection updateConnection}, {@link
     * ade.ADEComponent#requestMethods requestMethods}, {@link
     * ade.ADEComponent#requestHostInfo requestHostInfo}, and {@link
     * ade.ADEComponent#requestComponentInfo requestComponentInfo} methods</li> </ul>
     * This is enforced by the {@link ade.ADEComponentImpl#call call} method, which
     * use an {@link ade.ADERemoteCallTimer ADERemoteCallTimer} within a
     * <tt>pseudo-reference</tt> to perform the actual method calls.
     *
     * @param uid the user's ID
     * @param credentials confirmation information
     * @param componenttype the type of component (i.e., interface) for which the
     * methods are requested
     * @return the permitted method names
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions */
    public HashMap<String,ADEMethodConditions> requestMethods(String uid, Object credentials, String componenttype)
            throws ADERequestMethodsException, RemoteException, AccessControlException;

    /**
     * request to be notified by the component based on the arguments passed in noteobject
     * @param uid the requesting component's ID
     * @param credentials the requesting component's credentials
     * @param requester the remote reference to the requester
     * @param noteobject the Notification data structure
     * @throws AccessControlException
     * @throws RemoteException 
     */
    public void requestNotification(String uid, Object credentials, ADEComponent requester, ADENotification note)
	throws AccessControlException, RemoteException;

    /** removes a notification if it exists */
    public void cancelNotification(final String uid, Object credentials, final ADEComponent requester, final ADENotification note)
    	throws AccessControlException, RemoteException;

    /**
     * Returns the ID of the component
     */
    public String getID() throws RemoteException;

    /**
     * gets the name of the component
     */
    public String getName() throws RemoteException;

    /**
     * Starts or stops the ADE-wide logging mechanism (description to be completed...)
     */
    // MS: added to enable or disable ADEComponent logging
    public void setLocalLogging(Object credentials, boolean state)
            throws RemoteException, AccessControlException;

    /**
     * Gets the list of logged ADE calls since last invocation.
     *
     * @return a Vector with the calls that were logged
     */
    public ArrayList<String> getLoggedCalls() throws RemoteException;

    /**
     * Returns a data structure that hold GUI visualization information for the
     * component
     */
    public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException;

    /**
     * Suspends the main loop of the component. Note that this will only cause the
     * main loop to be suspended; any other processes need to be manually
     * suspended (e.g., based on monitoring the suspended status); only
     * registries can call this method for now
     */
    public void suspendComponent(Object credentials) throws RemoteException, AccessControlException;

    /**
     * Resumes a suspended component. Note that this will only cause the main loop
     * to resume; any other processes need to be manually resumed (e.g., based
     * on monitoring the suspended status); only registries can call this method
     * for now
     */
    public void resumeComponent(Object credentials) throws RemoteException, AccessControlException;

    /**
     * Puts the component into live mode (which calls updateComponent) versus log mode
     * (which calls updateLog) in the main loop; note that this will only cause
     * the main loop to switch between modes, all other processes need to be
     * manually adjusted (e.g., based on monitoring the liveMode status); only
     * registries, the component itself, and the log-playback toolbar
     * (ADELogPlaybackVis) can call this method for now
     */
    public void setUpdateModeLive(Object credentials, boolean mode)
            throws RemoteException, AccessControlException;

    /**
     * Allows/disallows dynamic adjustments of looptime when the component fails to
     * update within one cycle (only preliminary implementation so far)
     */
    public void setDynamicLoopTime(Object credentials, boolean on)
            throws RemoteException, AccessControlException;

    /**
     * Sets the looptime of the component's main loop; the component will attempt to
     * stay within the loop time for each update cycle (by sleeping the
     * remaining time when the cycle finishes early); it will dynam
     */
    public void setUpdateLoopTime(Object credentials, long looptime)
            throws RemoteException, AccessControlException;

    /**
     * Indicates that the component is processing live (instead of playing back
     * from a log)
     */
    public boolean isLive() throws RemoteException;

    public boolean hasPlaybackLogFile() throws RemoteException;

    public String getPlaybackLogFileName() throws RemoteException;

    /**
     * open playback log file based on existing file
     */
    public void openPlaybackLogFile(Object credentials, String logFileName) throws IOException,
            AccessControlException, RemoteException;

    /**
     * returns the playback position in the current log file
     */
    public int getPlaybackPosition() throws RemoteException;

    /**
     * returns the maximum playback position in the current log file
     */
    public int maxPlaybackPosition() throws RemoteException;
    
    /**
     * sets the playback position in the current log file
     */
    public void setPlaybackPosition(Object credentials, int jumpToInt) throws RemoteException;
    
    /**
     * turns playback on or off
     */
    public void setPlaybackRunning(Object credentials, boolean run) throws RemoteException;

    /**
     * returns the playback status
     */
    public boolean getPlaybackRunning() throws RemoteException;

    /**
     * registers a SystemView GUI visualization as a consumer of a component's
     * output. Note that, upon no longer being interested in component output, the
     * GUI must de-register with the component -- otherwise, the component will
     * continue to accumulate an output buffer for it.
     *
     * @param credentials: the Registry to which the SystemView GUI belongs.
     * @return a unique ID that identifies this particular consumer (that way,
     * can have multiple consumers, and know exactly how much information is yet
     * to be updated for the consumer)
     */
    public UUID registerOutputRedirectionConsumer(Object credentials)
            throws RemoteException, IOException, AccessControlException;

    /**
     * de-registers a SystemView GUI visualization as a consumer of a component's
     * output.
     *
     * @param consumerID: the UUID that the consumer was assigned by a former
     * call to registerOutputRedirectionConsumer()
     */
    public void deregisterOutputRedirectionConsumer(UUID consumerID) throws RemoteException;

    /**
     * returns any output that has been accumulated for a particular consumer
     * since its last call to this method.
     *
     * @param consumerID: the UUID that the consumer was assigned by a former
     * call to registerOutputRedirectionConsumer()
     */
    public String getAccumulatedRedirectedOutput(UUID consumerID) throws RemoteException;
}
