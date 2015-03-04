/*********************************************************************
 *
 * SWAGES 1.1
 *
 * (c) by Matthias Scheutz <mscheutz@nd.edu>
 *
 * Pop11 Parallel Client
 *
 * Last modified: 12-28-06
 *
 *********************************************************************/

discout('/tmp/swages') ->> cucharout -> cucharerr;

uses unix_sockets;
lconstant this_dir = sys_fname_path(popfilename);

;;; ------------------- load libraries --------------------------
;;; load the simulator and all required files
compile(this_dir dir_>< 'simworld.p');
;;; additional pop11 files for network communication
compile(this_dir dir_>< 'datasockets.p');

;;; ------------------- globals for client stuff  --------------------------
global vars
    ;;; socket used for communication with server
    comm_sock,

    ;;; by default the program runs in client mode, to stop this, pass
    ;;; either a command line argument, or call the simulation with a
    ;;; particular function
    clientID = false,
    connection_attempts = 50,   ;;; attempts to connect to the server before giving up
    waitforconnection = 10,     ;;;(in sec.)
    receive_attempts = 3,       ;;; number of attempts to receive a message

    ;;; default for communciation with the server
    ;;; this will be overwritten by experiment setup file
    servername = '127.0.0.1',
    serverport = 10002,

    ;;; for non-cluster mode:
    ;;; used to check whether there is a user on the console
    checkMIGRATION = false,     ;;; whether we should perform checks on the host
    maxtimeconsoleuser = 24000, ;;; run for little less than 5 min. of CPU time

    ;;; this will save the state of the simulation
    saveSTATE = false,          ;;; saveSTATE * checkINTERVAL = frequency at which simstate is saved
    timercount = 0,             ;;; variable containing the number of times the migration check has been performed
    IDLETIME = 30,		;;; how much idle time console user needs to accumulate before we'll start a job on his machine
    KEEPOUTPUT = false,

    proxyLOW = false,
    proxyHIGH = false,
    PARALLEL = false,
;

;;; message types
lconstant 
     PREQ = 0,
     SINT = 1, 
     SCNT = 2,
     SPAR = 3,
     CPAR = 4,
     DONE = 5,
     SSAV = 6,
     CCYC = 7,
     PCYC = 8,
     PSAV = 9,
     SPLT = 10;

;;; for debugging: print out lots of comments
lconstant verbose = false;

;;; parallel SWAGES stuff
global vars proxy_objects = [],
	    nonproxy_objects = [],
	    normal_objects = [],
	    killme, interactions, final_objects, sim_run_agent_stackloc,
	    some_agent_ran,
	    ;

define :mixin proxy;
    slot proxy_update_cycle == 0;
    slot proxy_update_list == [ sim_x sim_y ];
    slot cpu_total == 0; ;;; Total CPU time for agent
    slot cpu_elapsed == [% 0 %]; ;;; List of CPU times for agent
    slot cpu_sense_total == 0; ;;; Total sensing CPU time for agent
    slot cpu_sense_elapsed == 0; ;;; Sensing CPU time for current cycle
    slot cpu_action_total == 0; ;;; Total action CPU time for agent
    slot cpu_action_elapsed == 0; ;;; Action CPU time for current cycle
    slot cpu_overall == 0; ;;; CPU time for the current process
    slot real_total == 0; ;;; Total real time for agent
    slot real_elapsed == [% 0 %]; ;;; List of real times for agent
enddefine;

define :method proxy_update(entity:proxy, updatelist);
    lvars sname, sval;
    for sname sval in proxy_update_list(entity), updatelist do
	sval -> valof(sname)(entity);
    endfor;
enddefine;

define par_sim_scheduler(objects, lim); enddefine;

;;; this will try to send a message and if it does not succeed, it will reopen a new socket
;;; uses the globals: comm_sock, servername, and serverport
define keep_sending_message(mID,mtype,mdata);
    repeat connection_attempts times
    	returnif(send_message(mID,mtype,mdata,comm_sock))(true);
    	;;; close socket and reestablish connection to server and send ID, so the server
    	sys_socket(`i`, `S`, false) -> comm_sock;
    	[%servername, serverport%] -> sys_socket_peername(comm_sock);
	syssleep(waitforconnection*100);
    endrepeat;
    sysexit();
    return(false);
enddefine;

define saveit(imagename);
    ;;; Suppress warning messages
    dlocal cucharerr = discout('/dev/null');
    return(syssave(imagename));
enddefine;

;;; this will save the state of the simulation and notify the server
define migrate(level,safety);
    if saveit(imagename) then
