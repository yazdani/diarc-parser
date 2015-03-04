/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * LRFComponentImpl.java
 *
 * @author Paul Schermerhorn
 */
package com.lrf;

import ade.*;
import ade.gui.ADEGuiVisualizationSpecs;

import java.rmi.*;
import java.util.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.*;

import com.lrf.feature.Door;
import com.lrf.polar_laser_scan;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import vulcan_lcm.*;
import lcm.lcm.*;


/**
 * Base class for LRF servers.
 */
abstract public class LRFComponentImpl extends ADEComponentImpl implements LRFComponent {
    private Log log = LogFactory.getLog(getClass());
    /* ADE-related fields (pseudo-refs, etc.) */

    protected static String prg = "LRFComponentImpl";
    protected static String type = "LRFComponent";
    protected static boolean publishReadings = false;
    protected static boolean verbose = false;
    protected static boolean vverbose = false;
    protected static boolean debugGetLaserScan = false;
    static public boolean useLocalLogger = false;
    //    public Object lrfpub;

    /* Laser-specific fields */
    protected static String portName = "/dev/ttyUSB0";
    protected static boolean userPort = false;
    protected boolean status = false;
    protected static final float badRangeVal = -1f;     //ignores these range values
    protected int scanID = 0;
    protected int numReadings = 0;
    protected double scanAngle = 0.0;
    protected static boolean cutoffFound = false;
    protected static int cutoff = 0;
    protected int eastReading = 0;
    protected double[] readings;
    protected float[] readingsF;
    protected int[] history;
    protected static double CRITICAL_DIST = 0.6;
    protected boolean obstacle = false;
    protected boolean safeFront = false;
    protected boolean safeLeft = false;
    protected boolean safeRight = false;
    protected boolean safeBack = false;
    protected boolean openFront = false;
    protected boolean openLeft = false;
    protected boolean openRight = false;
    protected boolean openBack = false;
    //public double MINOPEN = 120; // 60 beams * 2 m
    public double MINOPEN = 50; // 60 beams * 2 m
    public int MINHIST = 1; // Watch for 1 cycles

    protected static boolean finishedLoading = false;
    protected static double offset = 0.0;
    protected static boolean flipped = false;
    protected static boolean coordinatesRead = false;
    protected double shortMax = 0.0;
    protected static double criticalspeed = 0.0;
    protected static double robotRadius = 0.0;
    protected static double mountX = 0.0;
    protected static double mountY = 0.0;
    protected static Point2D.Double mountingPoint = new Point2D.Double(0.0,0.0);
    protected static ArrayList<Point2D.Double> robotCorners = new ArrayList<Point2D.Double>();
    protected static double cRV; //current rotating velocity.
    protected LaserFeatureDetector detector;    
    protected static LCM lcm;
    protected static polar_laser_scan polarscan = new polar_laser_scan();
    

    // ***********************************************************************
    // * Abstract methods in ADEComponentImpl that need to be implemented
    // ***********************************************************************
    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    protected void clientConnectReact(String user) {
        System.out.println(myID + ": got connection from " + user + "!");
        return;
    }

    /**
     * This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     */
    protected boolean clientDownReact(String user) {
        System.out.println(myID + ": lost connection with " + user + "!");
        return false;
    }

    /**
     * This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed). 
     */
    protected void componentDownReact(String serverkey, String[][] constraints) {
        String s = constraints[0][1];

        return;
    }

