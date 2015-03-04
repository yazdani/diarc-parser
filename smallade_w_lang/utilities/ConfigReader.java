/**
ConfigReader.java

Provides methods for reading a configuration file.

@author Jim Kramer
*/
package utilities;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class ConfigReader {
	// NOTE: should really change the Vector that is returned by the
	// parseAgeSConfigFile() method to not contain Objects, since we
	// already do the work of figuring out what class we have
	private static boolean debug = false;
	static private String configPackage = "utilities";
	
	// static class; no instantiation allowed
	private ConfigReader() {}
	
	private static BufferedReader getReader(String fn) {
		FileReader fr;
		try {
			fr = new FileReader(fn);
		} catch (FileNotFoundException fnfe) {
			return null;
		}
		BufferedReader br = new BufferedReader(fr);
		return br;
	}
	
	/** Parses a line-by-line configuration file. Line-by-line configuration
	 * files have the following formatting:
	 * <ul><li>Blank lines are ignored</li>
	 * <li>Comments are entire lines that begin with "#" (hashmark)</li>
	 * <li>Each line consists of a <i>key</i> and <i>value(s)</i></li>
	 * <li>Key and values are whitespace-separated</li></ul>
	 * Note that all values must appear on the same line as the key.
	 * @param fn a filename, including a path if necessary
	 * @return a Vector of entries, each of which is a Vector whose first
	 * element is the <i>key</i> and whose subsequent elements are the
	 * associated <i>values</i>. */
	public static Vector parseLinesConfigFile(String fn) {
		BufferedReader br;
		Vector allEntries = new Vector();
		Vector oneEntry;
		String line = "#";
		String[] fields;
		
		if ((br = getReader(fn)) != null) {				
			try {
				while ((line = br.readLine()) != null) {
					// skip comment/blank lines
					if (line.matches("\\s") || line.equals("") || line.startsWith("#"))
						continue;
					// parse the line; change all whitespace to single spaces
					//line = line.replaceAll("//s", " ");
					fields = line.split("\\s");
					oneEntry = new Vector(fields.length);
					for (int i=0; i<fields.length; i++) {
						oneEntry.add(new String(fields[i]));
					}
					allEntries.add(oneEntry);
				}
			} catch (Exception e) {
				System.err.println("Error reading file");
				System.err.println(e);
				return null;
			}
		} else {
			System.out.println("Config file "+ fn +" does not exist");
			return null;
		}
		// just for testing
		if (debug) {
			System.out.println("Config entries:");
			for (int i=0; i<allEntries.size(); i++) {
				oneEntry = (Vector)allEntries.get(i);
				for (int j=0; j<oneEntry.size(); j++) {
					line = (String)oneEntry.get(j);
					System.out.print(line +"# ");
				}
				System.out.println("");
			}
		}
		return allEntries;
	}
	
	/** Parses an AgeS configuration file split up by sections. This method
	 * will use the file name to determine what type of entries are contained
	 * therein by checking if the name of the file (sans extension) is a known
	 * type of entity. More specifically, the file name must end have the
	 * extension <tt>.config</tt> and a lower-case string comparison of the
	 * file name without the extension must match the class name of an object
	 * in the <tt>utilities</tt> directory. See <tt>parseAgesConfigFile(fn,
	 * type)</tt> for more on the internal structure of the file. */
	public static Vector parseAgeSConfigFile(String fn) {
		File f = new File(fn);
		String fullname, etype;
		String[] etypes;
		
		// make sure it exists
		if (!f.isFile()) {
			if (debug) System.err.println("File "+ fn +" not found!");
			return null;
		}
		
		// parse the name into an entity
		fullname = f.getName();
		etype = fullname.substring(0, fullname.lastIndexOf('.'));
		return parseAgeSConfigFile(fn, etype);
	}
	
	/** Parses an AgeS configuration file split up by sections. The format
	 * of this type of AgeS configuration file is specified as:
	 * <ul><li>Each section begins with a descriptive name in brackets.</li>
	 * <li>Each entry in a section appears on its own line as a key/value
	 * pair (either in the form <tt>key value</tt> or <tt>key=value</tt>).</li>
	 * <li>Lines beginning with spaces or "#" (hashmarks) are ignored.</li>
	 * </ul>
	 * @param fn the name of the config file, including a path (if necessary)
	 * @param type the type of entries contained in the file
	 * @return a Vector of objects. */
	public static Vector parseAgeSConfigFile(String fn, String type) {
		// NOTE: should really change the Vector that is returned by the
		// parseAgeSConfigFile() method to not contain Objects, since we
		// already do the work of figuring out what class we have
		File f = new File(fn);
		BufferedReader br;
		Vector entries = null;
		Object entry = null;
		String line = "#";
		String lineSep = System.getProperty("line.separator");
		String[] keyval;
		LinkedList keyvals = new LinkedList();
		boolean newsection=false, haveobject=false;
		
		if (debug) System.out.println("File: "+ fn +"; entity type: "+ type);
		// make sure it's a file, is readable, and is a known type
		if (!f.isFile() || !f.canRead() || (type = confirmType(type)) == null) {
			if (debug) System.err.println("Error with file or type");
			return null;
		}
		
		if ((br = getReader(fn)) != null) {
			try {
				while ((line = br.readLine()) != null) {
					line = line.replaceAll("\\s+", " ");
					if (line.length() == 0 || line.startsWith("#") || line.startsWith(" ")) {
						if (debug) {System.out.println("skipping line="+ line);}
					} else if (line.startsWith("[") && (line.indexOf(']') > 0)) {
						// flush the current data
						if (haveobject) {
							if (fillObject(type, entry, keyvals)) {
								if (entries == null) entries = new Vector();
								entries.add(entry);
								if (debug) System.out.println("Added "+ entry);
							} else {
								if (debug) System.out.println("No entry added");
								entry = null;
							}
						}
						// start a new section
						newsection = true;
						haveobject = false;
						keyvals.clear();
						keyvals.add(createTag(line));
					} else if (newsection) {
						// create an object to fill
						newsection = false;
						if ((entry = createObject(type)) != null) {
							haveobject = true;
							keyval = parseLine(line);
							keyvals.add(keyval);
						}
					} else if (haveobject) {
						if (debug) System.out.println("\thaveobject; line is ["+ line +"]");
						keyval = parseLine(line);
						keyvals.add(keyval);
					}
				}
				// flush the contents of the last object
				if (haveobject) {
					if (fillObject(type, entry, keyvals)) {
						if (entries == null) entries = new Vector();
						entries.add(entry);
						if (debug) System.out.println("Added "+ entry);
					} else {
						if (debug) System.out.println("No entry added");
						entry = null;
					}
				}
			} catch (IOException ioe) {
				System.err.println("Exception reading file "+ fn +":");
				System.err.println(ioe);
			}
		}
		return entries;
	}
	
	private static Object createObject(String type) {
		Class c = null;
		Object entry = null;
		
		if (debug) System.out.println("Creating object of type "+ type +"...");
		try {
			c = Class.forName(configPackage+"."+ type);
		} catch (LinkageError le) {
			if (debug) System.err.println(le);
			return null;
		} catch (ClassNotFoundException cnfe) {
			if (debug) System.err.println(cnfe);
			return null;
		}
		if (debug) System.out.println("\tGot "+ type +" class");
		try {
			entry = c.newInstance();
		} catch (IllegalAccessException iae) {return null;
		} catch (InstantiationException ie) {return null;
		} catch (ExceptionInInitializerError eiie) {return null;
		} catch (SecurityException se) {return null;
		}
		if (debug) System.out.println("\tReturning object");
		return entry;
	}
	
	/** Make sure there is a class of type <tt>t</tt> in the <tt>utilities</tt>
	 * directory. Note that the comparison between <tt>t</tt> and available
	 * classes is case-insensitive; the return string will be the name of the
	 * found class. We assume it is correctly reflects the requested type and
	 * that it can be instantiated.
	 * @param t the class name
	 * @return the class name or <tt>null</tt> if not found */
	private static String confirmType(String t) {
		File f = new File(configPackage);
		String[] types;
		String fullname;
		String classname;
		
		if (debug) {
			System.out.println("In confirmType("+ t +"): ");
			try {
				System.out.println("\tLooking in directory "+ f.getCanonicalPath());
			} catch (IOException ioe) {System.err.println(ioe);}
		}
		if (!f.exists() || !f.isDirectory() || !f.canRead()) {
			if (!f.exists()) System.out.println("\tDoesn't exist!");
			if (!f.isDirectory()) System.out.println("\tNot a directory!");
			if (!f.canRead()) System.out.println("\tCan't read!");
			return null;
		}
		types = f.list();
		if (debug) System.out.println("\tGot directory listing");
		for (int i=0; i<types.length; i++) {
			fullname = types[i];
			if (fullname.endsWith(".class")) {
				if (debug) System.out.println("\tChecking "+ fullname);
				classname = fullname.substring(0, fullname.lastIndexOf('.'));
				if (t.compareToIgnoreCase(classname) == 0) {
					if (debug) System.out.println("\tFound "+ t +"/"+ classname);
					return classname;
				}
			}
		}
		if (debug) System.out.println("\tType "+ t +" NOT found!");
		return null;
	}
	
	private static String[] createTag(String line) {
		String[] keyval = new String[2];
		
		if (debug) System.out.println("Starting section "+ line +":");
		keyval[0] = new String("tag");
		keyval[1] = line.substring(1, line.indexOf(']'));
		if (debug) System.out.println("\tGot tag "+ keyval[1]);
		return keyval;
	}

	/** Parse a single configuration line. Expected format is one of <tt>key=value</tt>,
	 * <tt>key = value</tt>, or <tt>key value</tt>. I may need to rethink this, as we'd
	 * like to be able to have a space separated list of items as a value.
	 * @param line the line to be parsed
	 * @return an array where the first element is the key and the second is the value */
	private static String[] parseLine(String line) {
		String[] kv = new String[2];
		String[] spaceSeps = line.split("\\s");
		int eqix = spaceSeps[0].indexOf('=');
		
		if (eqix > 1) {
			// in "key=value" or "key= value" form
			kv[0] = spaceSeps[0].substring(0, eqix);
			if (spaceSeps[0].endsWith("=")) {
				kv[1] = spaceSeps[1];
			} else {
				kv[1] = spaceSeps[1].substring(eqix+1);
			}
		} else if (spaceSeps[1].compareTo("=") == 0) {
			// in "key = value" form
			kv[0] = spaceSeps[0];
			kv[1] = spaceSeps[2];
		} else if (spaceSeps[1].indexOf('=') == 0) {
			// in "key =value" form
			kv[0] = spaceSeps[0];
			kv[1] = spaceSeps[1].substring(1);
		} else {
			// assume "key value" form
			kv[0] = spaceSeps[0];
			kv[1] = spaceSeps[1];
		}
		if (debug) System.out.println("\tGot "+ kv[0] +"-"+ kv[1]);
		return kv;
	}
	
	/** Fill in the values of an object. Given the name of the class (denoted by
	 * <tt>type</tt>) and a list of key-value pairs, call the appropriate set
	 * methods. It is assumed that method names take the form of a concatentation
	 * of <tt>set</tt> and the key. Note that string comparisons are <b>NOT</b>
	 * case-sensitive.
	 * @param type the name of a class
	 * @param keyvals a list of key-value pairs
	 * @return success */
	private static boolean fillObject(String type, Object o, List keyvals) {
		Class c = null;
		Method[] methods;
		Method m, fin;
		String mname;
		HashMap hm;
		ListIterator li = keyvals.listIterator(0);
		String[] keyval;
		
		if (debug) {
			System.out.print("Filling object "+ type);
			System.out.println(" with "+ keyvals.size() +" values:");
		}
		try {
			c = Class.forName(configPackage+"."+type);
		} catch (LinkageError le) {return false;
		} catch (ClassNotFoundException cnfe) {return false;
		}
		if (debug) System.out.println("\tGot class "+ type);
		try {
			methods = c.getMethods();
			fin = c.getMethod("finish");
			if (debug) System.out.println("\tGot finish method");
			/*
			if (debug) {
				for (int i=0; i<methods.length; i++) {
					System.out.println("\tMethod "+ methods[i].getName());
				}
			}
			*/
		} catch (SecurityException se) {
			return false;
		} catch (NoSuchMethodException nsme) {
			return false;
		} catch (NullPointerException npe) {
			return false;
		}
		if (debug) System.out.println("\tGot methods");
		hm = new HashMap(methods.length+1);
		hm.put("finish", fin);
		if (debug) System.out.println("\tPut "+ fin.getName());
		for (int i=0; i<methods.length; i++) {
			mname = methods[i].getName();
			if (mname.startsWith("set")) {
				hm.put(mname.substring(3).toLowerCase(), methods[i]);
				//if (debug) System.out.println("\tPut "+ mname.substring(3).toLowerCase());
			}
		}
		try {
			while (li.hasNext()) {
				keyval = (String[])li.next();
				//if (debug) System.out.println("\tGetting "+ keyval[0]);
				m = (Method)hm.get(keyval[0].toLowerCase());
				m.invoke(o, keyval[1]);
			}
			fin.invoke(o);
		} catch (IllegalAccessException iae) {
			if (debug) System.out.println(iae);
			return false;
		} catch (InvocationTargetException ite) {
			if (debug) System.out.println(ite);
			return false;
		} catch (NullPointerException npe) {
			if (debug) System.out.println(npe);
			return false;
		} catch (ExceptionInInitializerError eiie) {
			if (debug) System.out.println(eiie);
			return false;
		}
		return true;
	}
	
	/** Parses an AgeS configuration file that uses a single line format.
	 * The format of this type of AgeS configuration file is specified as:
	 * <ul><li>Blank lines are ignored</li>
	 * <li>Comments are entire lines that begin with "#" (hashmark)</li>
	 * <li>The first non-comment/non-blank line begins with the word
	 * <b>FIELDS</b></li>
	 * <li><b>FIELDS</b> is followed by a series of words that represent
	 * the fields contained on each subsequent, non-blank line</li>
	 * <li>Words that represent fields are white-space separated</li>
	 * <li>A group of white-space separated words that should be
	 * grouped as a single field must be "enclosed" by pipe symbols</li>
	 * <li>Blank fields are indicated by the "~" (tilde)</li></ul>
	 * Note that all entries in the configuration file must be on a
	 * single line.
	 * @param fn the name of the config file, including a path if necessary
	 * @return a Vector of entries, each of which is a Vector itself */
	public static Vector parseAgeSSingleLineConfigFile(String fn) {
		BufferedReader br;
		Vector allEntries = new Vector();
		Vector oneEntry;
		Vector fields;
		String line = "#";
		char aChar;
		int beg, end, brak, tilde;
		
		if ((br = getReader(fn)) != null) {				
			// get the FIELDS line, parse it, make it the first entry
			while (line.matches("\\s") || line.startsWith("#")) {
				//System.out.println("line="+ line);
				try {
					line = br.readLine();
				} catch (Exception e) {
					System.err.println("Error reading file:");
					System.err.println(e);
				}
			}
			if (!line.startsWith("FIELDS")) {
				System.err.println("FIELDS line missing from config file");
				return null;
			}
			String[] spaceSeps = line.split("\\s");
			fields = new Vector((spaceSeps.length-1)); // leave off FIELDS
			for (int i=1; i<spaceSeps.length; i++) {
				fields.add(new String(spaceSeps[i]));
			}
			allEntries.add(fields);
			
			// now get the entries
			try {
				while ((line = br.readLine()) != null) {
					// skip comment/blank lines
					if (line.matches("\\s") || line.equals("") || line.startsWith("#"))
						continue;
					// parse the line; change all whitespace to single spaces
					line = line.replaceAll("//s", " ");
					beg = -1;
					end = brak = tilde = 0;
					oneEntry = new Vector(fields.size());
					while (end<line.length()) {
						aChar = line.charAt(end);
						if (brak > 0) {
							if (aChar == ']') {
								if (--brak == 0) {
									oneEntry.add(new String(line.substring(beg,end)));
								}
							} else if (aChar == '[') {
								brak++;
							}
						} else if (aChar == ' ') {
							if (tilde == 0) {
								// begin next token; ready for tilde
								tilde = 1;
								oneEntry.add(new String(line.substring(beg+1,end)));
							} else if (tilde == 2) {
								// have a solo tilde; add blank, begin next token
								tilde = 1;
								oneEntry.add(new String(""));
							}
							beg = end;
							end = beg;
						} else if (aChar == '[') {
							brak++;
							beg = ++end;
							end = beg;
						} else if (aChar == '~') {
							if (tilde == 1) {
								tilde = 2;
							}
						} else {
							tilde = 0;
						}
						end++;
					}
					if (brak > 0) {
						System.out.println("Unclosed bracket pair encountered");
						return null;
					} else if (tilde == 2) {
						oneEntry.add(new String(""));
					} else {
						oneEntry.add(new String(line.substring(beg, (line.length()))));
					}
					allEntries.add(oneEntry);
				}
			} catch (Exception e) {
				System.err.println("Error reading file");
				System.err.println(e);
				return null;
			}
		} else {
			System.out.println("Config file "+ fn +" does not exist");
			return null;
		}
		// just for testing
		if (debug) {
			System.out.println("Config entries:");
			for (int i=0; i<allEntries.size(); i++) {
				oneEntry = (Vector)allEntries.get(i);
				for (int j=0; j<oneEntry.size(); j++) {
					line = (String)oneEntry.get(j);
					System.out.print(line +"# ");
				}
				System.out.println("");
			}
		}
		return allEntries;
	}
}
