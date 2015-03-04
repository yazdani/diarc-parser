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

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiVisualizationSpecs;
import ade.gui.InfoRequestSpecs;
import ade.gui.SystemViewAccess;
import ade.gui.SystemViewComponentInfo;
import ade.gui.SystemViewStatusData;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * Interface that all ADERegistrys must implement; includes methods to register
 * components, service connection requests, and administration tasks.
 */
public interface ADERegistry extends ADEComponent {

    /**
     * Provides the means for a client to check what components of a particular
     * type are registered:
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param constraints the type of to request an Agent from
     * @return an {@link java.util.ArrayList ArrayList} of ADEComponent names that
     * meet the constraints
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad
     */
    public ArrayList<String> requestComponentList(
            String uid, // the username
            String upw, // the password
            String[][] constraints // the constraints to be met by the component
            ) throws AccessControlException, RemoteException;

    /**
     * Provides the means for a client to check what components of a particular
     * type are registered with its registry (note that user access is not checked here)
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param constraints the type of to request an Agent from
     * @return an {@link java.util.ArrayList ArrayList} of ADEComponent names that
     * meet the constraints
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad
     */
    public ArrayList<String> requestLocalComponentList(
            String uid, // The username
            String upw, // The password
            String[][] constraints // The constraints to be met by the component
            ) throws AccessControlException, RemoteException;



    /*
     public ArrayList<String> requestMethods(String fromuid, String touid, Object credentials)
     throws RemoteException, AccessControlException {
     */
    /**
     * checks if the given component key in use by this registry
     */
    // TODO: for now, this can only be used by registries, but should probably also be allowed for registered users...
    public boolean isUsed(String key, Object credentials) throws RemoteException;

    /**
     * Provides the means by which a connection to a component is granted.
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param requester the component making the request
     * @param constraints the constraints for the requested component
     * @return an {@link ade.ADEComponent ADEComponent} reference
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad or the component is
     * not accepting any more connections
     */
    public ADEComponentInfo requestConnection(
            String uid, // the username
            String upw, // the password
            ADEComponent requester, // the requesting component
            String[][] constraints // the constraints to be met by the component
            ) throws AccessControlException, RemoteException;

    /**
     * Provides the means for a client to receive all {@link ade.ADEComponent
     * ADEComponent}s of a particular type excluding itself.
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param requester the component making the request
     * @param constraints the constraints for the requested component
     * @return an {@link java.util.ArrayList ArrayList} of ADEComponent references
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ArrayList<ADEComponentInfo> requestConnections(
            String uid, // the username
            String upw, // the password
            ADEComponent requester, // the requesting component
            String[][] constraints // the constraints to be met by the component
            ) throws AccessControlException, RemoteException;

    /**
     * Provides the means for a client to receive all {@link ade.ADEComponent
     * ADEComponent}s of a particular type (whenever they register)
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param reqType the type of {@link ade.ADEComponent ADEComponent}
     * @return an {@link java.util.ArrayList ArrayList} of ADEComponent references
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public void requestNewComponentNotification(
            String uid, // the username
            String upw, // the password
            ADEComponent requester, // the component making the request
            String[][] constraints, // the constraints to be met by the component
            boolean on // whether to turn notification on or off
            ) throws AccessControlException, RemoteException;

    /**
     * generates a list of ADE component infos of all components meeting the criteria to which the given user has access
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param constraints to be met by the component
     * @param onbehalf the {@link ade.ADERegistry ADERegistry} making the
     *        request on behalf of a registry (null if it is the current one)
     * @param  checkavailableforuser // whether the user is allowed and could get a connection
     * @return an {@link java.util.ArrayList ArrayList} of ADE component infos of components that 
     *         meet the constraints and are accessible to the user
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad
     */
    public ArrayList<ADEComponentInfo> getAllApplicableComponents(
	    String uid, // the username of the requesting component
            String upw, // the password
            String[][] constraints, // the constraints to be met by the remote component
	    ADERegistry onbehalf, // the requesting registry
	    ADEUser u, // if applicable the user info
	    boolean checkavailableforuser, // whether the user is allowed and could get a connection
	    boolean forward // whether the request should be forwarded 
            ) throws AccessControlException, RemoteException;

