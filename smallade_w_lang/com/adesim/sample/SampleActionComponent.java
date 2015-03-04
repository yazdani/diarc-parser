package com.adesim.sample;

import java.rmi.RemoteException;

import com.action.ActionComponent;

public interface SampleActionComponent extends ActionComponent {
	public ActionComponentVisData getVisualizationData() throws RemoteException;
}
