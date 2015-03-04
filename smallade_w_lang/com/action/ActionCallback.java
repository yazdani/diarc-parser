package com.action;

import java.io.Serializable;

public class ActionCallback implements Runnable,/* Remote,*/ Serializable {
    @Override
    public void run() {
        System.out.println("Running runme.");
    }
}
