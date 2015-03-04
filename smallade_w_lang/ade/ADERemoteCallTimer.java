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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

/**
 * Used to provide a time limit on Remote Method Invocations (RMIs). After
 * creation, call one of the {@link #remoteCall} methods, supplying the method
 * name, the object on which to invoke the method, and the arguments. A timeout
 * value can optionally be specified for each individual call made. <p> By means
 * of reflection, this class obtains the {@link java.lang.reflect.Method
 * Method}s of a remote object on an "as used" basis, storing them in a
 * {@link java.util.concurrent.ConcurrentHashMap ConcurrentHashMap} for constant
 * time access at a later point. Facilities are in place to check parameter
 * types before making the remote call. <p> More information: to allow for
 * network latency and other issues, Java gives RMIs a somewhat large time limit
 * for completion (on the order of 30 seconds to minutes), blocking until the
 * call is either completed or fails (by throwing a
 * {@link java.rmi.RemoteException RemoteException}). Usage of this class gives
 * more precise control of the timeout mechanism via the {@link #remoteCall}
 * method. If a return value is expected, it will be an <tt>Object</tt> that
 * must then be cast appropriately in the calling program. <p> Any exceptions
 * that occur in making the remote call will be wrapped by an
 * {@link ade.exceptions.ADEException ADEException}. If the call times
 * out, an {@link ade.exceptions.ADETimeoutException ADETimeoutException} (a
 * subclass of <tt>ADEException</tt>) will be thrown; note that in this
 * case, the method's completion is ambiguous (e.g., the timeout might be due to
 * network latencies). <p> Example usage: <ul> <li>No return value expected,
 * with remote object <tt>robj</tt> that has no parameters:<p>
 * <tt>ADERemoteCallTimer rcTimer = new ADERemoteCallTimer(robj);</tt><br>
 * <tt>...</tt><br> <tt>try {</tt><br> <div style="margin-left: 40px;">
 * <tt>rcTimer.remoteCall(robj) {</tt><br>
 * <tt>System.out.println("Completed!");</tt><br> </div> <tt>} catch
 * (ADEException ace) {</tt><br> <div style="margin-left: 40px;">
 * <tt>System.err.println("Did not complete due to: "+ ace);</tt><br> </div>
 * <tt>}</tt></li> <li>Return value of type <tt>T</tt> expected, calling method
 * <tt>meth</tt> on remote object <tt>robj</tt> and using parameter array
 * <tt>p</tt>:<p> <tt>ADERemoteCallTimer rcTimer = new ADERemoteCallTimer(robj,
 * p);</tt><br> <tt>...</tt><br> <tt>T value;</tt><br> <tt>try {</tt><br> <div
 * style="margin-left: 40px;"> <tt>value = (T)rcTimer.remoteCall(robj);</tt><br>
 * <tt>System.out.println("Completed! Got "+ value);</tt><br> </div> <tt>} catch
 * (ADEException ace) {</tt><br> <div style="margin-left: 40px;">
 * <tt>System.err.println("Did not complete due to: "+ ace);</tt><br> </div>
 * <tt>}</tt></li> </ul>
 *
 * In more detail, each remote call is performed using a <tt>RemoteCaller</tt>
 * instance, which extends <tt>Runnable</tt> and is submitted to an {@link
 * java.util.concurrent.ExecutorService ExecutorService}, which uses a cached
 * thread pool and the specified timeout for completion. The
 * <tt>RemoteCaller</tt> objects are created as needed; to avoid re-creating one
 * for every call, they are re-used (and not garbage collected until the
 * <tt>ADERemoteCallTimer</tt> itself is garbage collected). The total number of
 * <tt>RemoteCaller</tt> objects created will be the maximum number of
 * concurrent method calls active at any point in the lifetime of the
 * instantiated <tt>ADERemoteCallTimer</tt>.
 */
public class ADERemoteCallTimer {

    static private String prg = "ADERemoteCallTimer";
    static private boolean debug = false;
    // private storage for reusing this RMI timer
    private int time;
    private Boolean timeGuard = new Boolean(false);
    private boolean captureTimes = false;
    private String myRCTName = prg;
    private String remoteClassName = null;   // will always be $Proxy??
    private Class remoteObjectType = null;   // ditto?
    private ConcurrentLinkedQueue<RemoteCaller> freeCallers;
    private ExecutorService singExeService;  // single remote object calls
    private ConcurrentHashMap<String, List<MethodInfo>> remoteMethods;
    private ConcurrentHashMap<String, MethodInfo> remoteMethodsFast;
    private HashMap<String, ADEMethodConditions> allMethods;
    private ExecutorService multExeService;  // multiple concurrent calls
    private Boolean setupGuard = new Boolean(false);
    private Boolean resultGuard = new Boolean(false);
    private ConcurrentHashMap<Thread, ArrayList<Callable>> multCallers;
    private ConcurrentHashMap<Thread, List<Future<Object>>> multResults;

