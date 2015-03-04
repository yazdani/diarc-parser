package com.adesim.commands;

import java.util.ArrayList;
import java.util.HashMap;

public class HistoryHolder {

	public class UpdateData {
		public ArrayList<ArrayList<ActorCommand>> commands;
		public int tickCount;
		
		public UpdateData(int groundTruthLastTickCount) {
			this.tickCount = groundTruthLastTickCount;
		}
	}
	
	
	private static int HISTORY_DEPTH_TO_KEEP = 20; // i.e., 2 second's worth.
	//   at some point, want to purge old histories.  If that's too far for the visualization, will return null
	//   for getUpdateHistory, signifying that the request failed, and that need to send a brand-new copy 
	//   of the model instead.
	
	
	private int groundTruthLastTickCount;
	private HashMap<Integer, ArrayList<ActorCommand>> history = new HashMap<Integer, ArrayList<ActorCommand>>();
	
	public synchronized void add(int tickCounter, ArrayList<ActorCommand> generalCommands) {
		history.put(tickCounter, generalCommands);
		groundTruthLastTickCount = tickCounter;
		
		// purge old history (if it exists.)  since tick count is always incremental, it's really simple:
		//    with each new addition, just delete an old one, if it's old enough (i.e., for first X entries,
		//    don't need to delete anything):
		history.remove(tickCounter - HISTORY_DEPTH_TO_KEEP);
	}

	/** returns an array or array lists of actor commands, or, if the visualization is far too behind
	 * (and hence history can't reach far enough), null.
	 * @param visLastUpdateTickCount
	 */
	public synchronized UpdateData getUpdateHistory(int visLastUpdateTickCount) {
		if (visLastUpdateTickCount == -1) {
			return null; // it's a new visualization, so need to give it the whole model from scratch.
		}
		
		if ((groundTruthLastTickCount - visLastUpdateTickCount) > HISTORY_DEPTH_TO_KEEP) {
			//  Too old, return null.
			return null;
		}
		
		UpdateData result = new UpdateData(groundTruthLastTickCount);
		result.commands = new ArrayList<ArrayList<ActorCommand>>();
		// update from ONE AFTER the one the vis last saw, to the current ground truth (inclusive)
		for (int updateIndex = (visLastUpdateTickCount+1); updateIndex <= groundTruthLastTickCount; updateIndex++) {
			result.commands.add(history.get(updateIndex));
		}
		
		return result;
	}

}