/*
	;;; first check whether the CID is still the same, this can be seen from the image name
	;;; 2nd or 3rd argument
	lvars us = locchar_back(`_`,length(poparglist(2)),poparglist(2)),
	     dot = locchar_back(`.`,length(poparglist(2)),poparglist(2)),
	     imID = consword(substring(us,dot-us,poparglist(2)));

	;;; if they are not identical, the server must have changed the ID for this client, so use the new one
	unless consword(imID) = clientID then
	    imID -> clientID;
	endunless;
*/
        ;;; the clientID is passed as an argument
	poparglist(1) -> clientID;
	discout('/tmp/swages_' >< clientID) ->> cucharout -> cucharerr;

	lvars tobecompiled = false;
	lconstant verbose = true;
	;;; try until successful
    	while not(tobecompiled) do
    	    ;;; establish connection to server and send ID, so the server
	    sys_socket(`i`, `S`, false) -> comm_sock;
	    [%servername, serverport%] -> sys_socket_peername(comm_sock);
	
    	    ;;; contact server and request the simulation parameters
	    until keep_sending_message(clientID,SCNT,[]) do 
		if verbose then npr('keep_sending_message failed for client ' >< clientID); endif;
	    enduntil;
    	    if verbose then npr(clientID >< ' Sent SCNT to server, waiting for CPAR...') endif;
	
    	    ;;; now get the simulation parameters
    	    wait_for_message(clientID,CPAR,comm_sock) -> tobecompiled;
    	endwhile;
        compile(stringin(tobecompiled(3)));
	if proxyLOW then
	    ;;; for loop--go through nonproxy list and place in proper list
	    lvars i = 1, tmpobj, newnonproxy = [], newproxy = [];
	    for tmpobj in nonproxy_objects do
		if i >= proxyLOW and i <= proxyHIGH then
		    tmpobj :: newnonproxy -> newnonproxy;
		else
		    tmpobj :: newproxy -> newproxy;
		endif;
		i + 1 -> i;
	    endfor;
	    rev(newnonproxy) -> nonproxy_objects;
	    ;;; Note that this does not preserve the order of proxy objects
	    rev(newproxy) -> proxy_objects;
	    false -> proxyLOW;
	endif;
	if verbose then npr(clientID >< ' Received CPAR, continuing simulation..') endif;
    else
        ;;; check whether we saved it for safety reasons, then we don't need to inform the server
	unless safety then
    	    npr('Client '>< clientID >< ' attempting to migrate...');	
	    saveSTATSFILE =>
	    until keep_sending_message(clientID,SINT,[%level,sysfilesize(imagename)%]) do enduntil;
	    if verbose then npr('Sent SINT to server, quitting simulation') endif;
    	    npr('Client ' ><  clientID >< ' gone...');	
            ;;; Clean up the psv files in /tmp, unless we're coming back to this
	    ;;; machine	   
	    unless level = 3 then
/*		if sys_file_exists(imagename) then
		    ;;; delete it if it exists
		    sysobey('/bin/rm', [%'/bin/rm',imagename%]);
		endif;
        */
		if sys_file_exists(imagename><'-') then
		    sysobey('/bin/rm', [%'/bin/rm',imagename><'-'%]);
		endif;
                if sys_file_exists(imagename >< '--') then
    	            sysobey('/bin/rm', [%'/bin/rm',imagename><'--'%]);
                endif;
                if sys_file_exists(imagename >< '---') then
    	            sysobey('/bin/rm', [%'/bin/rm',imagename><'---'%]);
                endif;
	    endunless;
	    sysexit(1);
	;;; otherwise let the server know that we are still around and that we saved state, so it can grab the image
	else
	    ;;; level contains the cycle number
	    until keep_sending_message(clientID,SSAV,[%level,sysfilesize(imagename),false%]) do enduntil;
	endunless;
	unless KEEPOUTPUT then
	    discout('/tmp/swages') ->> cucharout -> cucharerr;
	    if sys_file_exists('/tmp/swages_' >< clientID) then
		sysobey('/bin/rm', [%'/bin/rm','/tmp/swages_' >< clientID%]);
	    endif;
	endunless;
    endif;
enddefine;

;;; check whether the time t is within the interval (s,e)
define within_time_interval(t,s,e);
    lvars (th,tm,ts) = sys_parse_string(t,`:`,strnumber),
	 (sh,sm,ss) = sys_parse_string(s,`:`,strnumber),
	 (eh,em,es) = sys_parse_string(e,`:`,strnumber),
	 c = th*3600 + tm*60 + ts;
    (sh*3600 + sm*60 + ss < c) and (c < eh*3600 + em*60 + es)