    /** This method will be activated whenever the heartbeat reconnects
     * to a client (e.g., the server this is sending a heartbeat to has
     * failed and then recovered). <b>Note:</b> the pseudo-reference will
     * not be set until <b>after</b> this method is executed. To perform
     * operations on the newly (re)acquired reference, you must use the
     * <tt>ref</tt> parameter object.
     * @param serverkey the getID of the {@link ade.ADEComponent ADEComponent} that connected
     * @param ref the pseudo-reference for the requested server */
    protected void componentConnectReact(String serverkey, Object ref, String[][] constraints) {
        String s = constraints[0][1];

        return;
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    protected void localshutdown() {
        System.out.print("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println(prg + " shutting down...");
        shutdown();
        System.out.println("...done.");
    }

    // ***********************************************************************
    // Methods available to remote objects via RMI (from LRFComponent)
    // ***********************************************************************
    public polar_laser_scan getPolarScanData() throws RemoteException{
	polarscan.timestamp=System.nanoTime()/1000;
	polarscan.scanID = scanID;
	polarscan.offset_x = (float)mountX;
	polarscan.offset_y = (float)mountY;
	polarscan.offset_t = (float)offset;      
	for(int i = 0; i < readings.length; i++)
	    polarscan.ranges[i]=(float)readings[i];
	return polarscan;
    }

    /**
     * Get the distance that robot takes to be the distance at which obstacle
     * avoidance should engage.
     * @return the critical distance.
     */
    public double getCritDist() throws RemoteException {
        return CRITICAL_DIST;
    }

    /**
     * Set the distance that robot takes to be the distance at which obstacle
     * avoidance should engage.
     * @param dist the new critical distance
     */
    public void setCritDist(double dist) throws RemoteException {
        CRITICAL_DIST = dist;
        System.out.println("LRF: setting crit dist to: " + CRITICAL_DIST);
    }

    /**
     * Get the distance that robot uses to calculate open space.
     * @return the minopen distance.
     */
    public double getMinOpen() throws RemoteException {
        return MINOPEN;
    }

    /**
     * Set the distance that robot uses to calculate open space.
     * @param mo the new minopen distance
     */
    public void setMinOpen(double mo) throws RemoteException {
        MINOPEN = mo;
        System.out.println("LRF: setting minopen to: " + MINOPEN);
    }

    /**
     * Make the lrf aware of the rotational velocity in the event of a turn.
     * @param speed said velocity
     */
    public void setRV(double speed) throws RemoteException {
	cRV = speed;
        System.out.println("LRF: acknowledged: cRV is: " + CRITICAL_DIST + "R/S\n");
    }

    /**
     * Check safe spaces.  A sector is safe if there's no obstacle detected
     * within CRITICAL_DIST.
     * @return an array of booleans indicating safe*
     */
    public boolean[] getSafeSpaces() throws RemoteException {
        return new boolean[]{safeRight, safeFront, safeLeft, safeBack};
    }

    /**
     * Check open spaces.  A sector is open if there's space there for the
     * robot to move into.
     * @return an array of booleans indicating open*
     */
    public boolean[] getOpenSpaces() throws RemoteException {
        return new boolean[]{openRight, openFront, openLeft, openBack};
    }

    /**
     * Check whether there is currently a doorway detected
     * @return true if a doorway is present, false otherwise.
     */
    public boolean checkDoorway() throws RemoteException {
        log.warn("This API is deprecated. Use LaserFeatureExtractorComponent.getDoors().isEmpty() instead.");
        detector.updateFeatures(readings);
        return !detector.getDoors().isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public java.util.List<Door> getDoorways() throws RemoteException {
        log.warn("This API is deprecated. Use LaserFeatureExtractorComponent.getDoors() instead.");
        detector.updateFeatures(readings);
        return detector.getDoors();
    }

    /**
     * {@inheritDoc}
     */
    public boolean checkHallway() throws RemoteException {
        log.warn("This API is deprecated. Use com.lrf.extractor.LaserFeatureExtractorComponent.inHallway() instead.");
        // TODO: this only ever returns false, change back when hallway detection exists
        return false;
    }

    /**
     * Check whether there is currently an obstacle in front.
     * @return true if an obstacle is present, false otherwise.
     */
    public boolean checkObstacle() throws RemoteException {
        return obstacle;
    }

   /**
     * Check the angle of coverage of the LRF.
     * @return the total angle a full scan covers (in radians).
     */
    public double getScanAngle() throws RemoteException {
        log.warn("This API is deprecated. Use getLaserScan() instead.");
        return scanAngle;
    }

    /**
     * Check the angle of coverage of the LRF.
     * @return the total angle a full scan covers (in radians).
     */
    public double getScanOffset() throws RemoteException {
        log.warn("This API is deprecated. Use getLaserScan() instead.");
        return offset;
    }

    /**
     * Check how many readings this LRF returns.
     * @return the number of LRF readings.
     **/
    public int getNumLaserReadings() throws RemoteException {
        log.warn("This API is deprecated. Use getLaserScan() instead.");
        return numReadings;
    }

    /**
     * Get LRF readings.
     * @return the most recent set of LRF readings.
     */
    public double[] getLaserReadings() throws RemoteException {
        log.warn("This API is deprecated. Use getLaserScan() instead.");
        return readings;
    }

    // ********************************************************************
    // Local methods
    // ********************************************************************
    /** 
     * LRFComponentImpl constructor.
     */
    public LRFComponentImpl(final int numLaserReadings, final double laserScanAngle) throws RemoteException {
        super();
new Thread("starter") {
        @Override
            public void run(){
        numReadings = numLaserReadings;
	if(cutoffFound==false)
	    cutoff=numReadings+1;


        scanAngle = laserScanAngle;
        detector = new LaserFeatureDetector(numReadings, scanAngle, offset);
	history = new int[numReadings];
	for(int i = 0; i < numReadings; i++)
	    history[i]=0;
	if(publishReadings){
	    lcm = LCM.getSingleton();
	    readingsF=new float[numReadings];
	    new Thread("publishReadings"){
		@Override
		    public void run(){
		    while(true){
			try{Thread.sleep(20);}catch(Exception e){System.out.println("Insomnia");}
			publish();
		    }
        }
        }.start();
    }
    
    /*      while(lrfpub == null){
        lrfpub=getClient("com.lrf.laserpublisher.LaserPublisherComponent");
        try{
            Thread.sleep(5);
        }catch(Exception e){
        }
        }*/
    polarscan.intensities = new short[numReadings];
    Arrays.fill(polarscan.intensities, (short)0);
    polarscan.ranges = new float[numReadings];
    polarscan.startAngle = (float)((scanAngle/2 + Math.PI + offset) % (2*Math.PI));
    polarscan.angularResolution = (float)(scanAngle/numReadings);
    polarscan.numRanges = (short) numReadings;
    polarscan.maxRange = 8;     

    finishedLoading=true;
	}
    }.start();
    }

    /**This version of updating the LRF server doesn't dictate how safe you are based on your speed,
     * but rather how fast you can go, based on your current safety.                     
     * @author TEW
     * @return the fastest the chair can safely go without running the risk of a collision.
     */
    public double getSafeSpeed() {
	//the shortest a laser reading needs to be to check the robot's speed:
        //the amount of distance the robot can cover in two seconds plus the robot's furthest extension plus   
	//the critical distance.
	double recommendedSpeed;
	if (criticalspeed > 0 && robotRadius > 0){
	    shortMax = 2 * criticalspeed + CRITICAL_DIST + robotRadius;
	    recommendedSpeed = Math.min(criticalspeed, (closestObstacle()-CRITICAL_DIST)/2);
	}
	else
	    recommendedSpeed = Math.max(0, (closestObstacle()-CRITICAL_DIST)/2);
        //You shouldn't come any closer to an obstacle within the next second than 
	//halving the distance from you to it, less the critical distance.                                                                                      

	return recommendedSpeed;
    }
    public double closestObstacle(){
	if (cRV>0)
	    return Math.min(smallestLaserReadingAt(0.0), smallestLaserReadingAt(cRV));
	else
	    return smallestLaserReadingAt(0.0);
    }

    
    private double smallestLaserReadingAt(double angle){
	double shortest = (shortMax == 0.0 ? 999999999 : shortMax);
	double bearing;
	Point2D.Double candidate = new Point2D.Double(0.0,0.0);
	double candidateDistance;
	double cX, cY;
	for (int index = 0; index < numReadings; index++){
	    if (readings[index] != badRangeVal){ 
		//if the laser is short enough to warrant worry
		//Step 1: number of radians the laser covers.
		bearing = scanAngle;
		//Step 2: find angle of first reading.
		bearing = bearing/2 + Math.PI;
		//Step 3: account for offset.
		bearing = (bearing + offset) % (2*Math.PI);
		//Step 4: go to the current laser.
		bearing = (bearing + (scanAngle/numReadings) * (index)) % (2*Math.PI);
		//Finally, if we're looking at a rotating robot, 
		//look where the bearing will be pointed when a second
		//of that rotation is completed.
		bearing = (bearing + angle) % (2*Math.PI);
		//convert from polar to cartesian coordinates.
		cX =readings[index]*Math.cos(bearing)+mountingPoint.getX();
		cY =readings[index]*Math.sin(bearing)+mountingPoint.getY();
		candidate.setLocation(cX, cY);
		candidateDistance=shortestDistance(candidate); //back to meters.
		shortest = Math.min(shortest, candidateDistance);				
	    }
	}
	
	return shortest; 
    }

    private void calculateDistancesToBot(int right_beg, int right_end, 
					  int front_beg, int front_end, 
					  int left_beg, int left_end,
					  int back_beg, int back_end, double angle){
	boolean sFront = true;
	boolean sLeft = true;
	boolean sRight = true; 
	boolean sBack = true;
        double spaceFront = 0;
        double spaceLeft = 0;
        double spaceRight = 0;
	double spaceBack = 0;

	double shortest = (shortMax == 0 ? 999999999 : shortMax);
	double candidateDistance;
	double bearing;
	Point2D.Double candidate = new Point2D.Double(0.0,0.0);
	double cX, cY;
	for (int index = 0; index < numReadings; index++){
	    if(index<cutoff){
		if (readings[index] != badRangeVal){ 
		    //if the laser is short enough to warrant worry
		    //Step 1: number of radians the laser covers.
		    bearing = scanAngle;
		    //Step 2: find angle of first reading.
		    bearing = bearing/2 + Math.PI;
		    //Step 3: account for offset.
		    bearing = (bearing + offset) % (2*Math.PI);
		    //Step 4: go to the current laser.
		    bearing = (bearing + (scanAngle/numReadings) * (index)) % (2*Math.PI);
		    //Finally, if we're looking at a rotating robot, 
		    //look where the bearing will be pointed when a second
		    //of that rotation is completed.
		    bearing = (bearing + angle) % (2*Math.PI);
		    //convert from polar to cartesian coordinates.
		    cX = readings[index]*Math.cos(bearing)+mountingPoint.getX();
		    cY = readings[index]*Math.sin(bearing)+mountingPoint.getY();
		    candidate.setLocation(cX, cY);
		    candidateDistance=shortestDistance(candidate);
		    //		shortest = Math.min(shortest, candidateDistance);					    	
		    
		    if((right_beg <= index) && (index <= right_end)){
			spaceRight += candidateDistance;
			if (candidateDistance <= CRITICAL_DIST){
			    sRight=false;
			    if(verbose)
				System.out.println("Not safe on right: laser # "+index+" is "+candidateDistance+", original was "+readings[index]);
			}
		    }
		    else if((front_beg <= index) && (index <= front_end)){
			spaceFront += candidateDistance;
			if (candidateDistance <= CRITICAL_DIST){
			    sFront=false;
			    if(verbose)
				System.out.println("Not safe in front: laser # "+index+" is "+candidateDistance+", original was "+readings[index]);
			}
		    }
		    else if((left_beg <= index) && (index <= left_end)){
			spaceLeft += candidateDistance;
			if (candidateDistance <= CRITICAL_DIST){
			    sLeft=false;
			    if(verbose)
				System.out.println("Not safe on left: laser # "+index+" is "+candidateDistance+", original was "+readings[index]);
			}
		    }
		    else if((back_beg <= index) && (index <= back_end)){
			spaceBack += candidateDistance;
			if (candidateDistance <= CRITICAL_DIST)
			    sBack=false;
		    }
		}
	    }
	    safeRight = sRight;
	    safeBack = sBack;
	    safeLeft = sLeft;
	    safeFront = sFront;
	    
	    openRight = safeRight && (spaceRight > MINOPEN);
	    openLeft = safeLeft && (spaceLeft > MINOPEN);
	    openFront = safeFront && (spaceFront > MINOPEN);
	    openBack = safeBack && (spaceBack > MINOPEN);
	    
	    //This is for boundary based collision detection	
	    if (false && (!safeLeft || !safeRight || !safeFront /*|| !safeBack*/)) {
		System.out.print(prg + ": COLLISION WARNING: ");
		if (!safeLeft) {
		    System.out.print("left ");
		}
		if (!safeFront) {
		    System.out.print("front ");
		}
		if (!safeRight) {
		    System.out.print("right ");
		}    
		/*	    if (!safeBack) {
			    System.out.print("back ");
			    }*/
		System.out.println("");
	    }
	    if (verbose) {
		System.out.println("Safe left: " + safeLeft + " front: " + safeFront + 
				   " right: " + safeRight + "back: " + safeBack);
		System.out.println("Open left: " + openLeft + " front: " + openFront + 
				   " right: " + openRight + "back: " + openBack);
	    }
	    obstacle = !safeFront;		
	}
    }


    //Returns the shortest distance from a point (cartesian coordinates 
    //with origin at robot's center of rotation) to the robot, as dictated by the coordinates specified
    //in the coordinate configuration file.
    private double shortestDistance(Point2D.Double p){
	Point2D.Double v;
	Point2D.Double w;
	double shortestCandidate = 999999999; //arbitrarily large number.
	for (int segment = 0; segment < robotCorners.size() - 1; segment++){
	    v = (Point2D.Double)robotCorners.get(segment);
	    w = (Point2D.Double)robotCorners.get((segment + 1) % robotCorners.size()); //so the last iteration can wrap around.
	    shortestCandidate = Math.min(shortestCandidate, Line2D.ptSegDist(v.getX(), v.getY(),
									      w.getX(), w.getY(),
									      p.getX(), p.getY()));
	}
	return shortestCandidate;
    }
	    
    private void readCoordinates(String file){
	if (file == "") return;
	Scanner s = null;	
	try{
	    double keep = 0;
	    s = new Scanner(new BufferedReader(new FileReader(file)));
	    boolean even = true;
	    while (s.hasNext()){
		if(even){
		    keep = Double.parseDouble(s.next());
		}
		else{
		    Point2D.Double p = new Point2D.Double(keep, Double.parseDouble(s.next()));
		    robotCorners.add(p);
		}
		even = !even;
	    }	    
	    coordinatesRead = true;	    
	}
	catch(Exception e){
	    System.err.println("****************\n");
	    System.err.println("I take Exception to that!\n"+e+"\n");
	    System.err.println("****************\n");
	}finally{
	    s.close();
	}
    }

    private synchronized void publish(){
	laser_t msg = new laser_t();
	
	msg.timestamp = System.nanoTime()/1000;
	//	msg.start_angle =  (float)((scanAngle/2 + Math.PI + offset) % (2*Math.PI));
	msg.start_angle = -2.35619f;
	msg.angle_increment =  (float)(scanAngle/numReadings);
	msg.num_ranges = (short)numReadings;
	msg.ranges = readingsF;
	msg.max_range = 8.0f;
	msg.offset = new robot_pose_t();
	msg.offset.x = (float)mountY;
	msg.offset.y = -1*(float)mountX;
	msg.offset.theta = (float)offset;
	System.out.println(prg+": publishing! "+readingsF[700]);
	lcm.publish("SENSOR_FRONT_LASER", msg);


	/*	double start_angle =
	double angle_increment =
	short num_ranges = (short) numReadings;
	double[] ranges = new double[numReadings];
	System.arraycopy(readings, 0, ranges, 0,  numReadings);
	double max_range = scanAngle;
		try{
		call(lrfpub, "publishReadings", start_angle, angle_increment, num_ranges, ranges, max_range, mountX, mountY, offset);
	}catch(ADEException e){
	    System.out.println("Problem publishing Readings: ");
	    e.printStackTrace();
	    }*/
	
    }

    /**
     * * Get latest readings from laser and update local data structures.
     * Computes "open space area" and whether the area is safe.
     * Integrates history and current readings to determine safety.
     * @param right_beg - beginning index into readings for right-region (inclusive)
     * @param right_end - ending index into readings for right-region (inclusive)
     * @param front_beg - beginning index into readings for front-region (inclusive)
     * @param front_end  - ending index into readings for front-region (inclusive)
     * @param left_beg - beginning index into readings for left-region (inclusive)
     * @param left_end - ending index into readings for left-region (inclusive)
     * @param back_beg - beginning index into readings for left-region (inclusive)
     * @param back_end - ending index into readings for left-region (inclusive)
     */
    protected void updateLRF(int right_beg, int right_end,
            int front_beg, int front_end,
			     int left_beg, int left_end, int back_beg, int back_end) {
	if(!finishedLoading)
	    return;
	//	System.out.println("DBG: update call");
	scanID++;
	// MS: added logging here -- there is some overhead computing the string every time that can be eliminated
	StringBuffer result = new StringBuffer();
	if (readings.length > 0) {
	    result.append(readings[0]);
	    if(publishReadings)
		readingsF[0]=(float)readings[0];
	    for (int i=1; i<readings.length; i++) {
		result.append(",");
		result.append(readings[i]);
		if(publishReadings)
		    readingsF[i]=(float)readings[i];
	    }
	}
	canLogIt(result);

	if (coordinatesRead)
	    calculateDistancesToBot(right_beg, right_end, front_beg, front_end, left_beg, left_end, back_beg, back_end, 0.0);
	    else{
		//local params
		double d;
		boolean front = true;
		boolean right = true;
		boolean left = true;
		boolean back = true;
		double spaceFront = 0;
		double spaceLeft = 0;
		double spaceRight = 0;
		double spaceBack = 0;
		
		// Right sector
		for (int i = right_beg; i <= right_end; i++) {
		    if(i<cutoff){
			d = readings[i];
			if (d != badRangeVal) {
			    spaceRight += d;
			    // history requires a < CRIT reading MINHIST times before it's a hit
			    if (d < CRITICAL_DIST) {
				history[i] = Math.min(MINHIST, history[i] + 1);
			    } else {
				history[i] = Math.max(0, history[i] - 1);
			    }
			    right = right && (history[i] < MINHIST);
			    //if (i >= onethird) {
			    //    openRight = openRight && (d > CRITICAL_DIST * 1.25);
			    //}
			}
		    }
		}
		
		// Left sector
		for (int i = left_beg; i <= left_end; i++) {
		    if(i<cutoff){
			d = readings[i];
			if (d != badRangeVal) {
			    spaceLeft += d;
			    if (d < CRITICAL_DIST) {
				history[i] = Math.min(MINHIST, history[i] + 1);
			    } else {
				history[i] = Math.max(0, history[i] - 1);
			    }
			    left = left && (history[i] < MINHIST);
			    //openLeft = openLeft && (d > CRITICAL_DIST * 1.25);
			}
		    }
		    
		}
		
		// Front sector
		for (int i = front_beg; i <= front_end; i++) {
		    if(i<cutoff){
			d = readings[i];
			if (d != badRangeVal) {
			    spaceFront += d;
			    if (d < CRITICAL_DIST) {
				history[i] = Math.min(MINHIST, history[i] + 1);
			    } else {
				history[i] = Math.max(0, history[i] - 1);
			    }
			    front = front && (history[i] < MINHIST);
			    //openFront = openFront && (d > CRITICAL_DIST * 1.25);
			}
		    }
		}
		
		for (int i = back_beg; i <= back_end; i++) {
		    if(i<cutoff){
			d = readings[i];
			if (d != badRangeVal) {
			    spaceBack += d;
			    if (d < CRITICAL_DIST) {
				history[i] = Math.min(MINHIST, history[i] + 1);
			    } else {
				history[i] = Math.max(0, history[i] - 1);
			    }
			    back = back && (history[i] < MINHIST);
			    //openBack = openBack && (d > CRITICAL_DIST * 1.25);
			}
		    }
		}
		
		safeRight = right;
		safeLeft = left;
		safeFront = front;
		safeBack = back;
		
		openRight = right && (spaceRight > MINOPEN);
		openLeft = left && (spaceLeft > MINOPEN);
		openFront = front && (spaceFront > MINOPEN);
		openBack = back && (spaceBack > MINOPEN);
		
		if (false && (!safeLeft || !safeRight || !safeFront/* || !safeBack*/)) {
		    System.out.print(prg + ": COLLISION WARNING: ");
		    if (!safeLeft) {
			System.out.print("left ");
		    }
		    if (!safeFront) {
			System.out.print("front ");
		    }
		    if (!safeRight) {
			System.out.print("right ");
		    }   
		    /*
		    if (!safeBack) {
			System.out.print("back ");
			}*/
		    System.out.println("");
        }
		if (verbose) {
		    System.out.println("Safe left: " + safeLeft + " front: " + safeFront + " right: " + safeRight + " back: " + safeBack);
		    System.out.println("Open left: " + openLeft + " front: " + openFront + " right: " + openRight + " back: " + openBack);
		}
		//System.out.println("left: " + safeLeft + " front: " + safeFront + " right: " + safeRight);
		/*	if (coordinatesRead){
			try{
			double safespeed = getSafeSpeed();
			System.out.println(smallestLaserReadingAt(0.0)); 
			//DBG
			}
			catch(Exception e){
			System.err.println("!!!: "+e);
			}
			}*/
		obstacle = !safeFront;
		//if (obstacle)
		//    System.out.println("safeFront: " + safeFront);
	    }
        if(debugGetLaserScan)
            try{
		        System.out.println(this.getLaserScan().ranges[100]);
            }catch(Exception e){
		        System.out.println("COULD NOT GET LASER SCAN D:");
	        }
    }

    @Override
	public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
        ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
        specs.add("Lasers", LRFComponentVis.class);
        return specs;
    }
    
    /**
     * Provide additional information for usage...
     */
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -port <port name>          <override default (" + portName + ")>\n");
        sb.append("  -critical <dist>           <override default critical dist>\n");
        sb.append("  -debug-getlaserscan        <getlaserscan debugging>\n");
        sb.append("  -verbose                   <verbose output>\n");
        sb.append("  -vverbose                  <very verbose output>\n");
	sb.append("  -offset                    <laser angles offset by specified number of radians>\n");
	sb.append("  -flipped                   <lrf is in a flipped position>\n");
	sb.append("\n");
	sb.append("  -coordinates <file>        <specifies a file holding the coordinates of the robot>\n");
	sb.append("  -radius <r>                <specifies radius of these coordinates>\n");
	sb.append("  -criticalspeed <speed>    <specifies the absolute fastest the robot should ever go>\n ");
        return sb.toString();
    }

    /** 
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise 
     */
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-port") && (++i < args.length)) {
                portName = args[i];
                userPort = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-critical")&& (++i < args.length)) {
                double crit;
                try {
                    crit = Double.parseDouble(args[i]);
		    //                    i++;
                    CRITICAL_DIST = crit;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": crit " + args[i]);
                    System.err.println(prg + ": " + nfe);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-criticalspeed") && (++i < args.length)) {
		criticalspeed = Double.parseDouble(args[i]);
                found = true;
            } else if (args[i].equalsIgnoreCase("-cutoff") && (++i < args.length)) {
		cutoff= Integer.parseInt(args[i]);
		cutoffFound=true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-mountX")&& (++i < args.length)) {
                mountX= Double.parseDouble(args[i]);
                mountingPoint.setLocation(mountX, mountY);		
                found = true;
            } else if (args[i].equalsIgnoreCase("-mountY")&& (++i < args.length)) {
                mountY = Double.parseDouble(args[i]);
                mountingPoint.setLocation(mountX, mountY);		
                found = true;
            } else if (args[i].equalsIgnoreCase("-radius")&& (++i < args.length)) {
                robotRadius = Double.parseDouble(args[i]);
                found = true;
            } else if (args[i].equalsIgnoreCase("-publish")) {
                publishReadings = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-verbose")) {
                verbose = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-vverbose")) {
                verbose = true;
                vverbose = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-debug-getlaserscan")) {
                debugGetLaserScan = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-flipped")){
                flipped = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-offset") && (++i < args.length)) {
                offset = Double.parseDouble(args[i]);
                found = true;
            } else if (args[i].equalsIgnoreCase("-coordinates") && (++i < args.length)) {
                readCoordinates(args[i]);
                found = true;
            } else if (args[i].equals("")) {
                // subclass picked this one off
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }

