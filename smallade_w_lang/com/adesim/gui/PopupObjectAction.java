package com.adesim.gui;

import java.io.Serializable;

public class PopupObjectAction implements Serializable {
	private static final long serialVersionUID = 1L;
	public String actionNameForPopup;
	public String methodName;
	public Object[] args;
	
	public PopupObjectAction(String actionNameForPopup,
							 String methodName, Object... args) {
		this.actionNameForPopup = actionNameForPopup;
		this.methodName = methodName;
		this.args = args;
	}
}
