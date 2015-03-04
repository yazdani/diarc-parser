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

// convenience class for CL lists
import java.util.ArrayList;
import java.io.Serializable;

public class CLList<E> extends ArrayList<E> implements Serializable {
    public CLList(int l) {
	super(l);
    }
    public CLList() {
        super();
    }
}