    /**
     * Makes storing (and re-locating) remote methods easier.
     */
    private class MethodInfo {

        /**
         * The string representation of the remote method.
         */
        public String remoteMethodName;
        /**
         * The Method object that will be invoked.
         */
        public Method toinvoke;
        /**
         * The parameter types taken by the method.
         */
        public Class[] remoteParamTypes = null;
        /**
         * The return type.
         */
        public Class remoteReturnType;
        /**
         * The String identifier of this method (concatenation of method name
         * and parameter types in the format returned by one of the
         * {@link ade.ADEGlobals#getMethodString getMethodString} methods); used
         * as a unique key to find this object in a HashMap. Note that the key
         * matches some set of Objects passed, <b>not</b> necessarily the exact
         * signature of the Method.
         */
        public String key;
        /**
         * The preconditions for the action taken by this method
         */
        public String[] preconditions;
        /**
         * The postconditions for the action taken by this method
         */
        public String[] postconditions;
        /**
         * The failure conditions for the action taken by this method
         */
        public String[] failureconditions;
        /**
         * The original method condition are obtained from the interface
         */
        public ADEMethodConditions mc;

        /**
         * Constructor that reflectively obtains all {@link
         * java.lang.reflect.Method Method}s that can be invoked on the remote
         * component.
         *
         * @param mn the method name
         * @param ps an array of (possible) parameter classes
         * @throws ADEException exception that may chain other exceptions
         */
        public MethodInfo(String mn, Class[] cargs) throws ADEException {
            // store the pre, post, and failure conditions
            remoteParamTypes = cargs;
            key = ADEGlobals.getMethodString(mn, remoteParamTypes);
            // get the method to invoke and its return type
            try {
                toinvoke = remoteObjectType.getMethod(mn, remoteParamTypes);
            } catch (Exception e) {
                // the above should have taken care of the vast majority of
                // methods, but there are some special cases due to the fact
                // that parameter types must be exact when reflecting (e.g.,
                // parameter types need to be coerced or parameters are
                // primitive types but are received as boxed Objects); take
                // care of such special cases here...since we'll only have
                // to do this once for each set of parameter types, we're
                // not too worried about the computational expense.
                if (debug) {
                    System.err.println(myRCTName + ": exception getting " + mn + " method:");
                    System.err.println("\t" + e);
                    System.err.println("\tdelving further for " + mn + " method...");
                }
                // limit the search to methods with the same name
                ArrayList<Method> matchmns = new ArrayList<Method>();
                Method[] ms = remoteObjectType.getMethods();
                for (Method m : ms) {
                    if (mn.equals(m.getName())) {
                        matchmns.add(m);
                    }
                }
                if (debug) {
                    System.out.println("\t\tTotal " + ms.length + " methods");
                    System.out.println("\t\tChecking " + matchmns.size() + " methods");
                }
                // set up the data we'll use in the loop
                boolean found = false;
                Class[] ptypes;
                for (Method m : matchmns) {
                    //System.out.println("CHECKING " + m);
                    // short-circuit if number of params is different
                    ptypes = m.getParameterTypes();
                    if (ptypes.length != remoteParamTypes.length) {
                        continue;
                    }
                    //System.out.println("==> " + ADEGlobals.getMethodString(m));
                    // there is a function that produces the method strings directly from classes
                    if (key.equals(ADEGlobals.getMethodString(m))) {
                        if (debug) {
                            System.out.println("\t\t\tGot it!");
                        }
                        toinvoke = m;
                        //key = mstr;
                        found = true;
                        break;
                    }
                    // MS: fixed the conversion/coercion problem -- this has to be done together
                    // as there can be any combination of "auto-boxed", i.e., converted and coerced parameters...
                    found = true;
                    for (int i = 0; i < ptypes.length; i++) {
                        //System.out.println("  -- " + ADEGlobals.primitiveToObject(ptypes[i]));
                        if (!ADEGlobals.primitiveToObject(ptypes[i]).isAssignableFrom(remoteParamTypes[i])) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        if (debug) {
                            System.out.println("\t\t\tGot it2!");
                        }
                        toinvoke = m;
                        //key = ADEGlobals.getMethodString(mn, ptypes);
                        remoteParamTypes = ptypes;
                        break;
                    }
                }
                if (!found) {
                    if (debug) {
                        System.out.println("\t\t\tMethod " + mn + " not found!");
                    }
                    throw new ADEException("Method " + key + " not found");
                }
            }
            // now get whatever annotations are available
            for (Annotation annotation : toinvoke.getAnnotations()) {
                if (annotation instanceof ADEMethodConditions) {
                    // store it locally and also extract the individual conditions
                    mc = (ADEMethodConditions) annotation;
                    preconditions = mc.Preconditions();
                    postconditions = mc.Postconditions();
                    failureconditions = mc.Failureconditions();
                    if (debug) {
                        System.out.println("Annotations too, nice!");
                    }
                }
            }
            remoteMethodName = mn;
            remoteReturnType = toinvoke.getReturnType();
        }