    /**
     * Provides the means for a client to directly make a call in a remote component
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param requester the {@link ade.ADEComponent ADEComponent} making the request
     * @param constraints the constraints to be met by the component
     * @return an eligible ADEComponent
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate permissions
     */
    public Object callMethodInRemoteComponent(
            String uid,
            String upw,
            ADEComponent requester,
            String[][] constraints,
	    String methodname,
	    Object[] args
            ) throws AccessControlException, RemoteException;


    /**
     * Will return the "lines" from the log files.
     *
     * @param aid The administrator's username
     * @param apw The administrator's password
     * @param sType The type of the ADEComponent
     * @param sName The name of the ADEComponent
     * @param lines Number of lines to return from the end (0 == all)
     * @return ArrayList of String with log data
     * @exception RemoteException if the remote invocation fails
     * @exception AccessControlException if the password is bad
     */
    public ArrayList reportLogs(
            String aid,
            String apw,
            String sType,
            String sName,
            int lines) throws RemoteException, AccessControlException;

    /**
     * Provides the means for an {@link ade.ADEComponent ADEComponent} to obtain the
     * execution status of another <tt>ADEComponent</tt> from this {@link
     * ade.ADERegistry ADERegistry}.
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param reqType the type of component
     * @param reqName the name of a particular component
     * @return the {@link ade.ADEGlobals.RecoveryState RecoveryState}
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad or the component is
     * not accepting any more connections
     */
    public ADEGlobals.RecoveryState requestState(
            String uid, // the username
            String upw, // the password
            String reqType, // the type of the requested the component
            String reqName // the name of the requested component
            ) throws AccessControlException, RemoteException;

    /**
     * Provides the means for an {@link ade.ADERegistry ADERegistry} to obtain
     * the execution status of an <tt>ADEComponent</tt> from another {@link
     * ade.ADERegistry ADERegistry}.
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param reqType the type of component
     * @param reqName the name of a particular component
     * @param requester the origin of a connection request
     * @return the {@link ade.ADEGlobals.RecoveryState RecoveryState}
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad or the component is
     * not accepting any more connections
     */
    public ADEGlobals.RecoveryState requestState(
            String uid, // the username
            String upw, // the password
            String reqType, // the type of the requested the component
            String reqName, // the name of the requested component
            ADERegistry onbehalf // the registry requesting on behalf of the component
            ) throws AccessControlException, RemoteException;

    /* * * * * * * * * *
     * Component Interface *
     * * * * * * * * * */
    /**
     * Register a new component.
     *
     * @param si an {@link ade.ADEComponentInfo ADEComponentInfo} structure
     * @param passwd the password the registering component will use
     * @param forward whether this request should be forwarded to other
     * registries too
     * @return the name for the component (if no name was supplied, the new name)
     * @throws RemoteException if registration fails
     */
    public String registerComponent(
            ADEComponentInfo si,
            String passwd,
            boolean forward) throws RemoteException;

    /**
     * Register this {@link ade.ADERegistry ADERegistry} with another at the
     * behest of an already registered registry. This method lets
     * {@link ade.ADERegistryImpl ADERegistryImpl}s maintain a fully connected
     * graph, propagating new registrations.
     *
     * @param newreg the new {@link ade.ADERegistry ADERegistry}
     * @param requester an already registered
     * {@link ade.ADERegistry ADERegistry}
     * @throws RemoteException if registration fails
     * @throws AccessControlException if the requester is unrecognized
     */
    public void registerWithRegistry(
            ADERegistry newreg,
            ADERegistry requester) throws RemoteException, AccessControlException;

    /**
     * Register an {@link ade.ADEGuiComponent ADEGuiComponent}, which is handled
     * slightly differently than other {@link ade.ADEComponent ADEComponent}s.
     *
     * @param gui an {@link ade.ADEGuiComponent ADEGuiComponent}
     * @param uid the user id of the GUI component
     * @param upw the password of the GUI component
     * @throws RemoteException if registration fails
     * @throws AccessControlException if the requester is unrecognized
     */
    //public void registerGui(
    //        ADEGuiComponent gui,
    //        String uid,
    //        String upw) throws RemoteException, AccessControlException;
    /**
     * A component is shutting down and informing the registry; free the component
     * name and the user associated with the component. NOTE: this can usually only
     * be called by the component as nobody else has the components unique,
     * dynamically created password except for registry admins looking up the
     * password. Hence, a component can usually only be shutdown by itself using
     * this function....In particular, this means that if the component is supposed
     * to stay up that something unintentional happened...
     *
     * @param sID the component id
     * @param passwd the password
     * @exception RemoteException if the remote invocation fails
     */
    public void deregisterComponent(
            String sID,
            String passwd) throws RemoteException;

