package utilities;

/** Simple data structure to hold generic pairs of objects */
public class Pair<Type1, Type2> {
	public Type1 item1;
	public Type2 item2;
	
	public Pair(Type1 item1, Type2 item2) {
		this.item1 = item1;
		this.item2 = item2;
	}
	
	/** a factory-method for creating a pair, with a bit less typing */
	public Pair<Type1, Type2> create(Type1 item1, Type2 item2) {
		return new Pair<Type1, Type2>(item1, item2);
	}
	
	
	@Override
	/** override equal so that will include only unique items in a hashset/hashmap.
	 * note that hashCode must likewise be overriden (it is below) */
	public boolean equals(Object obj) {
		@SuppressWarnings("unchecked")
		Pair<Type1, Type2> otherPair = (Pair<Type1, Type2>) obj;
		return (item1.equals(otherPair.item1) &&
				item2.equals(otherPair.item2));
	}
	
	@Override
	/** overridden so that can be used in hashsets/hashmaps.  The numbers themselves are 
	 * fairly arbitrary, just want something that is unlikely to overlap if not the same... */
	public int hashCode() {
		if (item1 == null) {
			return (item2 == null) ? 0 : item2.hashCode() + 1;
		} else if (item2 == null) {
			return item1.hashCode() + 2;
		} else {
			return item1.hashCode() * 17 + item2.hashCode();
		}
	}
	

}