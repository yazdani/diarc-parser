///**
// * ADE 1.0
// * Copyright 1997-2010 HRILab (http://hrilab.org/)
// *
// * All rights reserved.  Do not copy and use without permission.
// * For questions contact Matthias Scheutz at mscheutz@indiana.edu
// *
// * Repairs.java
// *
// * @author Rehj Cantrell
// */
//package com.discourse.repair;
//
///** This class repairs (or simply labels) disfluencies. */
//public class Token extends edu.tufts.cs.hrilab.util.unit.Token {
//    String disfluencyType;
//
//    public Token() {
//	super();
//    }
//
//    public Token(String token,String tag) {
//	super(token,tag);
//    }
//
//    public void setDisfluencyType(String disfluencyType) {
//	this.disfluencyType = disfluencyType;
//    }
//
//    public String getDisfluencyType() {
//	return disfluencyType;
//    }
//
//    public String toString(String separator) {
//	return super.toString(separator) + separator + getDisfluencyType();
//    }
//
//    public String toString() {
//	return toString(" ");
//    }
//}