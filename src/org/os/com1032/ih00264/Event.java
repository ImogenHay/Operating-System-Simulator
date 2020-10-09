package org.os.com1032.ih00264;
/**
 * Class for creating an event as an object
 * 
 * Used MMayla's Event class from git-hub Process-Scheduling-Simulator
 */
public class Event {
	
	public static enum eventtype {
		arrived,
		scheduled,
		terminated,
		preempted,
		context;		
	}
	
	public int pID;
	public float time;
	public eventtype type;
	
	public Event(int id,float t,eventtype et){
		pID = id;
		time = t;
		type = et;
	}
}
