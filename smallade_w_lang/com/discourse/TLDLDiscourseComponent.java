/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * TLDLDiscourseComponent.java
 *
 * @author Paul Schermerhorn, Juraj Dzifcak
 *
 */
package com.discourse;

/**
 * TLDLDiscourseComponent uses the a CCG parser with a strict dictionary in order to 
 * produce semantic representations of input utterances.
 */
public interface TLDLDiscourseComponent extends com.interfaces.NLPComponent, 
       com.interfaces.SpeechProductionComponent {
}
