/*********************************************************************
 * 
 * SWAGES 1.1
 * 
 * (c) by Matthias Scheutz
 * 
 * Common Lisp symbol representation
 * 
 * Last modified: 05-12-07
 * 
 ********************************************************************/

package utilities.comlink;

import java.io.Serializable;

public class CLSymbol extends Word implements Serializable {
    public CLSymbol(String s) {
	super(s);
    }
}
