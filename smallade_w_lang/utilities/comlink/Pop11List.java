/*********************************************************************
 * 
 * SWAGES 1.1
 * 
 * (c) by Matthias Scheutz
 * 
 * Common Lisp list representation
 * 
 * Last modified: 05-12-07
 * 
 ********************************************************************/

package utilities.comlink;

// convenience class for pop11 lists
import java.util.ArrayList;
import java.io.Serializable;

public class Pop11List<E> extends ArrayList<E> {
    public Pop11List(int l) {
	super(l);
    }
    public Pop11List() {
        super();
    }
}
 
