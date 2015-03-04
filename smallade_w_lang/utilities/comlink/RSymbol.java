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

public class RSymbol extends Word {
    public RSymbol(String s) {
	super(s);
    }
}
