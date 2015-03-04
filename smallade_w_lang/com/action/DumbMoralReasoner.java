/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * DumbMoralReasoner.java
 *
 * @author Paul Schermerhorn
 */
package com.action;

import ade.*;
import com.*;
import java.rmi.*;

public interface DumbMoralReasoner extends ADEComponent {
    public Boolean askForOverride(Predicate conflict, Predicate goal) throws RemoteException;

}
// vi:ai:smarttab:expandtab:ts=8 sw=4