enddefine;

;;; NOTE: this function overwrites the save_simstate in simworld.p  !!!
;;; this performs all kinds of periodic checks of the host on which the simulation is
;;; running according to the following check levels (if the conditions are met, the simulation
;;; will migrate):
;;; 1 - any other user logged on
;;; 2 - a console user looged on
;;; 3 - a console user logged on and "maxtimeconsoleuser" was exceeded, in sec.
;;; 4 - the cpu time of the process dropped below "mincputime"
;;; all of this can be specified for certain intervals of the day
;;; the argument is a list of lists each of which is of the form
;;;	[level] sets runlevel
;;;	[level param] if level is 4, param is maximum allowable load
;;;		      
;;;	[level starttime endtime]
;;; regardless of the above the state of the simulation can also be saved at regular intervals

;;; TODO: here we could also check the socket and see if the server request any information from us,
;;; e.g., the current number of sim_cycles, etc. and send it across
define save_simstate(cycle);
    dlocal pop_asts_enabled;
    if useTIMER then
    	false -> pop_asts_enabled;
    else
    	;;; increase the timer count
    	timercount + 1 -> timercount;
    endif;

    lvars alreadymigrated = false;
    ;;; check if any user is logged on
    if checkMIGRATION then
	lvars item, currenttime = substring(12,8,sysdaytime());
	for item in checkMIGRATION do
	    lvars level = item(1),
		 timeok = if length(item) = 3 then
		              within_time_interval(currenttime,item(2),item(3))
		          elseif length(item) = 2 then
			      item(2)
			  else true endif;
            ;;; check if anybody is on
    	    if level == 1 and timeok then
	    	if line_repeater(pipein('./test_user',[],false),255)()/='false' then
	    	    migrate(1,false);
		    true->alreadymigrated;
		    quitloop(1);
	    	endif;
   	    ;;; check if a console user is on
    	    elseif level == 2  and timeok then
       	    	if line_repeater(pipein('./test_console',[],false),255)()/='false' then
    	    	    migrate(2,false);
		    true->alreadymigrated;
		    quitloop(1);
	    	endif;
	    ;;; check if a console user is on and the overall CPU time of the pop process
 	    ;;; exceeded the "maxtimeconsoleuser"
            elseif level == 3  and timeok then
	    	if line_repeater(pipein('./test_console',[],false),255)()/='false' and
               		(systime() > maxtimeconsoleuser) then
               		;;;(systime() * 100 > maxtimeconsoleuser) then
    	    	    migrate(3,false);
		    true->alreadymigrated;
		    quitloop(1);
	    	endif;	
	    ;;; check if the percentage of CPU this process gets is less a certain percentage
 	    elseif level == 4 then
		if strnumber(line_repeater(pipein('./test_cpu',[],false),255)()) < timeok then
    	    	    migrate(4,false);
		    true->alreadymigrated;
		    quitloop(1);
	    	endif;	
    	    elseif level == 5  and timeok then
		lvars idleresp,idleentry,idletime;
       	    	line_repeater(pipein('./test_console_idle',[],false),255)() -> idleresp;
		if idleresp = termin then
		    migrate(5,false);
		    true->alreadymigrated;
		    quitloop(1);
		endif;
       	    	if idleresp /= 'false' then
		    sysparse_string(idleresp) -> idleresp;
		    1 -> idleentry;
		    0 -> idletime;
		    if null(idleresp) then
			;;; No idle time, better get off the machine
			migrate(5,false);
			true->alreadymigrated;
			quitloop(1);
		    endif;
		    while length(idleresp) > idleentry and isnumber(idleresp(idleentry)) do
			if idleresp(idleentry + 1) = 'days' or 
			   idleresp(idleentry + 1) = 'day' then
			    idletime + (24*60*idleresp(idleentry)) -> idletime;
			elseif idleresp(idleentry + 1) = 'hours' or
			       idleresp(idleentry + 1) = 'hour' then
			    idletime + (60*idleresp(idleentry)) -> idletime;
			elseif idleresp(idleentry + 1) = 'minutes' or
			       idleresp(idleentry + 1) = 'minute' then
			    idletime + (idleresp(idleentry)) -> idletime;
			endif;
			idleentry + 2 -> idleentry;
		    endwhile;
		    if idletime < IDLETIME then
			;;; Idle time less than threshold, get off machine
			migrate(5,false);
			true->alreadymigrated;
			quitloop(1);
		    endif;
	    	endif;
	    ;;; Migrate if I'm too close to 5 min CPU limit, regardless of user
            elseif level == 6  and timeok then
	    	if systime() > maxtimeconsoleuser then
    	    	    migrate(3,false);
		    true->alreadymigrated;
		    quitloop(1);
	    	endif;	
	    ;;; check if the percentage of CPU this process gets is less a certain percentage
	    elseif level == -1 then
		;;; after split
    	    endif;
	endfor;
    endif;
    ;;; check if we need to save the state for safety reasons, then we will continue here...
    if isnumber(saveSTATE) and not(alreadymigrated) and (timercount rem saveSTATE = 0) then
	;;; save state without migrating, pass on the cycle number to server if necessary
	migrate(0,cycle);	
    ;;; unless we have already migrated and notified the server, produce a "current cycle" (CCYC) update message if required
    elseif not(alreadymigrated) then
	if PARALLEL then
	    lvars updatelist, offer, pobject, npobject, message, xlist, ylist,
		  deadp = [],
		  ;
	    /*
	    [%  for npobject in nonproxy_objects do
		    sim_cycle_number -> proxy_update_cycle(npobject);
		    [% float_code_bytes(sim_x(npobject) * 1.0) %] -> xlist;
		    [% float_code_bytes(sim_y(npobject) * 1.0) %] -> ylist;
		    [% sim_name(npobject), cycle, "sim_x", xlist, "sim_y", ylist %]
		    ;;;[% sim_name(npobject), sim_cycle_number, npobject %]
		endfor %] -> offer;
	    */
	    [] -> offer;
	    until keep_sending_message(clientID,PCYC, [% cycle, length(nonproxy_objects), offer, [] %]) do enduntil;   
	    receive_message(clientID, comm_sock) -> message;
	    message(3) -> updatelist;
	    for pobject in updatelist do
		lvars i = 3;
		lvars AID = valof(pobject(1));
		;;;pobject(3) -> idval(identof(pobject(1)));
		pobject(2) -> proxy_update_cycle(AID);
		while i <= length(pobject) do
		    ;;; PWS: later we can use the proxy class update
		    ;;; function, but since the message 
		    explode(pobject(i + 1)) -> float_code_bytes() -> valof(pobject(i))(AID);
		    i + 2 -> i;
		endwhile;
		;;; PWS: This is a clumsy attempt to detect death...
		if sim_x(AID) = 1000000 then
		    dies_basic_agent(AID, "eaten");
		    AID :: deadp -> deadp;
		endif;
		/*
		*/
	    endfor;
	    if deadp /= [] then
		[% for pobject in proxy_objects do unless lmember(pobject, deadp) then pobject endunless endfor %] -> proxy_objects;
	    endif;
	    ;;; PWS: need to check for SPLT
	    if message(2) = SPLT then
		npr('Received SPLT');
		migrate(-1, false);
	    endif;
	else
	    until keep_sending_message(clientID,CCYC,[%cycle%]) do enduntil;   
	endif;
    endif;
    ;;; if the timer is used, set it
    if useTIMER then
        checkINTERVAL -> sys_timer(save_simstate);
    	true -> pop_asts_enabled;
    endif;