        return found;
    }
    
    /**
     * Log a message using ADE Component Logging, if possible.  The ASL facility
     * takes care of adding timestamp, etc.
     * @param o the message to be logged
     */
    protected void logItASL(Object o) {
        canLogIt(o);
    }

    /**
     * Set the state of ADE Component Logging.  When true and logging is not
     * started, this starts logging.  When false and logging is started, this
     * stops logging and causes the log files to be written to disk.  ADE server
     * logging is a global logging facility, so starting logging here enables
     * logging in a currently instantiated ADE servers.  Note: You want to stop
     * ADE server logging before quitting, or the files will not be complete.
     * @param state indicates whether to start (true) or stop (false) logging.
     */
    protected void setASL(boolean state) {
        try {
            setADEComponentLogging(state);
        } catch (Exception e) {
            System.out.println("setASL: " + e);
        }
    }

    @Override
    protected void updateFromLog(String logEntry) {
	System.out.println("----------------------------------->>>>>");
    }

    /**
     * Shut down the LRF.
     */
    abstract public void shutdown();
    /**
     * <code>main</code> passes the arguments up to the ADEComponentImpl
     * parent.  The parent does some magic and gets the system going.
     *
    public static void main(String[] args) throws Exception {
    ADEComponentImpl.main(args);
    }
     */
}
