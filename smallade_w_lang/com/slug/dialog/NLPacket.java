package com.slug.dialog;

import com.slug.nlp.*;
import java.io.Serializable;

public class NLPacket implements Comparable<NLPacket>, Serializable {

	private static int nextID = 0;

	private int id;
	private int priority; 
  	private long timeRequested;
	private Utterance utterance;

 	///////////////////// constructor ///////////////////
 	public NLPacket() {
		this.priority = 0;
		//this.id = NLPacket.getNextId();
		this.id = this.hashCode();
		markCurrentTime();
	}

	public NLPacket(Utterance utt) {
		this.priority = 0;
		utterance = utt;
		//this.id = NLPacket.getNextId();
		this.id = this.hashCode();
		markCurrentTime();
		//System.out.println("NLPacket constructor " + utt + " : " + this.id);
	}

	public NLPacket(int priority, Utterance utt) {
		this.priority = priority;
		utterance = utt;
		//this.id = NLPacket.getNextId();
		this.id = this.hashCode();
		markCurrentTime();
		//System.out.println("NLPacket constructor " + utt + " : " + this.id);
	}

	//////////////////////////////////////////////////////

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority; 
	}

	public long getTimeRequested() {
		return timeRequested;
	}

	public Utterance getUtterance() {
		return utterance;
    	}
	
	public void setUtterance(Utterance utt) {
		utterance = utt;
	} 

	public void markCurrentTime() {
		timeRequested = System.currentTimeMillis();
	}

	public int getId() {
		return id;
	}

	public synchronized static int getNextId() {
		int next = NLPacket.nextID;
		NLPacket.nextID++;
		return next;
	}

	///////////////////////////////////////////////////
	
	public String toString() {
	  	String str = "nlp(" + id + "," + utterance.type  + "," + utterance.semantics + ")";
		return str;
	}

	///////////////////////////////////////////////////

	public int compareTo(NLPacket other) {
 		if(this.getPriority() > other.getPriority()) return -1;
		else if(this.getPriority() < other.getPriority()) return 1; 
		else {
			return -(new Long(this.timeRequested).compareTo(new Long(other.getTimeRequested())));
   		} 
        }

}