enddefine;

;;; replace the item following a key in an arbitrary list structure
define replacevalofkey(l,key,newitem);
    if null(l) then
        []
    elseif hd(l) == key then
        [% hd(l), newitem %] <> replacevalofkey(tl(tl(l)),key,newitem)
    elseif islist(hd(l)) then
        conspair(replacevalofkey(hd(l),key,newitem),
                 replacevalofkey(tl(l),key,newitem))
    else
        conspair(hd(l),replacevalofkey(tl(l),key,newitem))
    endif;
enddefine;

;;; returns the list of values with matching key, false if there are none
define getvalsfromkey(l,k);
    lvars x;
    for x in l do
        if hd(x) = k then
            return(tl(x))
        endif;
    endfor;
    false
enddefine;

;;; this function is the main entry point for the client
;;; it gets all the simulation parameters from the host
define runclient(params);
    ;;; make yourself independent of the calling shell, the father returns immediately, only the child continues
    if sys_fork(true) then 
	return;	
    endif;

    ;;;if verbose then npr('runclient executing') endif;
    ;;; get server, port, clientID, imagename (including the path), the checking frequency, the save state info, the local scratch
    ;;; info, and the ranseed
    unless null(params) then
        explode(params) -> (servername,serverport,clientID);
    endunless;
    discout('/tmp/swages_' >< clientID) ->> cucharout -> cucharerr;

    lvars simparammessage = false, tmpparseuser;
    lconstant verbose = false;

    ;;; this keeps reconnecting until the simparam messages is received
    ;;; MS: quoted out    while not(simparammessage) do
    ;;; establish connection to server and send ID, so the server
    sys_socket(`i`, `S`, false) -> comm_sock;
    [%servername, serverport%] -> sys_socket_peername(comm_sock);
    
    do
	;;; contact server and request the simulation parameters
        until keep_sending_message(clientID,PREQ,[]) do 
    	    if verbose then npr('keep_sending_message failed for client ' >< clientID); endif;
        enduntil;
    	if verbose then npr(clientID >< ' Sent PREQ to server, waiting for SPAR...') endif;
	sys_real_time() -> lastCHECK;
    	;;; now get the simulation parameters

    	wait_for_message(clientID,SPAR,comm_sock) -> simparammessage;
	;;; MS: quoted out   endwhile;	

    	if verbose then npr(clientID >< ' Received simparams from server: ' >< simparammessage(3)) endif;
	
    	;;; check if the state needs to be saved and/or the checkMIGRATION criteria are still met
    	;;;save_simstate(0);
	
    	;;; ignore server keywords in the parameter lists
    	true -> ignoreUNKNOWNKEYWORD;
    	parseuserPARAMS -> tmpparseuser;
    	procedure(kw,params,tmpproc);
	    lvars k = hd(kw);
	    if k == "parallel" then
	    	;;; PWS: this is where we're supposed to alter params
	    	;;; [% sys_parse_string(hd(entity), `_`) %]
	    	lvars entity;
	    	true -> PARALLEL;
	    	;;; Forces save image after first cycle
	    	if checkINTERVAL then
		    - checkINTERVAL - 1 -> lastCHECK;
	    	endif;
	    	par_sim_scheduler -> sim_scheduler;
	    	[%	for entity in params(2) do
		    	    lvars eparse = [% sys_parse_string(hd(entity), `_`) %],
			  	 rulestring,
			  	 classstring,
			  	 ;
		    	    if eparse(length(eparse)) = 'agent' then
				'vars proxy_' >< hd(entity) >< '_rulesystem = ' >< hd(entity) >< '_rulesystem;' -> rulestring;
				compile(stringin(rulestring));
				'define :class proxy_' >< hd(entity) >< '; is proxy, ' >< hd(entity) >< '; enddefine;' -> classstring;
				compile(stringin(classstring));
				('proxy_' >< hd(entity)) :: tl(entity)
		    	    else
				entity
		    	    endif;
			endfor %] -> params(2);
	    elseif tmpproc then
	    	tmpproc(kw);
	    endif;
    	endprocedure(%tmpparseuser%) -> parseuserPARAMS;
	
    	;;;run simulation on the simparam, which are the in the message body
    	simulate(simparammessage(3));
	
    	;;; tell the server that the client has finished, include the final cycle number
    	until keep_sending_message(clientID,DONE,[%current_cycle,sysfilesize(saveSTATSFILE)%]) do enduntil;
	
    	;;; clean up only backup copies, as the client may still be copying the last image in case something
    	;;; went wrong with it 
    	if sys_file_exists(imagename >< '-') then
    	    sysobey('/bin/rm', [%'/bin/rm',imagename><'-'%]);
    	endif;
    	if sys_file_exists(imagename >< '--') then
    	    sysobey('/bin/rm', [%'/bin/rm',imagename><'--'%]);
    	endif;
    	if sys_file_exists(imagename >< '---') then
    	    sysobey('/bin/rm', [%'/bin/rm',imagename><'---'%]);
    	endif;
    	
    	;;; for reuse of client
    	wait_for_message(clientID,UCID,comm_sock)(3) -> clientID;
    	;;; if we got a new ID, the VM will be re-used, so reset the simulation
    	if not(null(clientID)) then
	    ;;; reset the simulation
	    reset-simulation();
    	endif;
    while clientID;

    unless KEEPOUTPUT then
	discout('/tmp/swages') ->> cucharout -> cucharerr;
	if sys_file_exists('/tmp/swages_' >< clientID) then
	    ;;;sysobey('/bin/rm', [%'/bin/rm','/tmp/swages_' >< clientID%]);
	endif;
    endunless;
    if verbose then npr('All data was transmitted successfully, client ' >< clientID >< ' is shutting down'); endif;
