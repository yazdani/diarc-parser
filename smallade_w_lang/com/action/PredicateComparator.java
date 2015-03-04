package com.action;

import java.util.Comparator;
import java.util.BitSet;
import com.Predicate;
import utilities.Util; 

public class PredicateComparator implements Comparator<Predicate> {

	@Override
	public int compare(Predicate p1, Predicate p2) {
		String p1name = p1.getName();
		String p2name = p2.getName(); 
		if(p1name.startsWith("procedure") && !p2name.startsWith("procedure")) {
			return 1;
		} else if(!p1name.startsWith("procedure") && p2name.startsWith("procedure")) {
			return -1;
		}
		return 0;
	}

	// main method -- for testing	
	public static void main(String[] args) {
	    	Predicate p1 = Util.functionPredicate("type(a,b)");
	    	Predicate p2 = Util.functionPredicate("procedure(c,d)");
		PredicateComparator pc = new PredicateComparator();		

		System.out.println("p1 = "  + p1);
		System.out.println("p2 = "  + p2);
		System.out.println("compare(p1,p2) = " + pc.compare(p2,p1));

        }
}