    /**
     * Shutdown an {@link ade.ADEComponent ADEComponent}.
     *
     * @param aid the administrator id
     * @param apw the administrator password
     * @param sType the type of component
     * @param sName the name of the component
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean shutdownComponents(
            String aid, // Administrator username
            String apw, // Administrator password
            String[][] constraints // The constraints to be met by the component
            ) throws RemoteException, AccessControlException;

    /**
     * Start an {@link ade.ADEComponent ADEComponent}, based on the information in an
     * {@link ade.ADEComponentInfo ADEComponentInfo} structure.
     *
     * @param aid the administrator id
     * @param apw the administrator password
     * @param si information about the component
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     * @throws Exception
     */
    public boolean startComponent(
            String aid, // Administrator username
            String apw, // Administrator password
            ADEComponentInfo si) throws RemoteException, AccessControlException, Exception;

    /* * * * * * * * * * * * * * *
     * Registry/Component Interface *
     * * * * * * * * * * * * * * */
    /**
     * Update the heartbeat of an {@link ade.ADEComponent ADEComponent}.
     *
     * @param rmiString the RMI string of the component
     * @param p the new period
     * @throws RemoteException if the request fails
     */
    public void updateHeartBeatPeriod(
            String rmiString,
            int p) throws RemoteException;

    /**
     * Update an {@link ade.ADEComponent ADEComponent}'s status. Parameters include
     * one of the enumeration constants from {@link
     * ade.ADEGlobals.ComponentState}, an {@link java.util.ArrayList ArrayList} of
     * component names to which the component has references, and an {@link
     * java.util.ArrayList ArrayList} of component names that have obtained
     * connections to the component. The return is an {@link java.util.ArrayList
     * ArrayList} composed of {@link ade.ADEGlobals.ComponentState
     * ComponentState} objects, which are <b>only</b> the components whose state is
     * different than what was sent in the <tt>components</tt> list.
     *
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public void updateStatus(ADEMiniComponentInfo amsi //String ID,
            //ADEGlobals.ComponentState state,
            //HashSet<String> components,
            //HashSet<String> clients
            ) throws RemoteException, AccessControlException;

    /**
     * Change the period of time required to pass before the registry starts
     * recovery procedures for an {@link ade.ADEComponent ADEComponent} from the
     * standard one {@link ade.ADEGlobals#DEF_RPPULSE ReaperPeriod} to some
     * multiple of the reaper period. Note that the component will be unavailable
     * during this time, so careful consideration of the effects on the
     * application are required.
     *
     * @param sid the component's identification string
     * @param spw the component's password
     * @param mult the number of reaper periods that must pass before initiating
     * recovery
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public void setRecoveryMultiplier(String sid, String spw, int mult)
            throws RemoteException, AccessControlException;

    /* * * * * * * * * * * * * *
     * Administrator Interface *
     * * * * * * * * * * * * * */
    /**
     * Confirms valid administrator credentials.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public void checkAdmin(
            String aid, // Administrator username
            String apw // Administrator password
            ) throws RemoteException, AccessControlException;

    /**
     * Confirm a valid user. Different from <tt>verifyUser<tt> in that if valid,
     * nothing happens; if invalid, an exception is thrown.
     *
     * @param uid the user's ID
     * @param upw the user's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public void checkUser(
            String uid, // Administrator username
            String upw // Administrator password
            ) throws RemoteException, AccessControlException;

    public ADEUser verifyUser(
            String uid, // User username
            String upw, // User password
            ADERegistry ar // the registry requesting the info
            ) throws RemoteException, AccessControlException;

    /**
     * Add a user and the types of {@link ade.ADEComponent ADEComponent} to which the
     * user is allowed access to the user database.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @param uid the user's ID
     * @param upw the user's password
     * @param types a {@link java.util.HashSet HashSet} of component types to which
     * the user is granted access
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean addUser(
            String aid, // Administrator username
            String apw, // Administrator password
            String uid, // New username
            String upw, // New password
            HashSet<String> types // Strings of allowed types
            ) throws RemoteException, AccessControlException;

    /**
     * Add an administrator to the user database.
     *
     * @param aid the administrator's ID who already has access
     * @param apw the administrator's password who already has access
     * @param uid the new administrator ID
     * @param upw the new administrator password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean addAdmin(
            String aid, // Administrator username
            String apw, // Administrator password
            String uid, // New admin username
            String upw // New admin password
            ) throws RemoteException, AccessControlException;

    /**
     * Delete a user from the user database.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @param uid the user ID to remove
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the administrator does not have
     * adequate permissions
     */
    public boolean delUser(
            String aid, // Administrator username
            String apw, // Administrator password
            String uid // Admin to delete
            ) throws RemoteException, AccessControlException;

