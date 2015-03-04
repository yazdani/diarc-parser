package com.adesim.commands;

import java.util.ArrayList;
import java.util.HashMap;

/** A thread-safe repository for accumulated ActorCommands before they get 
 * flushed between ticks */
public class ActorCommandsHolder {
	private ArrayList<ActorCommand> generalCommands = new ArrayList<ActorCommand>();
	private HashMap<String, ArrayList<ActorCommand>> actorSpecificCommands = new HashMap<String, ArrayList<ActorCommand>>();

	public synchronized void add(ActorCommand command) {
		generalCommands.add(command);
	}
	
	public synchronized void add(String forActor, ActorCommand command) {
		ArrayList<ActorCommand> actorCommandsList;
		if (!actorSpecificCommands.containsKey(forActor)) {
			actorSpecificCommands.put(forActor, new ArrayList<ActorCommand>());
		}
		actorCommandsList = actorSpecificCommands.get(forActor);
		actorCommandsList.add(command);
	}
	

	/** returns an arraylist of actor commands and clears itself */
	public synchronized ActorCommandsHolder offloadAccumulatedCommands() {
		@SuppressWarnings("unchecked")
		ArrayList<ActorCommand> accumulatedGeneralCommands = (ArrayList<ActorCommand>) generalCommands.clone();
		@SuppressWarnings("unchecked")
		HashMap<String, ArrayList<ActorCommand>> accumulatedActorSpecificCommands = 
						(HashMap<String, ArrayList<ActorCommand>>) actorSpecificCommands.clone();
		generalCommands.clear();
		actorSpecificCommands.clear();
		
		ActorCommandsHolder actorCommandsHolderClone = new ActorCommandsHolder();
		actorCommandsHolderClone.generalCommands = accumulatedGeneralCommands;
		actorCommandsHolderClone.actorSpecificCommands = accumulatedActorSpecificCommands;
		
		return actorCommandsHolderClone;
	}

	public ArrayList<ActorCommand> getGeneralCommands() {
		return generalCommands;
	}

	public ArrayList<ActorCommand>  getActorSpecificCommands(String actorName) {
		ArrayList<ActorCommand> arrayListToReturn = actorSpecificCommands.get(actorName);
		// ADE does NOT allow nulls to be passed to calls.  So if it's null, make it into 
		//     an empty array list
		if (arrayListToReturn == null) {
			arrayListToReturn = new ArrayList<ActorCommand>();
		}
		return arrayListToReturn;
	}
	
	
}
