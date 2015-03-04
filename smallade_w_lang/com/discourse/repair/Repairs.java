/**
 * ADE 1.0 
 * Copyright 1997-2010 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * 
 *
 * @author Rehj Cantrell
 */
package com.discourse.repair;

import utilities.Util;

import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.ArrayList;

/** A repair object does some regular expression repair on strings. More complex repairs are done within mink. **/
public class Repairs {
	/** Whether or not to remove disfluencies (true), or just mark them (false). */
	static Boolean remove = false;
	/** length of maximum repetition to remove */
	static int maxRepLength = 3;

	// 11/6/09 -----------------------------------------------------------------------------------------
	// MS: added repairs on word lists -- this will destructively change the list passed in as an argument
	public static void repair(ArrayList<String> in) {
		for (int j=0; j<in.size(); j++)
			//	    System.out.println("in repair, word " + j + " " + in.get(j));

			removeSpecTokens(in);
		removeSimpleRepairs(in);
		if (in.size()==0)
			return;
		// converting to string because it's easier to remove multiword expressions that way
		String words = in.get(0);
		for(int i=1;i<in.size();i++)
			words = words + " " + in.get(i);
		words.replace("you'll","you will");
		//String[] out = removeReps(removePhrases(words)).split(" ");
		String[] out = words.split(" ");
		// converting back to array list
		in.clear();
		for(int i=0;i<out.length;i++)
			if (out[i].length()>0)
				in.add(out[i]);
	}

	/** Removes specific problem words such as "um" **/
	public static void removeSpecTokens(ArrayList<String> in) {
		// MS: added beginning vs rest of phrases
		// remove tokens at the beginning phrases
		Boolean firstremoved = false;
		if (in.size() > 0) {
			String word = wordRemover(in.get(0),true,true);
			//String word=in.get(0);
			//	    System.out.println("in repair 1 word = " + word);
			if (word.equals("")) {
				in.remove(0);
				firstremoved=true;
			}
			else
				in.set(0,word);
		}
		int i;
		if (firstremoved)
			i = 0;
		else
			i = 1;
		while (in.size()>i) {
			String word = wordRemover(in.get(i),false,true);
			//	    System.out.println("in repair 2 word = " + word);
			//	    String word=in.get(i);
			if (word.equals("")) {
				in.remove(i);
				System.out.println("Removed");
			}
			else {
				in.set(i,word);
				i++;
			}
		}
	}

	/** Removes one-word repairs **/
	static void removeSimpleRepairs(ArrayList<String> in) {
		String [] markers = {"no","wait","now"};
		String [] colorset = {"red","green","blue"};

		for (int n=1; n<in.size()-1; n++) {
			if (isMemberOf(markers, in.get(n)) && isMemberOf(colorset,in.get(n-1)) && isMemberOf(colorset,in.get(n+1))) {
				in.remove(n-1);
				in.remove(n-1); // this will remove the strings at n-1 and at n
			}
		}
	}

	//-----------------------------------------------------------------------------------------------

	public static String repair(String in) {
		String temp = regEx(in);
		return temp;
	}

	/** This uses purely regex to do some recovery **/
	public static String regEx(String sen) {
		String out = "";
		out = removeSpecTokens(sen);
		// MS: 04/25/09 added this in
		out = removeSimpleRepairs(out);
		out = removePhrases(out);
		out = removeReps(out);
		return out;
	}

	public static void main(String args[]) {
		//	String sen = "There should be like a wooden box";
		handleArgs(args);
		inputManager();
		//	repair(sen);
	}

	/** This is basically a version of main that can be called by another Java class **/
	public static void makeRepairs(Boolean t, Boolean r) {
		String[] args = {t==true?"-t":"", r==true?"-r":""};
		handleArgs(args);
		inputManager();
	}

	/** parse arguments. may alter the arguments by either adding args 
		found in a command line or emptying arguments that are not used again
		@param args arguments, presumably command-line
		@return possibly altered arguments */
	public static String [] handleArgs(String [] args) {
		for (int n=0; n<args.length; n++) {
			if (args[n].equalsIgnoreCase("-r")) {
				args[n] = "";
				remove=true;
			}
		}
		return args;
	}

	/** This opens up the input stream and starts streaming input, line by line, to the repair functions **/
	public static void inputManager() {
		Scanner stdin = new Scanner( System.in );
		stdin.useDelimiter("\\n");
		String input = "";

		while (stdin.hasNext()) {
			input = stdin.next();
			//	    System.out.println("Orig: " + input);
			input = removeSpecTokens(input);
			input = removeSimpleRepairs(input);
			input = removePhrases(input);
			input = removeReps(input);
			if (input.length()>0) {
				System.out.println(input);
			} else {
				System.out.println("");//No input left!");
			}
		}
	}

