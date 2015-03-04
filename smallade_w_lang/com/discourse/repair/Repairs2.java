///**
// * ADE 1.0
// * Copyright 1997-2010 HRILab (http://hrilab.org/)
// *
// * All rights reserved.  Do not copy and use without permission.
// * For questions contact Matthias Scheutz at mscheutz@indiana.edu
// *
// * Repairs2.java
// *
// * @author Rehj Cantrell
// */
//package com.discourse.repair;
//import edu.tufts.cs.hrilab.util.unit.Token;
//import edu.tufts.cs.hrilab.util.fileio.FileIO;
//import java.util.ArrayList;
//
///** This class repairs (or simply labels) disfluencies. */
//public class Repairs2 {
//    /** Maxumim length of repetitions to look for and filter out. */
//    private static int maxRepLength = 5;
//    /** whether to remove or simple mark disfluencies */
//    private static boolean remove=true;
//
//    /** Manage full repair process
//     *  @param input Input to be repaired
//     *  @return repaired utterance */
//    public static ArrayList<String> repair(ArrayList<String> input) {
//	ArrayList<Token> repaired=new ArrayList<Token>();
//	for (int i=0; i<input.size(); i++) {
//	    repaired.add(new Token(input.get(i)));
//	}
//	repaired = removeRepetition(repaired);
//
//	ArrayList<String> repairedString = new ArrayList<String>();
//	for (int i=0; i<repaired.size(); i++) {
//	    repairedString.add(repaired.get(i).getToken());
//	}
//
//	return repairedString;
//    }
//
//    /** Manage full repair process
//     *  @param input Input to be repaired
//     *  @return repaired utterance */
//    public static ArrayList<Token> repairToken(ArrayList<Token> input) {
//	ArrayList<Token> repaired=input;
//	repaired = removeTagged(repaired);
//	repaired = removeRepetition(repaired);
//	return repaired;
//    }
//
//    /** Repair based on things that have been tagged as disfluencies
//	@param input Input to be repaired
//	@return repaired utterance */
//    public static ArrayList<Token> removeTagged(ArrayList<Token> input) {
//	ArrayList<Token> repaired = new ArrayList<Token>();// [] repaired=input;
//	for (int i=0; i<input.size(); i++) {
//	    if (!input.get(i).getTag().equalsIgnoreCase("UH")
//		&& !input.get(i).getTag().equalsIgnoreCase("XY"))
//		repaired.add(input.get(i));
//	    /*	    else
//	      System.out.println(input.get(i).getToken() + " " + input.get(i).getTag());*/
//	}
//	return repaired;//.toArray(new Token[repaired.size()]);
//    }
//
//    /** Repair based on removing what appear to be disfluent repetitions
//	@param input Input to be repaired
//	@retirm repaired utterance */
//    public static ArrayList<Token> removeRepetition(ArrayList<Token> input) {
//	ArrayList<Token> repaired = input;
//
//	for (int repLength=1; repLength<maxRepLength; repLength++) {
//	    int diff=1;
//	    repLoop:
//	    for (int startPt=0; startPt+repLength+diff<repaired.size(); startPt++) {
//		if (repaired.get(startPt).getToken().equalsIgnoreCase(repaired.get(startPt+repLength).getToken())) {
//		    for (diff=1; diff<repLength; diff++) {
//			if (!repaired.get(startPt+diff).getToken().equalsIgnoreCase(repaired.get(startPt+repLength+diff).getToken())) {
//			    continue repLoop;
//			}
//		    }
//		    for (int i=repLength-1; i>=0; i--) {
//			repaired.remove(startPt+i);
//		    }
//		}
//	    }
//	}
//
//	return repaired;
//    }
//
//    /** parse arguments. may alter the arguments by either adding args
//	found in a command line or emptying arguments that are not used again
//	@param args arguments, presumably command-line
//	@return possibly altered arguments */
//    public static String [] handleArgs(String [] args) {
//	for (int n=0; n<args.length; n++) {
//	    if (args[n].equalsIgnoreCase("-r")) {
//		args[n] = "";
//		remove=true;
//	    }
//	}
//	return args;
//    }
//
//    /** Line by line, read and parse a file */
//    public static void fileRepair(FileIO infile) {
//	if (infile!=null) {
//	    String [] sentence;
//	    sentence = infile.readTokens();
//
//	    ArrayList<Token> sentenceTokens;
//	    while (sentence!=null && sentence.length>0) {
//		sentenceTokens = new ArrayList<Token>();
//		for (int i=0; i<sentence.length-1; i+=2) {
//		    sentenceTokens.add(new Token(sentence[i],sentence[i+1]));
//		}
//		sentenceTokens = Repairs2.repairToken(sentenceTokens);
//
//		for (int i=0; i<sentenceTokens.size(); i++) {
//		    System.out.print(sentenceTokens.get(i).getToken() + " " +
//				     sentenceTokens.get(i).getTag() + " ");
//		}
//		System.out.println("");
//
//		sentence = infile.readTokens();
//	    }
//
//	}
//    }
//
//    public static void main(String [] args) {
//	FileIO inputfile = new FileIO(args[0]);
//
//	Repairs2.fileRepair(inputfile);
//
//	/* ArrayList<Token> input = new ArrayList<Token>();
//
//	for (int i=0; i<args.length-1; i+=2) {
//	input.add(new Token(args[i],args[i+1]));
//	System.out.println(args[i] + "_" + args[i+1]);
//	}
//
//	ArrayList<Token> repaired = Repairs2.repairToken(input);
//	for (int i=0; i<repaired.size(); i++) {
//	    if (repaired.get(i)!=null)
//		System.out.println(repaired.get(i).toString() + " ");
//	    else
//		System.out.println("Problem");
//		}*/
//    }
//}