    /**
     * Allow a user to remove themselves from the user database.
     *
     * @param uid the user's ID
     * @param upw the user's password
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean delSelf(
            String uid,
            String upw) throws RemoteException, AccessControlException;

    /**
     * Delete an administrator from the user database.
     *
     * @param aid the administrator's ID who already has access
     * @param apw the administrator's password who already has access
     * @param uid the administrator ID to remove
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean delAdmin(
            String aid, // Administrator username
            String apw, // Administrator password
            String uid // Username to delete
            ) throws RemoteException, AccessControlException;

    /**
     * Modify a user's entry in the user database. Note that there is no
     * parameter to change the <tt>isComponent</tt> value, which can only be set
     * when an {@link ade.ADEComponent ADEComponent} registers.
     *
     * @param aid The administrator's username
     * @param apw The administrator's password
     * @param uid The username to modify
     * @param nad Whether the user has administration rights (null to leave
     * as-is)
     * @param nuid The new username of the user (null to leave as-is)
     * @param nupw The new password of the user (null to leave as-is)
     * @param ntypes a {@link java.util.HashMap HashMap} containing the names of
     * components to which the user is granted access (null to leave as-is)
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean modUser(
            String aid, // Administrator username
            String apw, // Administrator password
            String uid, // Username to modify
            boolean nad, // Administrator rights?
            String nuid, // The new username
            String nupw, // The new password
            HashSet<String> ntypes // The types to allow
            ) throws RemoteException, AccessControlException;

    /**
     * Write the user database out to a file.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @param file the name of the file
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean dumpUDB(
            String aid, // Administrator username
            String apw, // Administrator password
            String file // Filename to use
            ) throws RemoteException, AccessControlException;

    /**
     * Read the user database in from a file.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @param file the name of the file (if null, uses <tt>default.udb</tt>)
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean loadUDB(
            String aid, // Administrator username
            String apw, // Administrator password
            String file // Filename to use
            ) throws RemoteException, AccessControlException;

    /**
     * Shut down the registry.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean shutdownRegistry(
            String aid, // Administrator username
            String apw // Administrator password
            ) throws RemoteException, AccessControlException;

    /**
     * Get an {@link java.util.ArrayList ArrayList} of the users currently in
     * the user database.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ArrayList<ArrayList<String>> listUDB(
            String aid, // Administrator username
            String apw // Administrator password
            ) throws RemoteException, AccessControlException;

    /**
     * Get an {@link java.util.ArrayList ArrayList} of the administrators
     * currently in the user database.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ArrayList<String> listAdmins(
            String aid, // Administrator username
            String apw // Administrator password
            ) throws RemoteException, AccessControlException;

    /**
     * Get an {@link java.util.ArrayList ArrayList} of the {@link
     * ade.ADEComponent ADEComponent}s currently registered.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @return the list of component names
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ArrayList<String> listComponents(
            String aid, // Administrator username
            String apw // Administrator password
            ) throws RemoteException, AccessControlException;

    //public String requestCapabilityString(
    //		String type,   // ADEComponent type
    //		String name    // ADEComponent name
    //		) throws RemoteException, AccessControlException;
    /**
     * Toggle the debugging flag, which will cause lots of console output.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean toggleDebug(
            String aid, // Administrator username
            String apw // Administrator password
            ) throws RemoteException, AccessControlException;

    /**
     * Obtain a {@link java.util.HashMap HashMap} of {@link ade.ADEComponentInfo
     * ADEComponentInfo} structures (keyed on the component reference).
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @return the map of components
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public HashMap<ADEComponent, ADEComponentInfo> getComponentInfos(
            String aid, // Administrator username
            String apw // Administrator password
            ) throws RemoteException, AccessControlException;

    /**
     * Obtain an {@link java.util.ArrayList ArrayList} of references to the
     * currently registered {@link ade.ADEComponent ADEComponent}s.
     *
     * @param aid The administrator's ID
     * @param apw The administrator's password
     * @return The list of component references
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ArrayList<ADEComponent> getComponentRefs(
            String aid, // Administrator username
            String apw // Administrator password
            ) throws RemoteException, AccessControlException;

    /**
     * Retrieves an <tt>ADEComponent</tt>'s state.
     *
     * @param sid The ID of the component making the request
     * @param spw The component's password
     * @return An instance of {@link ade.ADEState ADEState} (if one exists)
     * @throws AccessControlException If the user is unrecognized
     * @throws RemoteException If another error occurs
     */
    public ADEState getComponentState(String sid, String spw)
            throws AccessControlException, RemoteException;


