package com.adesim.commands;

import java.io.Serializable;

import com.adesim.objects.model.SimModel;


/** An interface for commands that the environment will issue to actors 
 * Must be serializable so can pass through RMI*/
public interface ActorCommand extends Serializable{
	void execute(SimModel model);
}
