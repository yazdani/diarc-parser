package com.adesim;

import com.adesim.commands.ActorCommand;
import com.adesim.commands.ActorCommandsHolder;
import com.adesim.commands.HistoryHolder;
import com.adesim.commands.HistoryHolder.UpdateData;

/** A class responsible for sending updates from the environment to the actors,
 * either immediately or after each tick. */
public class UpdateManager {
	private ActorCommandsHolder currentTickCommandHolder = new ActorCommandsHolder();
	private HistoryHolder historyHolder = new HistoryHolder();
	
	private ADESimEnvironmentComponentImpl envComponent;
	private boolean groupUpdates; // flag for whether to send updates all in a group, on a tick
	//    or to dispatch them immediately.
	
	public UpdateManager(ADESimEnvironmentComponentImpl envComponent, boolean groupUpdates) {
		this.envComponent = envComponent;
		this.groupUpdates = groupUpdates;
	}
	
	public void addCommand(ActorCommand command) {
		currentTickCommandHolder.add(command);
		if (!groupUpdates) {
			envComponent.sendImmediateUpdate(command);
		}
	}
	public void addCommand(String actorName, ActorCommand command) {
		currentTickCommandHolder.add(actorName, command);
		if (!groupUpdates) {
			envComponent.sendImmediateUpdate(actorName, command);
		}
	}

	public ActorCommandsHolder offloadAccumulatedCommandsIfDispatchingUpdatesInGroup(int tickCounter) {
		final ActorCommandsHolder accumulatedCommandHolder = currentTickCommandHolder.offloadAccumulatedCommands();
		historyHolder.add(tickCounter, accumulatedCommandHolder.getGeneralCommands());
		if (groupUpdates) {
			return accumulatedCommandHolder;
		} else {
			// return an empty one:
			return new ActorCommandsHolder();
		}
	}

	public UpdateData getUpdateHistory(int lastUpdateTickCount) {
		return historyHolder.getUpdateHistory(lastUpdateTickCount);
	}

}