    /**
     * Stores an <tt>ADEComponent</tt>'s state.
     *
     * @param sid The ID of the component making the request
     * @param spw The component's password
     * @param state An instance of {@link ade.ADEState ADEState}
     * @throws AccessControlException If the user is unrecognized
     * @throws RemoteException If another error occurs
     */
    public void setComponentState(String sid, String spw, ADEState state)
            throws AccessControlException, RemoteException;


    /**
     * Obtain an {@link java.util.ArrayList ArrayList} of {@link
     * ade.ADEHostInfo ADEHostInfo} objects (the hosts known to this registry).
     *
     * @param credentials The component's credentials
     * @return The list of {@link ade.ADEHostInfo ADEHostInfo} objects
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ArrayList<ADEHostInfo> requestHostList(Object credentials)
            throws RemoteException, AccessControlException;

    /**
     * Obtain an {@link java.util.ArrayList ArrayList} of all {@link
     * ade.ADEHostInfo ADEHostInfo} objects known to all registries.
     *
     * @param credentials The component's credentials
     * @return The list of {@link ade.ADEHostInfo ADEHostInfo} objects
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public ArrayList<ADEHostInfo> requestHostListAll(Object credentials)
            throws RemoteException, AccessControlException;

    /**
     * Turns infrastructure logging on/off.
     *
     * @param uid The user ID making the request
     * @param upw The user's password
     * @param state <tt>true</tt> to turn logging on, <tt>false</tt> to turn
     * logging off
     * @param requester The <tt>ADERegistry</tt> making the request, if there is
     * one
     * @throws AccessControlException If the user is unrecognized
     * @throws RemoteException If another error occurs
     */
    public void setADEComponentLogging(
            String uid, String upw, boolean state, ADEComponent requester)
            throws AccessControlException, RemoteException;

    /**
     * *********************************************************************
     */
    /**
     * ************ ACCESS METHODS FOR THE ADE SYSTEM VIEW GUI *************
     */
    /**
     * *********************************************************************
     */
    /**
     * NOTE that ALL gui-related methods accept a SystemViewAccess        *
     */
    /**
     * accessKey as the first parameter, for authentication purposes.     *
     */
    /**
     * *********************************************************************
     */
    /**
     * gets information about a registry or, if "entireSystem" flag is set to
     * true, for the entire ADE system (including other registries).
     *
     * @param accessKey
     * @param requestSpecs: An InfoRequestSpecs data structure that defines what
     * information the GUI wants to know (that way, not sending huge amounts of
     * info that the GUI won't be using anyhow)
     * @param entireSystem: A boolean flag to indicate if should report status
     * for the entire system (this registry's info included), or only for the
     * local components (not including one's own self). Needed in order to prevent
     * infinite recursion.
     * @throws RemoteException
     */
    public SystemViewStatusData guiGetRegistryStatus(SystemViewAccess accessKey,
            InfoRequestSpecs requestSpecs, boolean entireSystem) throws RemoteException;