enddefine;

define lconstant procedure sim_stack_check(object, len, name, cycle);
    lvars object, len, name, cycle, inc, mess, vec = {};
    stacklength() - len -> inc;
    returnif(inc == 0);

    if inc fi_> 0 then
	consvector(inc) -> vec;
	'Stack increased by ',
    else
	'Stack decreased by '
    endif sys_>< abs(inc) sys_>< ' items in cycle ' sys_>< cycle -> mess;
    ;;; Reduce call stack before calling mishap
    chain(mess, ['In' ^name, ^object %explode(vec)%], mishap)
enddefine;

define lconstant prb_STOPAGENT(rule_instance, action);
	lvars rest = fast_back(action);
	if ispair(rest) then rest==> endif;
	unless prb_actions_run == 0 then true -> some_agent_ran endunless;
	exitto(sim_run_agent_stackloc);
enddefine;

prb_STOPAGENT -> prb_action_type("STOPAGENT");

define lconstant prb_STOPAGENTIF(rule_instance, action);
	lvars rest = fast_back(action);
	if ispair(rest) then
		if recursive_valof(fast_front(rest)) then
			if ispair(fast_back(rest)) then fast_back(rest) ==> endif;
			unless prb_actions_run == 0 then true -> some_agent_ran endunless;
			exitto(sim_run_agent_stackloc);
		endif
	else
		mishap('MISSING ITEM AFTER STOPAGENTIF', [^action])
	endif;
