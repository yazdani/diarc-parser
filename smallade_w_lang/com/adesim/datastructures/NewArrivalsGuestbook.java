package com.adesim.datastructures;

import java.util.ArrayList;

/* A thread-safe "guestbook" for newly arrived actors, that want to get added on next "tick" */
public class NewArrivalsGuestbook {
	private ArrayList<String> names = new ArrayList<String>();

	public synchronized void add(String name) {
		names.add(name);
	}

	public ArrayList<String> offloadAccumulatedGuests() {
		@SuppressWarnings("unchecked")
		ArrayList<String> accumulatedGuests = (ArrayList<String>) names.clone();
		names.clear();
		return accumulatedGuests;
	}
	
	
}