        /**
         * Confirms an array of arguments matches the parameters of this method.
         *
         * @param args an array of parameter instances
         * @return <tt>true</tt> if each element of <tt>args</tt> matches the
         * expected method parameter types (in order), <tt>false</tt> otherwise
         */
        public boolean confirmArgTypes(Object[] args) {
            if ((args == null || args.length < 1) && remoteParamTypes == null) {
                return true;
            }
            if (args.length != remoteParamTypes.length) {
                return false;
            }
            for (int i = 0; i < args.length; i++) {
                //if (args[i].getClass() != remoteParamTypes[i]) return false;
                if (debug) {
                    System.out.print(myRCTName + ": checking " + remoteParamTypes[i].getName());
                    System.out.println(".isAssignableFrom(" + args[i].getClass() + ")");
                }
                if (!remoteParamTypes[i].isAssignableFrom(args[i].getClass())) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Utility data structure for concurrent calls of a single method to
     * multiple remote objects (e.g., {@link #remoteCallConcurrent}). When a
     * method is invoked on multiple remote objects concurrently, a set of
     * <tt>RemoteCaller</tt> objects are submitted for execution using the      {@link java.util.concurrent.ExecutorService#invokeAll(Collection)
     * invokeAll} method, which returns a list of {@link
     * java.util.concurrent.Future Future} objects upon completion. To make the
     * <tt>remoteCallConcurrent</tt> method itself thread-safe, the list of
     * results is stored. need to be returned in the same order as the objects.
     * To allow multiple threads concurrent access to the
     * {@link #remoteCallConcurrent} methods,
     */
    /**
     * The class that actually makes the remote method call. Note that prior to
     * use, the <tt>reset</tt> method must be called, setting the method,
     * invoking object, and arguments. This allows us to avoid creation of a new
     * <tt>RemoteCaller</tt> for every call, instead maintaining a
     * {@link java.util.concurrent.ConcurrentLinkedQueue ConcurrentLinkedQueue}
     * of <tt>RemoteCaller</tt> objects that are not being used. During a remote
     * call, an element is removed from the queue, then put back after
     * completion; the size of the queue will be the largest number of
     * concurrent remote calls active at any point.
     */
    private class RemoteCaller implements Callable {

        public MethodInfo mi = null;  // the Method information object
        public Object robj = null;    // the remote object
        public Object[] rargs = null; // the arguments for meth

        /**
         * Prepare to make a remote method call. Performs some parameter
         * validation, locates the {@link java.lang.reflect.Method Method}
         * object corresponding to <tt>mn</tt>, and stores requisite info.
         *
         * @param mn The method name
         * @param obj The remote object on which <tt>mn</tt> will be invoked
         * @param args The parameters for <tt>mn</tt>
         * @throws ADEException If an error occurs
         */
        public void reset(String mn, Object obj, Object... args)
                throws ADEException, ADERequestMethodsException {
            if (debug) {
                System.out.println(myRCTName + ".RemoteCaller: resetting");
            }
            // some parameter validation
            validateParams(mn, obj);
            // so far so good; now get the method (or find and store it)
            // one thing to note: a method may be stored multiple times if its
            // parameters can take different forms (e.g., can be coerced); we
            // store each combination encountered to avoid repeating the process
            // of locating the suitable method
            if (debug) {
                System.out.println(myRCTName + ": locating method " + mn);
            }
            reset(getMethodInfo(mn, args), obj, args);
        }

        public void reset(MethodInfo minfo, Object obj, Object... args) {
            mi = minfo;
            robj = obj;
            rargs = args;
        }

        @Override
        public Object call() throws ADEException {
            Object obj = null;
            if (debug) {
                System.out.println(myRCTName + ".RemoteCaller: " + mi.key);
            }
            try {
                obj = mi.toinvoke.invoke(robj, rargs);
            } catch (Throwable e) {
                if (debug) {
                    System.err.println(myRCTName + " caught " + e);
                    //e.printStackTrace();
                }
                // MS: free the caller...
                freeCallers.add(this);
                throw new ADEException(mi.remoteMethodName + " invocation failed", e);
            }
            freeCallers.add(this);
            return obj;
        }
    }

    /**
     * Constructor with specified timeout and remote object. Methods will be
     * added as they are referenced/used.
     *
     * @param t the timeout (in milliseconds)
     * @param robj the remote object; we just care about the type
     * @throws ADEException Superclass exception that may chain other
     * exceptions
     */
    public ADERemoteCallTimer(int t, Object robj) throws ADEException {
        this(t, robj.getClass());
    }

    /**
     * Constructor with specified timeout and remote object. Methods will be
     * added as they are referenced/used.
     *
     * @param t the timeout (in milliseconds)
     * @param c the remote class; we just care about the type
     * @throws ADEException Superclass exception that may chain other
     * exceptions
     */
    public ADERemoteCallTimer(int t, Class c) throws ADEException {
        if (debug) {
            System.out.println(myRCTName + ": in constructor...");
        }
        time = t;
        if (c == null) {
            throw new ADEException("Cannot have a remote object of null type");
        }
        singExeService = Executors.newCachedThreadPool();
        multExeService = Executors.newCachedThreadPool();
        freeCallers = new ConcurrentLinkedQueue<RemoteCaller>();
        freeCallers.add(new RemoteCaller());
        multCallers = new ConcurrentHashMap<Thread, ArrayList<Callable>>();
        multResults = new ConcurrentHashMap<Thread, List<Future<Object>>>();
        remoteMethods = new ConcurrentHashMap<String, List<MethodInfo>>();
        remoteMethodsFast = new ConcurrentHashMap<String, MethodInfo>();
        allMethods = new HashMap<String, ADEMethodConditions>();
        remoteObjectType = c;
        remoteClassName = remoteObjectType.getName();
        myRCTName = "RCT->" + remoteClassName;
    }

    /**
     * Explicit shutdown/disposal of the class. Call this to assure that the
     * <tt>ExecutorService</tt> is properly shutdown.
     */
    public void terminate() {
        try {
            singExeService.shutdown();
            multExeService.shutdown();
        } catch (Exception e) {
            System.err.println(myRCTName + " encountered during disposal:\n" + e);
        }
    }

    /**
     * Allows retrieval of a {@link java.lang.reflect.Method Method} object from
     * the class-wide <tt>remoteMethods</tt> hash (used for access from within
     * inner classes). Note that the hash key is the method's signature (or a
     * slight variation thereof), as returned by the {@link
     * ade.ADEGlobals#getMethodString getMethodString} method in
     * <tt>ADEGlobals</tt>; if the method is being used for the first time, a
     * new <tt>MethodInfo</tt> object will be added to the hash for quick
     * subsequent lookups.
     *
     * @param mn The method name
     * @param args Example arguments for calling the method; used to obtain the
     * parameter types, so the values are irrelevant
     * @return The Method object, if one exists
     * @throws ADEException
     */
    public Method getMethod(String methodname, Object... args) throws ADERequestMethodsException {
        return getMethodInfo(methodname, args).toinvoke;
    }

    /**
     * returns the pre-conditions associated with this method
     */
    public HashMap<String, ADEMethodConditions> getAllMethods() throws ADERequestMethodsException {
        return allMethods;
    }

    /**
     * returns the pre-conditions associated with this method
     */
    public String[] getPreconditions(String methodname, Object... args) throws ADERequestMethodsException {
        return getMethodInfo(methodname, args).preconditions;
    }

    /**
     * returns the post-conditions associated with this method
     */
    public String[] getPostconditions(String methodname, Object... args) throws ADERequestMethodsException {
        return getMethodInfo(methodname, args).postconditions;
    }

    /**
     * returns the failure-conditions associated with this method
     */
    public String[] getFailureconditions(String methodname, Object... args) throws ADERequestMethodsException {
        return getMethodInfo(methodname, args).preconditions;
    }

    /**
     * Adds an allowable method to the method list
     */
    synchronized public void setAllMethods(ArrayList<String> methods) throws ADERequestMethodsException, ADEException {
        for (String methodstring : methods) {
            int start = methodstring.indexOf('(') + 1;
            int end = methodstring.indexOf(')');
            // System.out.println("==> processing " + methodstring.substring(0,start-1));
            String argstring = methodstring.substring(start, end);
            Class[] c = null;
            if (!argstring.equals("")) {
                String[] args = argstring.split(",");
                c = new Class[args.length];
                try {
                    for (int i = 0; i < args.length; i++) {
                        c[i] = Class.forName(args[i]);
                    }
                } catch(ClassNotFoundException cnf) {
                    throw new ADEException("Strange: could not find class for arguments in " + methodstring + " " + cnf);
                }
            }
            // now search for the method and store it if found
            //System.out.println("PUTTING " + methodstring);
            String methodname = methodstring.substring(0, start - 1);
            List l = remoteMethods.get(methodname);
            if (l == null) {
                remoteMethods.put(methodname, l = new ArrayList<MethodInfo>());
            }
            MethodInfo mi = new MethodInfo(methodname, c);
            l.add(mi);
            // and also store it for fast access
            remoteMethodsFast.put(methodstring, mi);
            // and build the list of all methods with their annotations
            allMethods.put(methodstring, mi.mc);
        }
    }

    /**
     * Set the remote class name (mostly for exception or debugging messages).
     * The remote class name is set automatically during construction but, due
     * to the way Java handles remote objects (constructing a <tt>Proxy</tt>),
     * the name will be uninformative and one may wish to have a better
     * indication of what type of object this timer is handling.
     *
     * @param name the name of the type of remote object
     */
    public void setName(String name) {
        myRCTName = "RCT:" + name;
    }

    /**
     * Set the length of the timeout. A timeout of 0 will wait indefinitely.
     *
     * @param t The new timeout length (in ms)
     * @throws IllegalArgumentException Thrown if the timeout value is less than
     * 0
     */
    public void setTimeout(int t) {
        if (t < 0) {
            throw new IllegalArgumentException("Negative timeout");
        }
        synchronized (timeGuard) {
            time = t;
        }
    }

    /**
     * Invoke method <tt>mn</tt> for the remote object instance <tt>robj</tt>
     * with parameters <tt>args</tt> using the assigned timeout. Note that all
     * exceptions thrown derive from the superclass {@link ade.exceptions.ADEException
     * ADEException}.
     *
     * @param mn the name of the method to invoke
     * @param robj the remote object
     * @param args the parameter array for method <tt>methodName</tt>
     * @return The value returned by method <tt>mn</tt> as an <tt>Object</tt>
     * (<tt>null</tt> if the return type is <tt>void</tt>)
     * @throws ADETimeoutException if the remote call times out
     * @throws ADEException Superclass wrapper exception
     */
    public Object remoteCall(int to, Callable obj) throws
            ADETimeoutException, ADEException {
        long start = 0;
        if (captureTimes) {
            start = System.currentTimeMillis();
        }

        if (debug) {
            System.out.println(myRCTName + ": in remoteCall on Callable...");
        }
        // do the (timed) remote call
        try {
	    // if to is negative, do not block
            if (to < 0) {
		// simply return the future
		return singExeService.submit(obj);
	    }
	    else if (to == 0) {
		// MS: why don't we just call it, because we're blocking anyway?  Re-use the current thread???
		try {
		    return obj.call();
		} catch(Exception e) {
		    throw new ADEException("Execution exception in blocking call: ", e);
		}
		// block on the return value
		//return singExeService.submit(obj).get();
            } else {
                // else wait for the timeout
                return singExeService.submit(obj).get(to, TimeUnit.MILLISECONDS);
            }
        } catch (TimeoutException te) {
            throw new ADETimeoutException("No completion in " + to + "ms");
        } catch (RejectedExecutionException ree) {
            throw new ADEException("Rejected Execution", ree);
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause != null) {
                throw new ADEException("Execution exception", cause);
            } else {
                throw new ADEException("Execution exception", ee);
            }
        } catch (InterruptedException ie) {
            throw new ADEException("Interrupted", ie);
	}
    }

    /** Invoke method <tt>mn</tt> for the remote object instance <tt>robj</tt>
     * with parameters <tt>args</tt> using the assigned timeout. Note that all
     * exceptions thrown derive from the superclass {@link ade.exceptions.ADEException
     * ADEException}.
     * @param mn the name of the method to invoke
     * @param robj the remote object
     * @param args the parameter array for method <tt>methodName</tt>
     * @return The value returned by method <tt>mn</tt> as an <tt>Object</tt>
     * (<tt>null</tt> if the return type is <tt>void</tt>)
     * @throws ADETimeoutException if the remote call times out
     * @throws ADEException Superclass wrapper exception */
    public Object remoteCallNonBlocking(String mn, Object robj, Object... args) throws
            ADETimeoutException, ADEException {
        return remoteCall(-1, mn, robj, args);
    }


    /**
     * Invoke method <tt>mn</tt> for the remote object instance <tt>robj</tt>
     * with parameters <tt>args</tt> using the assigned timeout. Note that all
     * exceptions thrown derive from the superclass {@link ade.exceptions.ADEException
     * ADEException}.
     *
     * @param mn the name of the method to invoke
     * @param robj the remote object
     * @param args the parameter array for method <tt>methodName</tt>
     * @return The value returned by method <tt>mn</tt> as an <tt>Object</tt>
     * (<tt>null</tt> if the return type is <tt>void</tt>)
     * @throws ADETimeoutException if the remote call times out
     * @throws ADEException Superclass wrapper exception
     */
    public Object remoteCall(String mn, Object robj, Object... args) throws
            ADETimeoutException, ADEException {
        return remoteCall(time, mn, robj, args);
    }

    /**
     * Invoke method <tt>mn</tt> for the remote object instance <tt>robj</tt>
     * with parameters <tt>args</tt>. Note that all exceptions thrown derive
     * from the superclass
     * {@link ade.exceptions.ADEException ADEException}.
     *
     * @param to The timeout in milliseconds; a value of 0 waits indefinitely
     * @param mn The name of the method to invoke
     * @param robj The remote object
     * @param args The parameter array for method <tt>methodName</tt>
     * @return The value returned by method <tt>mn</tt> as an <tt>Object</tt>
     * (<tt>null</tt> if the return type is <tt>void</tt>)
     * @throws ADETimeoutException if the remote call times out
     * @throws ADEException Superclass wrapper exception
     */
    public Object remoteCall(int to, String mn, Object robj, Object... args)
            throws ADETimeoutException, ADEException {
        try {
            return remoteCall(to, getRemoteCaller(mn, robj, args));
        } catch (ADERequestMethodsException rme) {
            throw new ADEException("Method " + mn + " not accessible for user", rme);
        }
    }

    /**
     * Invoke method <tt>mn</tt> on a group of remote objects using the already
     * specified timeout value. For more information, see the other
     * <tt>remoteCallConcurrent</tt> method.
     *
     * @param mn The name of the method to call
     * @param robjs An array of remote objects
     * @param args The parameters to pass to method <tt>mn</tt>
     * @throws ADEException Thrown if any of the invocations will fail
     * (e.g., a null remote object or unknown method); note that if this
     * exception is not thrown, it does <b>not</b> signify anything about the
     * status of any individual remote call (as described above)
     */
    public Object[] remoteCallConcurrent(
            String mn, Object[] robjs, Object... args)
            throws ADEException, ADERequestMethodsException {
        return remoteCallConcurrent(time, mn, robjs, args);
    }

    /**
     * Invoke method <tt>mn</tt> on each remote object instance in the
     * <tt>robjs</tt> array concurrently with a timeout value of <tt>to</tt>,
     * using the same set of parameters <tt>args</tt> for each. The order of the
     * returned values will correspond to the order of the <tt>robjs</tt> array
     * (i.e., the value returned by <tt>robj[0]</tt> will be at index 0 of the
     * return array). <p> While this method may be called safely by multiple
     * threads, each thread is granted exclusive access (i.e., will cause other
     * threads to block) twice during operation, once for <i>setup</i> and again
     * for processing <i>results</i>. The operations in these synchronized
     * blocks are: <ul>Setup: <li>Parameter validation</li> <li><tt>Method</tt>
     * retrieval</li> <li>Retrieval/creation of <tt>RemoteCaller</tt>
     * objects</li> </ul> <ul>Results: <li>Return array creation (holds
     * individual results)</li> <li>Return array filling</li> </ul> At this
     * point, the timeout value is only used when making the remote calls; time
     * devoted to the <i>setup</i> and <i>results</i> phases are taken into
     * account. <p> <b>NOTE:</b> the intention of this method is to complete as
     * many of the method calls as possible (hopefully all); however, any (or
     * all) of the remote method calls may fail -- and will do so silently. The
     * caller is responsible for checking each returned value to see if it is an {@link
     * ade.exceptions.ADEException ADEException} and, if so, to handle
     * it appropriately. Nonetheless, if it is determined that any (or all) of
     * the calls cannot be executed prior to making them (e.g., one of the
     * remote objects is <tt>null</tt>, an unknown method is specified, etc.),
     * an <tt>ADEException</tt> will be thrown pre-emptively.
     *
     * @param to The timeout in milliseconds; a value of 0 waits indefinitely
     * @param mn The name of the method to call
     * @param robjs An array of remote objects
     * @param args The parameters to pass to method <tt>mn</tt>
     * @throws ADEException Thrown if any of the invocations will fail
     * (e.g., a null remote object or unknown method); note that if this
     * exception is not thrown, it does <b>not</b> signify anything about the
     * status of any individual remote call (as described above)
     * @throws ADETimeoutException Thrown if the specified timeout value is
     * negative
     */
    public Object[] remoteCallConcurrent(
            int to, String mn, Object[] robjs, Object... args)
            throws ADEException, ADETimeoutException, ADERequestMethodsException {
        if (to < 0) {
            throw new ADEException("Negative timeout value");
        }
        if (robjs.length < 1) {
            throw new ADEException("No remote objects for method " + mn);
        }
        MethodInfo minfo = null;
        ArrayList<Callable> calls;
        Object[] returnValues;
        long start = 0;
        if (captureTimes) {
            start = System.currentTimeMillis();
        }

        if (debug) {
            System.out.println(myRCTName + ": in remoteCallConcurrent(" + mn + ")");
        }
        synchronized (setupGuard) {
            // if there's only a single call being made, use the single call
            // method, as it's more efficient (no object creation, etc.)
            if (robjs.length == 1) {
                returnValues = new Object[1];
                try {
                    returnValues[0] = remoteCall(to, mn, robjs[0], args);
                } catch (Throwable t) {
                    returnValues[0] = t;
                }
                return returnValues;
            }
            // otherwise, setup the call ArrayList and store it
            for (Object robj : robjs) {
                validateParams(mn, robj);
            }
            minfo = getMethodInfo(mn, args);
            calls = getRemoteCallers(minfo, robjs, args);
            if (calls == null) {
                throw new ADEException("No remote calls to make");
            }
            multCallers.put(Thread.currentThread(), calls);
        }

        // invoke the calls for the current thread
        try {
            if (to == 0) {
                multResults.put(Thread.currentThread(), multExeService.invokeAll(
                        (Collection) multCallers.get(Thread.currentThread())));
            } else {
                multResults.put(Thread.currentThread(), multExeService.invokeAll(
                        (Collection) multCallers.get(Thread.currentThread()),
                        to, TimeUnit.MILLISECONDS));
            }
        } catch (InterruptedException ie) {
            // at least one of the calls didn't complete; indicate in return value
        } catch (NullPointerException npe) {
            // at least one of the tasks was null; should never happen
        } catch (RejectedExecutionException ree) {
            // at least one of the tasks was rejected; indicate in return value
        }

        // retrieve the results and return them
        synchronized (resultGuard) {
            List<Future<Object>> futs = multResults.remove(Thread.currentThread());
            returnValues = new Object[futs.size()];
            int i = 0;
            for (Future fut : futs) {
                try {
                    returnValues[i] = fut.get();
                } catch (CancellationException ce) {
                    returnValues[i] = new ADEException("Call cancelled", ce);
                } catch (ExecutionException ee) {
                    returnValues[i] = new ADEException("Execution exception", ee);
                } catch (InterruptedException ie) {
                    returnValues[i] = new ADEException("Get interupted", ie);
                }
                i++;
            }
            multCallers.remove(Thread.currentThread());
            return returnValues;
        }
    }

    /**
     * Make sure we have a method name and an appropriate object.
     */
    private void validateParams(String mn, Object robj)
            throws ADEException {
        boolean err = false;
        StringBuilder sb = new StringBuilder();
        if (robj == null) {
            err = true;
            sb.append("Remote object is null");
        } else if (!remoteObjectType.isAssignableFrom(robj.getClass())) {
            err = true;
            sb.append("Wrong object type; expected ");
            sb.append(remoteClassName);
            sb.append(", got ");
            sb.append(robj.getClass().getName());
        } else if (mn == null || mn.equals("")) {
            err = true;
            sb.append("No method specified");
        }
        if (err) {
            throw new ADEException(sb.toString());
        }
    }

    /**
     * Get (or create, if necessary) the <tt>MethodInfo</tt> object.
     */
    private MethodInfo getMethodInfo(String mn, Object... args) throws ADERequestMethodsException {
        //System.out.println("TRYING TO GET " + ADEGlobals.getMethodString(mn, args));
        String mstring = ADEGlobals.getMethodString(mn, args);
        MethodInfo quick = remoteMethodsFast.get(mstring);
        if (quick != null) {
            //System.out.println("+++ FAST ACCESS to " + mn);
            return quick;
        }
        //System.out.println("No fast access, locating " + mn);
        // otherwise we need to search
        Class[] c = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            c[i] = args[i].getClass();
        }
        List<MethodInfo> methodlist = remoteMethods.get(mn);
        if (methodlist == null) {
            throw new ADERequestMethodsException("Access to method info for " + mn + " not allowed.");
        }
        //System.out.println("Got list with " + methodlist.size() + " elements");
        for (MethodInfo mi : methodlist) {
            // skip if parameter lengths are not the same
            if (mi.remoteParamTypes.length != c.length) {
                continue;
            }
            boolean found = true;
            for (int i = 0; i < mi.remoteParamTypes.length; i++) {
                //System.out.println("  -- " + ADEGlobals.primitiveToObject(ptypes[i]));
                if (!ADEGlobals.primitiveToObject(mi.remoteParamTypes[i]).isAssignableFrom(c[i])) {
                    found = false;
                    break;
                }
            }
            if (found) {
                //System.out.println("Found it");
                // if the methodstrings are not the same, add the new signature for faster look-up
                remoteMethodsFast.put(mstring, mi);
                return mi;
            }
        }
        return null;
    }

    /**
     * Gets (or creates, if necessary) a <tt>RemoteCaller</tt> object that will
     * be used to make a remote call.
     */
    private synchronized RemoteCaller getRemoteCaller(String mn,
            Object robj, Object... args) throws ADEException, ADERequestMethodsException {
        RemoteCaller rc = freeCallers.poll();
        if (rc == null) {
            rc = new RemoteCaller();
        }
        rc.reset(mn, robj, args);
        return rc;
    }

    /**
     * Gets (or creates, if necessary) a <tt>RemoteCaller</tt> objects for each
     * element of <tt>robjs</tt>. This method avoids the overhead that would
     * result from repeatedly making individual <tt>getRemoteCaller</tt> calls.
     *
     * @param mi The <tt>Method</tt> information
     * @param robjs The array of remote objects on which the method will be
     * invoked
     * @param args The parameters that will passed to the method
     * @return An array list of <tt>robjs.length</tt> <tt>RemoteCaller</tt>
     * objects
     * @throws ADEException If a <tt>RemoteCaller</tt> cannot be
     * <tt>reset</tt>
     */
    private synchronized ArrayList<Callable> getRemoteCallers(
            MethodInfo mi, Object[] robjs, Object... args)
            throws ADEException {
        RemoteCaller rc;
        int added = 0;
        ArrayList<Callable> callers =
                new ArrayList<Callable>(robjs.length);
        while ((rc = freeCallers.poll()) != null && added < robjs.length) {
            rc.reset(mi, robjs[added], args);
            callers.add(rc);
            added++;
        }
        while (added < robjs.length) {
            rc = new RemoteCaller();
            rc.reset(mi, robjs[added], args);
            callers.add(rc);
            added++;
        }
        return callers;
    }

    /**
     * Return a string representation of this object.
     *
     * @param listMethods If <tt>true</tt>, include a full list of the methods
     * stored (so far) for this object
     * @return A formatted String with information about this object
     */
    public String toPrintString(boolean listMethods) {
        StringBuilder sb = new StringBuilder(prg);
        sb.append(": ");
        sb.append(myRCTName);
        sb.append("\nRemote class:   ");
        sb.append(remoteClassName);
        sb.append("\nTimeout:        ");
        sb.append(time);
        if (listMethods) {
            sb.append("\nStored methods: ");
            if (remoteMethods.isEmpty()) {
                sb.append(" <none>");
            } else {
                for (MethodInfo mi : remoteMethodsFast.values()) {
                    sb.append("\n");
                    sb.append(mi.remoteReturnType);
                    sb.append(" ");
                    sb.append(mi.key);
                }
            }
        } else {
            sb.append("\n#Stored methods: ");
            sb.append(remoteMethods.size());
        }
        sb.append("\n#Free callers:  ");
        sb.append(freeCallers.size());
        return sb.toString();
    }

    /**
     * Print information (front-end to {@link #toPrintString}).
     *
     * @param listMethods If <tt>true</tt>, include a full list of the methods
     * stored (so far) for this object
     */
    public void print(boolean listMethods) {
        System.out.println(toPrintString(listMethods));
    }
}
