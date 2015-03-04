/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * DefaultActionComponentArch.java
 *
 * Last update: April 2010
 *
 * @author Paul Schermerhorn
 *
 */
package com.action;

import ade.*;

/** 
 * <code>DefaultActionComponentArch</code> implements the ActionComponent's
 * runArchitecture method, and whatever supporting code is necessary, for an
 * architecture specified in Java, rather than Action scripts.  This is the
 * one that's automatically chosen by {@link ade.BootstrapADE BootstrapADE}
 * if another isn't passed as an argument.
 */
public interface DefaultActionComponentArch extends ADEComponent {
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
