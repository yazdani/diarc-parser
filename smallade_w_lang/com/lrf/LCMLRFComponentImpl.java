/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Tom Williams
 *
 * Copyright 1997-2013 Tom Williams and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact Tom Williams at Thomas_E.Williams@Tufts.edu.
 */

package com.lrf;

import java.io.*;
import java.rmi.*;

import com.LaserScan;
import lcm.lcm.*;
import vulcan_lcm.*;
/**
 *The LCM LRF Component interface
 */
public class LCMLRFComponentImpl extends LRFComponentImpl implements LCMLRFComponent{

    private boolean localServicesReady = false;
    private static String lcmport = "/dev/ttyACM0";
    private static String pathToVulcan = "default";
    private String laserChannel = "SENSOR_FRONT_LASER_INTENSITY";
    private Process utmProc;

    public LCMLRFComponentImpl() throws RemoteException{
	super(270*4+1, Math.toRadians(270.0));
	readings=new double[numReadings];
	System.out.println("Starting LCM'd UtmLRFComponent");
	try{
	    UTMSubscriber utmsub = new UTMSubscriber();
	}catch(IOException ioe){
	    System.out.println("Couldn't create subscriber!");
	    ioe.printStackTrace();
	}
	//	System.out.println("LCMLRF Subscriber successfully created");
	try{Thread.sleep(1000);}catch(Exception e){System.out.println("Insomnia");}
	localServicesReady=true;	
    }	
    protected boolean localServicesReady(){
	return localServicesReady;
    }

    @Override
    public LaserScan getLaserScan() throws RemoteException {
        // TODO
        return null;
    }

    public class UTMSubscriber implements LCMSubscriber{
	LCM lcm;
	public UTMSubscriber() throws IOException{
	    try{
		lcm=LCM.getSingleton();
		lcm.subscribe(laserChannel, this);
		if(pathToVulcan.equals("default"))
		    pathToVulcan="../../Vulcan";
		utmProc = Runtime.getRuntime().exec("sudo "+pathToVulcan+"/build/bin/laser_scan_producer --config-file "+pathToVulcan+"/build/bin/front_laser_scan_producer.cfg" );
		final InputStream utm_in = utmProc.getInputStream();
		final InputStream utm_err = utmProc.getErrorStream();
		gobbleStream("UTM_IN",  utm_in);
		gobbleStream("UTM_ERR", utm_err);
	    }catch(Exception e){
		System.out.println("Problem in UTMSubscriber: "+e);
	    }
	}

	public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins){
	    try{
		if(localServicesReady){
		    if(channel.equals(laserChannel)){
			laser_with_intensity_t msg_a = new laser_with_intensity_t(ins);
			laser_t msg = (laser_t)msg_a.laser;
			for(int i = 0; i < msg.num_ranges; i++)
			    readings[i]=(double)msg.ranges[i];
			if(verbose)
			    System.out.println("Reading 500: "+readings[500]);
			updateLRF(0, 540, 541, 900, 901, 1080, 0, -1);
		    // System.out.println("Reading 300: "+readings[300]);
		    }
		}else 
		    if(verbose)
			System.out.println("Not yet ready to deal with incoming message...");
	    }catch(IOException ex){System.out.println("Exception: "+ex);}
	}	
    }
    
    private void gobbleStream(String name, InputStream instream){
	final InputStream stream = instream;
	new Thread(name){
	    @Override
		public void run(){
		try{
		    InputStreamReader isr = new InputStreamReader(stream);
		    BufferedReader br = new BufferedReader(isr);
		    while(true)			
			br.readLine();
		}catch(Exception e){
		    System.out.println("Probbles with the Gobbles!");
		    e.printStackTrace();}
		}
	}.start();
    }

    protected boolean parseadditionalargs(String[]args){
        boolean found = false;
	String[] otherArgs = new String[args.length];
	int j=0;
        for (int i = 0; i < args.length; i++) {
	    if (args[i].equalsIgnoreCase("-path") &&
		(args[++i]!=null)) {
                pathToVulcan = args[i];
                found = true;
	    }
	    else if (args[i].equalsIgnoreCase("-lcmport") &&
		(args[++i]!=null)) {
                lcmport = args[i];
                found = true;
	    }
	    else if (args[i].equalsIgnoreCase("-verbose")){
		verbose=true;
                found = true;
	    }
	    else otherArgs[j++]=args[i];	    
	}       
	return (super.parseadditionalargs(otherArgs));
    }

    public void updateComponent(){

    }
    public void shutdown(){
	System.out.println("Shutting down LCMLRF");
	utmProc.destroy();
    }
}