	/** Removes specific problem words such as ``um'' **/
	public static String removeSpecTokens(String in) {
		StringTokenizer st = new StringTokenizer(in);
		String output = "";
		// MS: added beginning vs rest of phrases
		// remove tokens at the beginning phrases
		if (st.hasMoreTokens()) {
			output += wordRemover(st.nextToken(),true);	  
		}
		while (st.hasMoreTokens()) {
			output += wordRemover(st.nextToken(),false);
		}
		return output;      
	}

	public static String getType(String word,boolean arraylist) {
		if (!word.matches("a") && word.matches("([ouae]*[mh]*)|er")) {
			return remove?"":"NLFP ";
		} else if (word.matches("[A-Za-z]+-")) {
			return remove?"":"ABW ";
		} else if (word.matches("well")) {
			return remove?"":"LFP ";
		} else if (word.matches("good")) {
			return remove?"":"LFP ";
		} else if (word.matches("okay")) {
			return remove?"":"LFP ";
		} else if (word.matches("now")) {
			return remove?"":"LFP ";
		} else if (word.matches("so")) {
			return remove?"":"LFP ";
		} else if (word.matches("maybe")) {
			return remove?"":"QUAL ";
		} else if (word.matches("first")) {
			return remove?"":"COR ";
		} else {
			return (arraylist? word + " ": word);
		}
	}

	/** Used by removeSpecTokens (above) **/
	static String wordRemover(String word,boolean first) {
		return wordRemover(word,first,true);//false);
	}

	// MS: added an argument to make to be able to use this for the new arraylist filter, so that the final space
	// does not get appended to words
	private static String wordRemover(String word,boolean first,boolean arraylist) {
		// for the first word only
		if (first) {
			return getType(word,arraylist);
		}
		// for words starting with the second position
		else {
			return getType(word,arraylist);
		}
	}

	/** Removes one-word repairs **/
	static String removeSimpleRepairs(String sentence) {
		StringTokenizer st = new StringTokenizer(sentence);
		String [] tokens = new String [st.countTokens()];
		String output = "";

		String [] markers = {"no","wait","now"};
		String [] colorset = {"red","green","blue"};

		for (int n=0; n<tokens.length; n++) {
			tokens[n] = st.nextToken();
		}


		for (int n=1; n<tokens.length-1; n++) {
			if (isMemberOf(markers, tokens[n]) && isMemberOf(colorset,tokens[n-1]) && isMemberOf(colorset,tokens[n+1])) {
				tokens[n-1]="";
				tokens[n]="";
			}
		}

		for (int n=0; n<tokens.length;n++) {
			output = output + " " + tokens[n];
		}

		return output;
	}

	/** This removes things like ``go ahead and'', ``and everything'', ``kind of like'', etc. **/
	static String removePhrases(String Utterance) {
		//If you modify phrases, make sure that you modify replace if necessary!!
		String[] phrases = {"go ahead and ", "and everything ", "kind of like ", "[Ii] guess ", "[Ii]'d ", "go and ", "just ([^i][^n])"};
		String[] replace = {"", "", "", "", "", "", "$1"};

		for (int n=0; n < phrases.length; n++) {
			Utterance = Utterance.replaceAll(phrases[n],replace[n]);
		}

		return Utterance;
	}

	/** This removes repetitions **/
	public static String removeReps(String Utterance) {
		ArrayList<String> repaired = Util.getArrayList(Utterance);

		for (int repLength=1; repLength<maxRepLength; repLength++) {
repLoop:
			for (int startPt=0; startPt+repLength<repaired.size(); startPt++) {
				if (repaired.get(startPt).equalsIgnoreCase(repaired.get(startPt+repLength))) {
					for (int diff=1; diff<repLength && startPt+repLength+diff < repaired.size(); diff++) {
						if (!repaired.get(startPt+diff).equalsIgnoreCase(repaired.get(startPt+repLength+diff))) {
							continue repLoop;
						}
					}
					for (int i=repLength-1; i>=0; i--) {
						repaired.remove(startPt+i);
					}
				}
			}
		}

		String repairedString = "";
		for (int i=0; i<repaired.size(); i++) {
			repairedString+=repaired.get(i);
		}
		return repairedString;    
	}

	// Helper Functions

	static Boolean isMemberOf(String [] set,String s) {
		for (int n=0; n<set.length; n++) {
			if (s.equals(set[n]))
				return true;
		}
		return false;
	}
}
