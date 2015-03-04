/*********************************************************************
 * 
 * SWAGES 1.1
 * 
 * (c) by Matthias Scheutz
 * 
 * Common word/symbol representation
 * 
 * Last modified: 05-12-07
 * 
 ********************************************************************/

package utilities.comlink;

import java.io.Serializable;

public class Word implements Comparable<String>, Serializable  {
    public String word = null;
    public Object binding = null;
    public boolean backquote = false;

    public Word(String s) {
	word = s;
    }

    public Word(byte[] s) {
	word = new String(s);
    }

    public String toString() {
	return word;
    }
    
    public boolean equals(Object o) {
        return word.equals(o);
    }
    
    public int compareTo(String s) {
	return word.compareTo(s);
    }
}