    /**
     * gets status for a PARTICULAR COMPONENT, to be displayed by the GUI.
     *
     * @param accessKey
     * @param componentID (type$name)
     * @param requestSpecs: An InfoRequestSpecs data structure that defines what
     * information the GUI wants to know (that way, not sending huge amounts of
     * info that the GUI won't be using anyhow)
     * @param searchEntireSystem: true if should search the entire ADE system,
     * false if only amongst oneself and local components. Needed in order to
     * prevent infinite recursion.
     * @throws RemoteException
     */
    public SystemViewComponentInfo guiGetComponentStatus(SystemViewAccess accessKey,
            String componentID, InfoRequestSpecs requestSpecs,
            boolean searchEntireSystem) throws RemoteException;

    /**
     * gets GUI visualization possibilities for a particular component.
     *
     * @param accessKey
     * @param componentID (type$name)
     * @param searchEntireSystem: true if should search the entire ADE system,
     * false if only amongst oneself and local components. Needed in order to
     * prevent infinite recursion.
     * @throws RemoteException
     */
    public ADEGuiVisualizationSpecs guiGetComponentVisualizationSpecs(
            SystemViewAccess accessKey, String componentID,
            boolean searchEntireSystem) throws RemoteException;

    /**
     * creates a GUICallHelper, so that can create a new GUI panel for a
     * particular component.
     *
     * @param accessKey
     * @param componentID (type$name)
     * @param searchEntireSystem: true if should search the entire ADE system,
     * false if only amongst oneself and local components. Needed in order to
     * prevent infinite recursion.
     * @throws RemoteException
     */
    public ADEGuiCallHelper guiCreateGUICallHelper(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException;

    /**
     * shuts down component, returning success value
     *
     * @param accessKey
     * @param componentID (type$name)
     * @param searchEntireSystem: true if should search the entire ADE system,
     * false if only amongst oneself and local components. Needed in order to
     * prevent infinite recursion.
     * @throws RemoteException
     */
    public boolean guiShutdownComponent(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException;

    /**
     * returns a human-readable info text, showing current status of component
     *
     * @param accessKey
     * @param componentID (type$name)
     * @param searchEntireSystem: true if should search the entire ADE system,
     * false if only amongst oneself and local components. Needed in order to
     * prevent infinite recursion.
     * @throws RemoteException
     */
    public String guiGetComponentInfoPrintText(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException;

    /**
     * returns config text for the component
     *
     * @param accessKey
     * @param componentID (type$name)
     * @param searchEntireSystem: true if should search the entire ADE system,
     * false if only amongst oneself and local components. Needed in order to
     * prevent infinite recursion.
     * @throws RemoteException
     */
    public String guiGetComponentConfigText(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException;

    /**
     * loads a config file from the GUI
     */
    public void guiLoadConfig(SystemViewAccess accessKey,
            String configFileName) throws RemoteException;

    /**
     * runs a component based on a config string (e.g., STARTCOMPONENT ... ENDCOMPONENT).
     * Similar to the guiLoadConfig, but string-based, and only for a single
     * component.
     */
    public void guiRunComponent(SystemViewAccess accessKey,
            String componentConfigString) throws RemoteException, Exception;

    /**
     * returns the help text for the component (same as running the component with
     * --help)
     */
    public String guiGetComponentHelpText(SystemViewAccess accessKey,
            String componentType) throws RemoteException, Exception;

    /**
     * registers GUI as an output redirection consumer for a component with
     * componentID
     */
    public UUID guiRegisterOutputRedirectionConsumer(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException, IOException;

    /**
     * de-registers GUI as an output redirection consumer for a component with
     * componentID. return true if found the component.
     */
    public boolean guiDeregisterOutputRedirectionConsumer(SystemViewAccess accessKey,
            String componentID, UUID consumerID, boolean searchEntireSystem) throws RemoteException;

    /**
     * returns any output that has been accumulated for the GUI by a particular
     * component, since its last call to this method.
     */
    public String guiGetAccumulatedRedirectedOutput(SystemViewAccess accessKey,
            String componentID, UUID consumerID, boolean searchEntireSystem) throws RemoteException;
}