enddefine;

define par_sim_scheduler(objects, lim);
    lvars proxy_tmp, objects, object, speed, lim, messages, messagelist = [];
    lvars tmptime;
    
    ;;; clear any previously saved objects.
    [] -> final_objects;

    ;;; make the list of objects globally accessible, to methods, etc.
    dlocal pop_pr_places = (`0` << 16) || 16;

    dlocal
	some_agent_ran,

	popmatchvars,
	sim_objects = objects,
	sim_myself ,	    ;;; used so that rules can access self
	sim_cycle_number = 0,	;;; incremented below
	sim_object_delete_list = [],
	sim_object_add_list = [],
	;;; supppress printing of rulesystem information
	prb_noprint_keys = sim_noprint_keys,
	sim_stopping_scheduler = false,
	;

    
    for proxy_tmp in objects do
	if isproxy(proxy_tmp) then
	    proxy_tmp :: nonproxy_objects -> nonproxy_objects;
	    unless isdeclared(sim_name(proxy_tmp)) then
		ident_declare(sim_name(proxy_tmp),0,0);
	    endunless;
	    proxy_tmp -> idval(identof(sim_name(proxy_tmp)));
	else
	    proxy_tmp :: normal_objects -> normal_objects;
	endif;
    endfor;
    rev(nonproxy_objects) -> nonproxy_objects;
    rev(normal_objects) -> normal_objects;

    ;;; First ensure that rulesystems are all analysed and rulesets stored
    ;;; in databases, etc.

    procedure();    ;;; exitto(sim_scheduler), will exit this
      lvars len = stacklength();

        ;;; make sure all agents have been setup before anything
	;;; starts
      applist(sim_objects, sim_setup);

      repeat
	lvars pobject, npobject, pobjectlist, updatelist = [], rmlist = [],
	      offer, request, message, npobjectlist, deadnp = [], deadp = [],
	      npupdatelist = [],
	      ;
	;;; check whether to abort
	quitif(sim_cycle_number == lim);	;;; never true if lim = false

	;;; PWS: Here I'm going through the proxy agents to see which need
	;;; updating; working on a copy of the list

	
	lvars newpobjectlist = copylist(proxy_objects);	
	;;;lvars pobjectlist = copylist(proxy_objects);	
	fast_for npobject in nonproxy_objects do
	    lvars ints = interactions(npobject), ints2, npupdate = false;

	    if killme(npobject) then
		npobject :: deadnp -> deadnp;
		1000000 -> sim_x(npobject);
		1000000 -> sim_y(npobject);
		nextloop;
	    endif;
 	    newpobjectlist -> pobjectlist;
	    [] -> newpobjectlist;
	    [% fast_for pobject in pobjectlist do
		lvars pdist = sim_cycle_number - proxy_update_cycle(pobject), idist, idist2;
		
		if pdist > 0 then
		    lvars cname = class_name(datakey(pobject));
		    unless ints and (ints matches ![== ^cname ?idist ==]) then
			max_range(npobject) -> idist;
		    endunless;
		    ;;;npr('it\'s been ' >< pdist >< ' cycles');
		    pdist * maxspeed(pobject) -> pdist;
		    ;;;npr('could\'ve gone ' >< pdist >< ' units');
		    sim_distance(npobject, pobject) - pdist -> pdist;

		    ;;;npr('so it could be ' >< pdist >< ' close to me');
		    if abs(pdist) < idist then
			;;;npr('proxy ' >< sim_name(pobject) >< ' may be in range of nonproxy ' >< sim_name(npobject));
			pobject :: updatelist -> updatelist;
			true -> npupdate;
		    else
			lvars cname2;
		    	interactions(pobject) -> ints2;
		    	class_name(datakey(npobject)) -> cname2;
		    	unless ints2 and (ints2 matches ![== ^cname2 ?idist2 ==]) then
			    max_range(pobject) -> idist2;
		    	endunless;
			if abs(pdist) < idist2 then
			    ;;;npr('nonproxy ' >< sim_name(npobject) >< ' may be in range of proxy ' >< sim_name(pobject));
			    pobject :: updatelist -> updatelist;
			    true -> npupdate;
			endif;
		    endif;
		else
		    pobject
		endif;
	    endfast_for %] -> newpobjectlist;
	endfast_for;

	;;; PWS: now create the message
	[%  for pobject in updatelist do
		sim_name(pobject)
	    endfor %] -> request;
	/*
	[%  for pobject in updatelist do
		[% sim_name(pobject), "sim_x", "sim_y" %] 
	    endfor %] -> request;
	*/
	if request /= [] or deadnp /= [] then
	    lvars xlist, ylist, deaths = false, updateprint = [];
	    ;;; make this a vector for faster access later
	    {%  fast_for npobject in nonproxy_objects do
		    sim_cycle_number -> proxy_update_cycle(npobject);
		    {% float_code_bytes(sim_x(npobject) * 1.0) %} -> xlist;
		    {% float_code_bytes(sim_y(npobject) * 1.0) %} -> ylist;
		    {% sim_name(npobject), if killme(npobject) then 1000000 else sim_cycle_number endif, "sim_x", xlist, "sim_y", ylist %}
		    ;;;[% sim_name(npobject), sim_cycle_number, npobject %]
		endfast_for %} -> offer;
	    ;;;'offer:' =>
	    ;;;offer ==>
	    keep_sending_message(clientID,PCYC,[% sim_cycle_number, length(nonproxy_objects), offer, request %]) -> ;
	    receive_message(clientID, comm_sock) -> message;
	    message(3) -> updatelist;
	    fast_for pobject in updatelist do
		lvars i = 3;
		lvars AID = valof(pobject(1));
		pobject(2) -> proxy_update_cycle(AID);
		while i <= length(pobject) do
		    ;;; PWS: later we can use the proxy class update
		    ;;; function, but since the message 
		    explode(pobject(i + 1)) -> float_code_bytes() -> valof(pobject(i))(AID);
		    i + 2 -> i;
		endwhile;
		;;; PWS: This is a clumsy attempt to detect death...
		if sim_x(AID) = 1000000 then
		    dies_basic_agent(AID, "eaten");
		    true -> deaths;
		    AID :: deadp -> deadp;
		endif;
		[% sim_name(AID), proxy_update_cycle(AID), "sim_x", sim_x(AID), "sim_y", sim_y(AID) %] :: updateprint -> updateprint;
	    endfast_for;
	    ;;; PWS: I need to do this here because the deaths might come
	    ;;; before the cycle, but objects are not removed until after
	    ;;; the cycle.  The return from SPLT should be OK, though,
	    ;;; because it happens at the end of the cycle.
	    if deaths then
		sim_edit_object_list(sim_objects, sim_cycle_number) -> sim_objects;
	    endif;
	    ;;; PWS: Need to check here for SPLT
	    if message(2) = SPLT then
		migrate(-1, false);
	    endif;
	    ;;;npr('updates:');
	    ;;;rev(updateprint) ==>
	endif;
	
	;;; this should be made faster too, take them out above
	if deadnp /= [] then
	    [% fast_for npobject in nonproxy_objects do unless lmember(npobject, deadnp) then npobject endunless endfast_for %] -> nonproxy_objects;
	endif;
	if deadp /= [] then
	    [% fast_for pobject in proxy_objects do unless lmember(pobject, deadp) then pobject endunless endfast_for %] -> proxy_objects;
	endif;
	
	sim_cycle_number fi_+ 1 -> sim_cycle_number;
	;;; Allow user-definable setup, e.g. for graphics
	sim_setup_scheduler(sim_objects, sim_cycle_number);

	false -> some_agent_ran;    ;;; may be set true in sim_run_agent

	;;; go through all objects running their rules, then do
	;;; post-processing to update world
	;;; PWS: only going through non-proxy objects
	fast_for object in normal_objects do

	    ;;; XXX should do interval check here?
	    ;;; Must check if agent needs to be set up, in case it is
	    ;;; new or has been given a new rulesystem
	    unless sim_setup_done(object) then sim_setup(object) endunless;
	    object -> sim_myself;
            [] -> popmatchvars;
	    ;;; NOW LET THE AGENT DO ITS INTERNAL STUFF
	    ;;; PWS: sending all objects, could limit for performance
	    sim_run_agent(object, sim_objects);
	    sim_stack_check(object, len, sim_run_agent, sim_cycle_number);
	endfast_for;
	fast_for object in nonproxy_objects do

	    ;;; XXX should do interval check here?
	    ;;; Must check if agent needs to be set up, in case it is
	    ;;; new or has been given a new rulesystem
	    unless sim_setup_done(object) then sim_setup(object) endunless;
	    object -> sim_myself;
            [] -> popmatchvars;
	    ;;; NOW LET THE AGENT DO ITS INTERNAL STUFF
	    ;;; PWS: sending all objects, could limit for performance
	    timediff() -> tmptime;
	    sim_run_agent(object, sim_objects);
	    timediff() -> cpu_sense_elapsed(object);
	    cpu_sense_elapsed(object) + cpu_sense_total(object) -> cpu_sense_total(object);
	    sim_stack_check(object, len, sim_run_agent, sim_cycle_number);

	endfast_for;
	
	
;;;	if some_agent_ran then
	    ;;; now distribute messages and perform actions to update world
	    ;;; PWS: only going through non-proxy objects
	    fast_for object in normal_objects do;
		object -> sim_myself;
		sim_do_actions(object, sim_objects, sim_cycle_number);
	    endfast_for;

	    fast_for object in nonproxy_objects do;
		object -> sim_myself;
		timediff() -> tmptime;
		sim_do_actions(object, sim_objects, sim_cycle_number);
		timediff() -> cpu_action_elapsed(object);
		cpu_action_elapsed(object) + cpu_action_total(object) -> cpu_action_total(object);
		cpu_sense_total(object) + cpu_action_total(object) -> cpu_total(object);
		(cpu_sense_elapsed(object) + cpu_action_elapsed(object)) :: cpu_elapsed(object) -> 
		    cpu_elapsed(object);
		systime() -> cpu_overall(object);
	    endfast_for;
/*
	else
	    ;;; no rules were fired in any object
	    no_objects_runnable_trace(normal_objects <> nonproxy_objects, sim_cycle_number)
	endif;
*/
	sim_scheduler_pausing_trace(normal_objects <> nonproxy_objects, sim_cycle_number);

	;;; This can add new objects or delete old ones
	sim_edit_object_list(sim_objects, sim_cycle_number) -> sim_objects;

	;;; This can be used for updating a connected simulation, etc.
	sim_post_cycle_actions(normal_objects <> nonproxy_objects, sim_cycle_number);

	;;; Moved after call to sim_post_cycle_actions at suggestion of BSL
	[] ->> sim_object_delete_list -> sim_object_add_list;

	;;; Added 8 Oct 2000
        if sim_stopping_scheduler then 
	    sim_stop_scheduler(); 
	endif;

      endrepeat
    endprocedure();

    sim_scheduler_finished(sim_objects, sim_cycle_number);
    lvars xlist, ylist, npobject, offer, message;
    [%  fast_for npobject in nonproxy_objects do
	    npr('FINAL: ' >< sim_name(npobject) >< ' sim_x ' >< sim_x(npobject) >< ' simy ' >< sim_y(npobject));
	    sim_cycle_number -> proxy_update_cycle(npobject);
	    [% float_code_bytes(sim_x(npobject) * 1.0) %] -> xlist;
	    [% float_code_bytes(sim_y(npobject) * 1.0) %] -> ylist;
	    [% sim_name(npobject), sim_cycle_number, "sim_x", xlist , "sim_y", ylist %]
	    ;;;[% sim_name(npobject), sim_cycle_number, npobject %]
	endfast_for %] -> offer;
    keep_sending_message(clientID,PCYC,[% sim_cycle_number, length(nonproxy_objects), offer, [] %]) -> ;
    receive_message(clientID, comm_sock) -> message;
    npr('Quitting!');

    sim_objects -> final_objects;

enddefine;

