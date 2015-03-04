/*********************************************************************
 *
 * SWAGES 1.1
 *
 * (c) by Matthias Scheutz <mscheutz@nd.edu>
 *
 * Utilities to interact with other language environments over sockets
 * (including Pop11 and Common Lisp)
 *
 * Last modified: 06-18-07
 *
 *********************************************************************/

package utilities.comlink;

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.math.*;

// provides serialization for different languages and socket communication
public class ComLink {
    // set to true to print debug messages
    private static final boolean debug = false;
    private static final boolean strict = true;

    // this will hold the deserializer method for class types so they have to be looked up only once
    private static final Hashtable deserializers = new Hashtable();

    // support for different languages 
    // NOTE: these need to start from 0 as they are used for indices in a mapping from clients to languages
    public static final String[] supportedlanguages = new String[]{"JAVA","pop11","Common Lisp","R"};
    public static final int RAW = -1;   // take it as it comes... do not try to deserialize    
    public static final int JAVA = 0;   // not fully implemented yet
    public static final int POP11 = 1;
    public static final int CL = 2;
    public static final int R = 3;      // not fully implemented yet
    
    // message acknowledgements
    public static final int OK = 1;
    public static final int NOTOK = -1;

    /* socket communication in CLISP:
       (setq s (uni-make-socket "127.0.0.1" 10000))
       (uni-send-string s (write-to-string '(1 2 3))
    */

    @SuppressWarnings("unchecked")
    public static ArrayList cons(Object o,ArrayList v) {
        v.add(0,o);
        return v;
    }

    // parses an expression in a file for a given language
    public static Object parseExpression(File f,int language) throws IOException, SyntaxError {
	char[] buffer = new char[(int)f.length()];
	FileReader fr = new FileReader(f);

    // Fill buffer with entire file contents - RM
    int count = fr.read(buffer);
    int index = count;
    while( index < buffer.length ) {
        count = fr.read( buffer, index, buffer.length - index );
        index += count;
    }

	fr.close();
	return parseExpression(new String(buffer),language);
    }

    // call the appropriate parsing function
    public static Object parseExpression(String s,int language) throws SyntaxError {
	switch(language) {
	case RAW:
        case JAVA:
	    return s;
	case POP11:
	    return parseExpressionPop11(s);
	case CL:
	    return parseExpressionCL(s);
	case R:
	    return parseExpressionR(s);
	default:
	    if (strict) throw new SyntaxError("Language code not defined: " + language);
	    System.err.print("Language code not defined: " + language);
	    return null;
	}
    }
    
    // parse pop11 expressions
    @SuppressWarnings("unchecked")
    public static Object parseExpressionPop11(String s) throws Pop11SyntaxError {
	ArrayList ret = null;
	Stack stk = new Stack();
	boolean parsingstring = false;

	// get the list structure out, if there is any
	StringTokenizer st = new StringTokenizer(s,"{}[]'\"",true);
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    //System.out.println("Token is: " + token);
            if (token.equals("[")) {
		if (parsingstring) {
		    if (strict) throw new Pop11SyntaxError("Unexpected [ in string.");
		    System.err.println("Problem parsing POP11 expression: " + s);
		    return null;
		}
		// start of a new list, so push the current ArrayList if not null
		if (ret != null)
		    stk.push(ret);
		ret = new Pop11List();
		//System.out.println("new vec");
	    }
            else if (token.equals("{")) {
		if (parsingstring) {
		    if (strict) throw new Pop11SyntaxError("Unexpected { in string.");
		    System.err.println("Problem parsing POP11 expression: " + s);
		    return null;
		}
		// start of a new ArrayList, so push the current ArrayList if not null
		if (ret != null)
		    stk.push(ret);
		ret = new ArrayList();
		//System.out.println("new vec");
	    }
	    else if (token.equals("]")) {
		// end of a list, so wrap up the ArrayList, place it in the one on the stack
		// and restore ret
		if (parsingstring) {
		    if (strict) throw new Pop11SyntaxError("Unexpected ] in string.");
		    System.err.println("Problem parsing POP11 expression: " + s);
		    return null;
		}
		if (!stk.empty()) {
		    ArrayList temp = (ArrayList)stk.pop();		    
		    temp.add(ret);
		    ret = temp;
		}
		else
		    return ret;
	    }
	    else if (token.equals("}")) {
		// end of a ArrayList, so wrap up the ArrayList, place it in the one on the stack
		// and restore ret
		if (parsingstring) {
		    if (strict) throw new Pop11SyntaxError("Unexpected } in string.");
		    System.err.println("Problem parsing POP11 expression: " + s);
		    return null;
		}		
		if (!stk.empty()) {
		    ArrayList temp = (ArrayList)stk.pop();		    
		    temp.add(ret);
		    ret = temp;
		}
		/*	else if (st.hasMoreTokens()) {
		    token = st.nextToken();
		    if (token.equals("\r") || token.equals("\n"))
			return ret;
		    if (strict) throw new Pop11SyntaxError("Unexpected end of expression reading " + token);
		    System.err.println("Problem parsing POP11 expression: " + s);
		    return null;
		}*/
		else
		    return ret;
	    }
	    else if (token.equals("'"))
		parsingstring = !parsingstring;
	    else if (!token.equals("\r") && !token.equals("\n")) {
		// it is a string without lists, so parse it into parts
		String[] result = (parsingstring ? new String[]{token} : token.split("\\s"));
		
		if (result.length > 1 && ret == null) {
		    if (strict) throw new Pop11SyntaxError("Pop11: multiple expressions without a list.");
		    System.err.println("Problem parsing POP11 expression: " + s);
		    return null;
		}		
		for (int x=0; x<result.length; x++) {

		    Object value = null;
		    try {
			// check if it is an integer
			value = Integer.valueOf(result[x]);
		    } catch (NumberFormatException e1) {
			try {
			    // or a BigDecimal
			    value = new BigDecimal(result[x]);
			} catch (NumberFormatException e2) {
			    // must be a either a string or a pop11 word
			    if (parsingstring)
				value = result[x];
			    else if (result[x].equals("true"))
				value = new Boolean(true);
			    else if (result[x].equals("false"))
				value = new Boolean(false);
			    else
				value = new Pop11Word(result[x]);
			}
		    }    
		    /*
		    if (result.length == 1 && stk.empty()) {
                        System.out.println("*******************");
			return value;
                    }
		    else
                     */ 
                        
                    if (ret != null)
			ret.add(value);
		    else {
			if (strict) throw new Pop11SyntaxError(s);		    
			System.err.println("Problem parsing POP11 expression: " + value);
			return null;
		    }
		}
	    }
	}	
	return ret;
    }

    
    // R deserialization
    // TODO: need to implement it...
    // For now we simply use the LISP tokenizer for the message structure as we the rclient
    // produceds a LISP list expression
    public static Object parseExpressionR(String s) throws RSyntaxError {
	// get the list structure out, if there is any
	// StringTokenizer st = new StringTokenizer(s,"()\'\"`,;#\n\t\r",true);
	// return s; // parseRExpression(st,false);
	try {
	    return parseExpressionCL(s);
	} catch (CLSyntaxError e) {
	    throw new RSyntaxError(e.toString());
	}
    }

    // helper function for parsing Rexpressions
    //@SuppressWarnings("unchecked")
    //private static Object parseRExpression(StringTokenizer st,boolean allowcommas) throws RSyntaxError {
    // SEE CODE BELOW FOR CLISP...
    
    
    // Common Lips deserialization
    // TODO: need to handle strings correctly
    //
    public static Object parseExpressionCL(String s) throws CLSyntaxError {
	// get the list structure out, if there is any
	StringTokenizer st = new StringTokenizer(s,"()\'\"`,;#\n\t\r",true);
	return parseCLExpression(st,false);
    }

    // helper function for parsing LISP expressions
    @SuppressWarnings("unchecked")
    private static Object parseCLExpression(StringTokenizer st,boolean allowcommas) throws CLSyntaxError {
	ArrayList ret = null;
	Stack stk = new Stack();
	boolean parsingstring = false;
	boolean incomment = false;
	String currentstring = "";

	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    if (debug) System.out.println("Token is: " + token);
	    // check if we are in a comment and just skip tokens until we hit newline or EOF
            if (incomment) {
		if (debug) System.out.println("In comment, skipping " + token);
		if (token.equals("\n")) {
		if (debug) System.out.println("End comment");
		    incomment = false;
		}
		// otherwise just skip the token for now, should record the comment though...
	    }
	    else if (parsingstring) {
		if (debug) System.out.println("In string");
		if (token.equals("\"")) {
		    if (debug) System.out.println("End string, string is: " + currentstring);
		    parsingstring = false;
		    if (ret == null)
			return currentstring;
		    else 
			ret.add(currentstring);
			currentstring="";
		}
		else {
		    if (debug) System.out.println("Adding to string: " + token);
		    // add to string
		    currentstring = currentstring + token;
		}
	    }
	    else if (token.equals("\n") || token.equals("\t") || token.equals("\r")) {
		// whitespace, skip it
	    }
	    // start reading comment
            else if (token.equals(";")) {
		if (debug) System.out.println("Starting comment");
		incomment = true;
	    }
            else if (token.equals(",")) {
		if (debug) System.out.println("Found comma ");
		// check if commas are allowed
		if (allowcommas)
		    ret.add(new CLSymbol(token));
		else {
		    if (strict) throw new CLSyntaxError(token);		    
		    System.err.println("Problem parsing LISP expression: " + token);
		    return null;
		}
	    }
            else if (token.equals("#")) {
                // TODO: need to handle this for function symbols, i.e., #'...
		System.out.println("WARNING: # not implemented yet!");
	    }
            else if (token.equals("`")) {
		if (debug) System.out.println("Found backquote");
		System.out.println("WARNING: ` not fully implemented yet");
		// start of a new ArrayList, so push the current ArrayList if not null
		if (ret != null)
		    stk.push(ret);
		ret = new Pop11List();
		// add the special form "quote" as the first element, 
		// the next element needs to be an expression...
		CLSymbol cmd = new CLSymbol("quote");
		ret.add(cmd);
		ret.add(parseCLExpression(st,true));
		// mark it as backquoted list...
		cmd.backquote = true;
		//System.out.println("new vec");
		if (!stk.empty()) {
		    ArrayList temp = (ArrayList)stk.pop();		    
		    temp.add(ret);
		    ret = temp;
		}
		else
		    return ret;
	    }
            else if (token.equals("(")) {
		if (debug) System.out.println("New list");
		// start of a new list, so push the current ArrayList if not null
		if (ret != null)
		    stk.push(ret);
		ret = new Pop11List();
		//System.out.println("new vec");
	    }
            else if (token.equals("\'")) {
		if (debug) System.out.println("New quote");
		if (ret != null)
		    stk.push(ret);
		ret = new Pop11List();
		// add the special form "quote" as the first element, 
		// then the next element needs to be an expression...
		CLSymbol cmd = new CLSymbol("quote");
		ret.add(cmd);
		ret.add(parseCLExpression(st,allowcommas));
		if (!stk.empty()) {
		    ArrayList temp = (ArrayList)stk.pop();		    
		    temp.add(ret);
		    ret = temp;
		}
		else
		    return ret;
		//System.out.println("new vec");
	    }
	    else if (token.equals(")")) {
		if (debug) System.out.println("End list");
		// end of a list, so wrap up the ArrayList, place it in the one on the stack
		// and restore ret
		if (!stk.empty()) {
		    ArrayList temp = (ArrayList)stk.pop();		    
		    temp.add(ret);
		    ret = temp;
		}
		else
		    return ret;
	    }
	    else if (token.equals("\"")) {
		if (debug) System.out.println("Starting string");	    
		parsingstring = true;
		// must be a string 
	    }
	    else  {
		if (debug) System.out.println("Other tokens...");	    
		// it is a string without lists, so parse it into parts
		// split on whitespace
		String[] result = token.split("\\s");

		if (debug) System.out.println("NUMBER OF SPLIT TOKENS: " + result.length);
		
		if (result.length > 1 && ret == null) {
		    if (strict) throw new CLSyntaxError("LISP: Multiple expressions without a list.");
		    System.err.println("Problem parsing LISP expression: " + token);
		    return null;
		}		
		for (int x=0; x<result.length; x++) {
		    if (result[x].trim().equals(""))
			continue;
		    Object value = null;
		    try {
			// check if it is an integer
			value = Integer.valueOf(result[x]);
		    } catch (NumberFormatException e1) {
			try {
			    // or a BigDecimal
			    value = new BigDecimal(result[x]);
			} catch (NumberFormatException e2) {
			    // must be a either a string or a pop11 word
			    if (parsingstring)
				value = result[x];
			    else if (result[x].equals("t") || result[x].equals("T"))
				value = new Boolean(true);
			    else if (result[x].equals("nil") || result[x].equals("NIL"))
				value = new Boolean(false);
			    else
				value = new CLSymbol(result[x]);
			}
		    }    
		    /*		    
		    if (result.length == 1 && stk.empty()) {
			System.out.println("Returning token " + value);
			return value;
		    }
		    else 
		    */
		    if (ret != null) {
			if (debug) System.out.println("Adding " + value);	    
			ret.add(value);
		    }
		    else {
			return value;
			/*			
			{
			if (strict) throw new Pop11SyntaxError(token);		    
			System.err.println("Problem parsing LISP expression: " + value);
			return null;
			*/
		    }
		}
	    }
	}	
	return ret;
    }    

    // general entry
    public static void produceList(File f,Object o,int language) throws IOException, SyntaxError {	
	FileWriter fr = new FileWriter(f);
	fr.write(produceList(o,language));
	fr.close();
    }
    
    
    // call the appropriate production function
    public static String produceList(Object o,int language) throws SyntaxError {
	switch(language) {
	case JAVA:
        case RAW:
	    return o.toString();
	case POP11:
	    return produceListPop11(o);
	case CL:
	    return produceListCL(o);
        case R:
	    return produceListR(o);
	default:
	    if (strict) throw new SyntaxError("Language code not defined: " + language);
	    System.err.print("Language code not defined: " + language);
	    return null;
	}
    }
    
    // pop11 list production
    @SuppressWarnings("unchecked")
    public static String produceListPop11(Object o) {
	StringBuffer s = new StringBuffer(1024);
	Stack stk = new Stack();
	boolean lastwasitem = false;
	int indent = 0;
	boolean firstopen = true;
	boolean lastwasclosed = false;
	
	// put the object on the stack
	stk.push(o);
	while (!stk.empty()) {
	    o = stk.pop();
	    if (o instanceof ArrayList) {
		ArrayList v = (ArrayList)o;
		// put the closing bracket on the stack as a delimiter
		stk.push(new Character(']'));
		// put all elements on the stack in reversed order
		for(int i = v.size()-1; i>=0; i--)		    
		    stk.push(v.get(i));
		// write the open list bracket
		if (!firstopen && !lastwasclosed)
		    s.append('\n');
		for (int k=0; k<indent; k++)
		    s.append(' ');
		s.append('[');
		indent++;
		lastwasitem = false;
		lastwasclosed = false;
		firstopen = false;
	    }
	    else if (o instanceof Character) {
		if (((Character)o).charValue() == ']') {
		    indent--;
		    if (lastwasclosed) {
			for (int k=0; k<indent; k++)
			    s.append(' ');
		    }
		    s.append(o);
		    s.append('\n');
		    lastwasclosed = true;		
		    lastwasitem = false;
		}
		else {
		    if (lastwasitem)
			s.append(' ');
		    s.append(o);
		    lastwasitem = true;
		    lastwasclosed = false;					
		}
	    }
	    else {
		if (lastwasitem)
		    s.append(' ');
		if (o instanceof Word)
		    s.append(((Word)o).word);
		else if (o instanceof String)
		    s.append("'" + o + "'");		
		else
		    // must have been a number, just append it
		    s.append(o);
		lastwasitem = true;
		lastwasclosed = false;		
	    }
	}
	return s.toString();
    }

    
    // R list production
    // TODO: finish....
    // for now we produce simple R list expressions that can be parsed by R
    @SuppressWarnings("unchecked")
    public static String produceListR(Object o) throws RSyntaxError {
	StringBuffer s = new StringBuffer(1024);
	Stack stk = new Stack();
	boolean lastwasitem = false;
	int indent = 0;
	boolean firstopen = true;
	boolean lastwasclosed = false;

	// put the object on the stack
	stk.push(o);
	while (!stk.empty()) {
	    o = stk.pop();
	    if (o instanceof ArrayList) {
		ArrayList v = (ArrayList)o;
		// check if it is a quote or backquote structure
		if (v.size() > 0) {
		    Object fo = v.get(0);
		    if ((fo instanceof Word) &&(((Word)fo).word).equals("quote")) {
                        if (((CLSymbol)fo).backquote) {
                            s.append(" `");
                        }
                        else {
                            s.append(" \'");
                        }
                        if (v.size()>2) {
                            if (strict) throw new RSyntaxError("Illegal quote construct: " + v);
                            System.err.print("Illegal quote construct: " + v);
                            return null;
                        }
                        stk.push(v.get(1));
                        lastwasitem = false;
                        firstopen = true;                        
		    }
		    else {
			stk.push(new Character(')'));
			// put all elements on the stack in reversed order
			for(int i = v.size()-1; i>=0; i--) {
			    stk.push(v.get(i));
			    if (i>0)
				stk.push(new Character(','));
			}
			// write the open list bracket
			//if (!firstopen && !lastwasclosed) {
			    //s.append("\n");
			  //  for (int k=0; k<indent; k++)
			//	s.append(' ');
			//}
			s.append("list(");
			//indent++;
			lastwasitem = false;
			firstopen = false;
		    }
		}
		else {
		    s.append("list()");
		    lastwasitem = true;
		    firstopen = false;
		}
		lastwasclosed = false;
	    }
	    else if (o instanceof Character) {
		if (((Character)o).charValue() == ']') {
		    //indent--;
		   // if (lastwasclosed) {
			//for (int k=0; k<indent; k++)
			//    s.append(' ');
		    //}
		    s.append(o);
		    //s.append('\n');
		    lastwasclosed = true;		
		    lastwasitem = false;
		}
		else {
		    if (lastwasitem)
			s.append(' ');
		    s.append(o);
		    lastwasitem = true;
		    lastwasclosed = false;					
		}
	    }
	    else {
		if (lastwasitem)
		    s.append(' ');
		lastwasitem = true;
		lastwasclosed = false;		
		if (o instanceof Word) {
		    if ((((Word)o).word).equals(",")) {
			s.append(',');		
			lastwasitem = false;
		    }
                    else {
                        s.append("quote(" + (((Word)o).word) + ")");
                    }		    
		}
		else if (o instanceof String)
		    s.append("\"" + o + "\"");
                // map "null" onto the empty list
		else if (o == null)
                    s.append("list()");		
                else
		    // must have been a number, just append it
		    s.append(o);
	    }
	}
	return s.toString();
    }



    // CL list production
    //
    @SuppressWarnings("unchecked")
    public static String produceListCL(Object o) throws CLSyntaxError {
	StringBuffer s = new StringBuffer(1024);
	Stack stk = new Stack();
	boolean lastwasitem = false;
	int indent = 0;
	boolean firstopen = true;
	boolean lastwasclosed = false;

	// put the object on the stack
	stk.push(o);
	while (!stk.empty()) {
	    o = stk.pop();
	    if (o instanceof ArrayList) {
		ArrayList v = (ArrayList)o;
		// check if it is a quote or backquote structure
		if (v.size() > 0) {
		    Object fo = v.get(0);
		    if (fo instanceof CLSymbol && (((CLSymbol)fo).word).equals("quote")) {
			if (((CLSymbol)fo).backquote) {
			    s.append(" `");
			}
			else {
			    s.append(" \'");
			}
			if (v.size()>2) {
			    if (strict) throw new CLSyntaxError("Illegal quote construct: " + v);
			    System.err.print("Illegal quote construct: " + v);
			    return null;
			}
			stk.push(v.get(1));
			lastwasitem = false;
			firstopen = true;
		    }
		    else {
			stk.push(new Character(')'));
			// put all elements on the stack in reversed order
			for(int i = v.size()-1; i>=0; i--)		    
			    stk.push(v.get(i));
			// write the open list bracket
			if (!firstopen && !lastwasclosed) {
			    s.append('\n');
			    for (int k=0; k<indent; k++)
				s.append(' ');
			}
			s.append('(');
			indent++;
			lastwasitem = false;
			firstopen = false;
		    }
		}
		else {
		    s.append(" ()");
		    lastwasitem = true;
		    firstopen = false;
		}
		lastwasclosed = false;
	    }
	    else if (o instanceof Character) {
		if (((Character)o).charValue() == ']') {
		    indent--;
		    if (lastwasclosed) {
			for (int k=0; k<indent; k++)
			    s.append(' ');
		    }
		    s.append(o);
		    s.append('\n');
		    lastwasclosed = true;		
		    lastwasitem = false;
		}
		else {
		    if (lastwasitem)
			s.append(' ');
		    s.append(o);
		    lastwasitem = true;
		    lastwasclosed = false;					
		}
	    }
	    else {
		if (lastwasitem)
		    s.append(' ');
		lastwasitem = true;
		lastwasclosed = false;		
		if (o instanceof Word) {
		    if ((((Word)o).word).equals(",")) {
			s.append(',');		
			lastwasitem = false;
		    }
		    else
			s.append(((Word)o).word);
		}
		else if (o instanceof String)
		    s.append("\"" + o + "\"");		
                // map "null" onto the empty list
		else if (o == null)
                    s.append("()");		
		else
		    // must have been a number, just append it
		    s.append(o);
	    }
	}
	return s.toString();
    }
    

    // reads an object from an existing socket and returns the error status at the end
    public static Object read(Socket s, int language) throws IOException {
	BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
	return read(in,language);
    }

    public static Object read(BufferedReader in,int language) throws IOException {
        return read(in,language,true);
    }
    
    public static Object read(BufferedReader in,int language, boolean waitforline) throws IOException {
	switch(language) {
	case RAW:
        case JAVA:            
	    //try {
	    return in.readLine();  // TODO: make this ready as many lines a necessary...
	    //} catch(Pop11SyntaxError e) {
	    //throw new IOException(e.toString());
	    // }
	case POP11:
	    return datafread(in);
	case CL:
	    try {
                if (!waitforline) {
                    try {
                        while(!in.ready())
                            Thread.sleep(100);
                    } catch (InterruptedException ie) {}
                    Integer i = new Integer(in.read());
                    //System.err.println("------> " + i);  
                    return i;
                }
                else {
                    String line = null;
                    try {
                        while ((line = in.readLine()) == null) {
                            Thread.sleep(100);
                        }
                        // now read as many lines as there are waiting...
                        while (in.ready())
                            line += "\n" + in.readLine();                        
                    } catch(InterruptedException e) {}
                    //System.err.println("======> " + parseExpressionCL(line));
                    return parseExpressionCL(line); 
                }
	    } catch(CLSyntaxError e) {
		throw new IOException(e.toString());
	    }
	case R:
	    try {
		//	    String s;
		//char c;
		//while ((c = in.read()) != -1)
		String line = null;
		try {
                    while ((line = in.readLine()) == null) {
                        System.out.println("TRYING TO READ IT...");
                        Thread.sleep(100);
                    }
                    // now read as many lines as there are waiting...
                    while (in.ready())
                        line += "\n" + in.readLine();     
                } catch(InterruptedException e) {}
		return parseExpressionR(line);
	    } catch(RSyntaxError e) {
		throw new IOException(e.toString());
	    }
            
	default:
	    if (strict) throw new IOException("Language code not defined: " + language);
	    System.err.print("Language code not defined: " + language);
	    return null;
	}
    }

    // writes an object into an existing socket and returns the error status at the end
    public static Object read(File f,int language) throws IOException {
	switch(language) {
        case RAW:
            try {
		return parseExpression(f,RAW);
	    } catch(SyntaxError e) {
		throw new IOException(e.toString());
	    }
	case JAVA:        
	    try {
		return parseExpression(f,JAVA);
	    } catch(SyntaxError e) {
		throw new IOException(e.toString());
	    }
	case POP11:
	    FileReader in = new FileReader(f);
	    return datafread(in);
	case CL:
	    try {
		return parseExpression(f,CL);
	    } catch(SyntaxError e) {
		throw new IOException(e.toString());
	    }
	case R:
	    try {
		return parseExpression(f,R);
	    } catch(SyntaxError e) {
		throw new IOException(e.toString());
	    }
	default:
	    if (strict) throw new IOException("Language code not defined: " + language);
	    System.err.print("Language code not defined: " + language);
	    return null;
	}
    }

    // writes an object into a string and returns
    public static Object read(String str,int language) throws IOException, SyntaxError {
	switch(language) {
        case RAW:                
	case JAVA:
	    //try {
	    return str;
	    //} catch(Pop11SyntaxError e) {
	    //throw new IOException(e.toString());
	    //}
	case POP11:
	    StringReader sr = new StringReader(str);
	    return datafread(sr);
	case CL:
            return parseExpression(str,CL);
	case R:
            return parseExpression(str,R);
	default:
	    if (strict) throw new IOException("Language code not defined: " + language);
	    System.err.print("Language code not defined: " + language);
	    return null;
	}
    }
        
    // the actual pop11 read function
    @SuppressWarnings("unchecked")
    static private Object datafread(Reader r) throws IOException {       	
	final StreamTokenizer s = new StreamTokenizer(r);

	class fread {

	    Object datafreadhelper() throws IOException {	
		if (s.nextToken() == s.TT_WORD) 
		    if (s.sval.equals("zl")) {
			s.nextToken();
			int l = (int)s.nval;
			Pop11List v = new Pop11List(l);
			for(int i=0;i<l;i++) {
			    v.add(datafreadhelper());
			}	
			return v;
		    }
		/* NO IMPLEMENTED
		   elseif x == "zp" then
		   conspair(datafread(),datafread()) -> x;
		*/
		    else if (s.sval.equals("zs")) {
			s.nextToken();
			int l = (int)s.nval;
			byte[] stringbytes = new byte[l];
			for(int i=0;i<l;i++) {
			    s.nextToken();
			    stringbytes[i] = (byte)s.nval;
			}
			return new String(stringbytes);
		    }
		    else if (s.sval.equals("zv")) {
			s.nextToken();
			int l = (int)s.nval;
			ArrayList v = new ArrayList(l);
			for(int i=0;i<l;i++) {
			    v.add(datafreadhelper());
			}
			return v;
		    }
		/* NOT IMPLEMENTED
		   else if x == "za" then
		   newarray(datafread()) -> x;
		   datalength(arrayArrayList(x)) -> t;
		   for n from 1 to t do
		   datafread() -> fast_subscrv(n,arrayArrayList(x));
		   endfor;
		   elseif x == "zr" then
		   consref(datafread()) -> x;
		*/
		    else if (s.sval.equals("zb")) {
			s.nextToken();
			return new Boolean(s.sval.equals("true") ? true : false);
		    }
		    else if (s.sval.equals("zw")) {
			s.nextToken();
			int l = (int)s.nval;
			byte[] wordbytes = new byte[l];
			for(int i=0;i<l;i++) {
			    s.nextToken();
			    wordbytes[i] = (byte)s.nval;
			}
			return new Pop11Word(new String(wordbytes));
		    }
		    // MS: new, deserialize class
		    else if (s.sval.equals("zc")) {
			// TODO: if a compiler is available and the class does not exist, then produce it on the fly...
			// get the class name, language is the next token			s.nextToken();
			s.nextToken();
			String key = s.sval;
			boolean trydirect = false;
			Object o = null;
			Class co = null;
			// first get the class denoted by the key
			try {
			    co = Class.forName(key);		
			    // create a new instance of the class...
			    o = co.newInstance();	
			    // } catch (SecurityException e1) {
			    // if (debug) System.out.println("SECURITY");
			} catch (ClassNotFoundException e6) {
			    System.err.println("Class definition not found for pop11 key: " + key);
			    return null;
			} catch (IllegalAccessException e3) {
			    System.err.println("ILLEGAL ACCESS will instantiation class " + key);
			    return null;
			} catch (InstantiationException e7) {
			    System.err.println("Class " + key + " cannot be instantiated");
			    return null;
			}
			// first check if there is a specific serialization method for this class
			try {
			    Method meth = null;
			    Object[] newargs = null;
			    ArrayList al;
			    int numargs = 0;
			    // check the stored ones
			    if ((al = (ArrayList)deserializers.get(key)) == null) {
				// did not find it, so look it up
				// get all methods and then find the one called "fromPop11"
				// NOTE: THIS WILL NOT WORK IF THERE ARE MULTIPLE SUCH METHODS...
				Method[] meths = co.getDeclaredMethods();
				for(int i = 0; i< meths.length; i++) {
				    meth = meths[i];
				    // check if it is the "fromPop11" method
				    if (meth.getName().equals("fromPop11"))
					break;
				    meth = null;
				}
				if (meth == null) {
				    if (debug) System.err.println("Class " + key + " does not have a 'fromPop11' method!");
				    trydirect = true;
				} 
				else {
				    // create a non-synchronized list
				    al = new ArrayList(2);
				    al.add(meth);
				    newargs = new Object[meth.getParameterTypes().length];
				    al.add(newargs);
				    // store the method plus the argument number in the Hashtable under the class key
				    deserializers.put(key,al);
				}
			    }
			    // otherwise we already have them in the Hashtable, so get them
			    else {
				meth = (Method)al.get(0);
				newargs = (Object[])al.get(1);
			    }
			    // check if we have to try it the direct way
			    if (!trydirect) {
				// TODO: we could check that the types are OK too
				for(int i=0; i<newargs.length; i++) {
				    newargs[i] = datafreadhelper();
				}
				// now invoke the deserializer on the arguments
				meth.invoke(o,newargs);
				return o;
			    }
			} catch (SecurityException e1) {
			    if (debug) System.err.println("Access to pop11 field serializer denied in class " + key);
			} catch (IllegalAccessException e2) {
			    if (debug) System.err.println("Access to pop11 field serializer denied in class " + key);
			} catch (InvocationTargetException e3) {
			    if (debug) System.err.println("Cannot invoke field serializer defined in class " + key);
			}
			// if we get here then errors occured above (e.g., we don't have the deserializer), so try it directly
			try {
			    // now get the fields
			    Field[] fs = co.getDeclaredFields();
			    // read in the fields one by one--THIS will cause an exception if not all fields are Public!
			    for(int f=0;f<fs.length;f++)
				// got the fields, so put them on the stack, one by one
				// set the value of the field in the object to the serialized value
				fs[f].set(o,datafreadhelper());
			} catch (SecurityException e1) {
			    if (debug) System.out.println("SECURITY");
			} catch (IllegalAccessException e2) {
			    if (debug) System.out.println("ILLEGAL ACCESS");
			} catch (IllegalArgumentException e3) {
			    if (debug) System.out.println("ILLEGAL ARGUMENT");
			} catch (NullPointerException e4) {
			    if (debug) System.out.println("NULL POINTER");
			} catch (ExceptionInInitializerError e5) {
			    if (debug) System.out.println("INITIALIZER");
			}
			return o;				
		    }
		/* NOT IMPLEMENTED
		   elseif x == "zu" then
		   ;;; get ArrayListclass - Aled, June 1st, 1987
		   datafread() -> t;
		   key_of_dataword(t) -> key;
		   unless key then
		   mishap('Unknown dataword encountered in datafile\n' >< ';;;          (ArrayListclass declaration not loaded?)', [^t]);
		   endunless;
		   repeat (datafread() ->> t) times datafread() endrepeat;
		   apply(t, class_cons(key)) -> x
		*/
		    else if (s.sval.equals("zh")) {
			Hashtable h = new Hashtable();
			ArrayList v = (ArrayList)datafreadhelper();
			for(int i=0;i<v.size();i++) {
			    ArrayList pair = (ArrayList)v.get(i);
			    h.put(pair.get(0),pair.get(1));
			}
			// ignore the size, the default index, and the storage property
			s.nextToken();
			s.nextToken();
			s.nextToken();
			return h;
		    }
		    // don't the key, error
		    else return null;
		else {
		    int d = (int)s.nval;
		    if ((double)d == s.nval)
			return new Integer((int)s.nval);
		    else
			return new BigDecimal(s.nval);
		}
		/* NO IMPLEMENTED
		   elseif x == "zC" then
		   rditem() -> t;
		   partapply(valof(t),datafread()) -> x;
		   elseif x == "zP" then
		   valof(rditem()) -> x;
		*/
		// should never get here...
	    }
	}
	return new fread().datafreadhelper();
    }
    
    
    // writes an object into an existing socket and returns the error status at the end
    public static void write(Socket s,Object o,int language) throws IOException {
	PrintWriter out = new PrintWriter(s.getOutputStream(),false);
        write(out,o,language);
    }


    // writes an object into an existing socket and returns the error status at the end
    public static void write(PrintWriter out,Object o,int language) throws IOException {        
	switch(language) {
        case RAW:
            out.write(o.toString());
            break;
        case JAVA:
	    try{
		out.write(produceList(o,0));
	    } catch(SyntaxError e) {
		throw new IOException(e.toString());
	    }
	    break;
	case POP11:
	    datafwrite(out,o);
	    break;
	case CL:
	    try {                
		out.write(produceListCL(o));
                //System.err.println("~~~~~~> " + o);  
	    } catch(CLSyntaxError e) {
		throw new IOException(e.toString());
	    }
	    break;
	case R:
	    try {
                //System.out.println(produceListR(o));
		out.write(produceListR(o));                
	    } catch(RSyntaxError e) {
		throw new IOException(e.toString());
	    }
	    break;
	default:
	    if (strict) throw new IOException("Language code not defined: " + language);
	    System.err.print("Language code not defined: " + language);
	    break;
	}
	out.write('\n');
	out.flush();
	if (out.checkError())
	    throw new IOException("Problem with socket in datafwrite");
    }

    // writes an object into a file and returns
    // MS: added newline at the end to make the files identical to pop11
    public static void write(File f,Object o,int language) throws IOException {
	FileWriter fw = new FileWriter(f);
	switch(language) {
	case RAW:
            fw.write(o.toString());
            break;
        case JAVA:
	    try{
		fw.write(produceList(o,0));
	    } catch(SyntaxError e) {
		throw new IOException(e.toString());
	    }
	    break;
	case POP11:
	    datafwrite(fw,o);
	    break;
	case CL:
	    try{
		fw.write(produceListCL(o));
	    } catch(CLSyntaxError e) {
		throw new IOException(e.toString());
	    }
	    break;
	case R:
	    try{
		fw.write(produceListR(o));
	    } catch(RSyntaxError e) {
		throw new IOException(e.toString());
	    }
	    break;	default:
	    if (strict) throw new IOException("Language code not defined: " + language);
	    System.err.print("Language code not defined: " + language);
	    break;
	}
	fw.flush();
	fw.close();
    }

    // writes an object into a string and returns
    public static String write(Object o,int language) throws IOException {
	switch(language) {
	case RAW:
            return o.toString();
        case JAVA:
	    try{
		return produceList(o,0);
	    } catch(SyntaxError e) {
		throw new IOException(e.toString());
	    }
	case POP11:
	    StringWriter sw = new StringWriter(1024);
	    datafwrite(sw,o);
	    sw.flush();
	    return sw.toString();
	case CL:
	    try {
		return produceListCL(o);
	    } catch(CLSyntaxError e) {
		throw new IOException(e.toString());
	    }            
	case R:
	    try {
		return produceListR(o);
	    } catch(RSyntaxError e) {
		throw new IOException(e.toString());
	    }
	default:
	    if (strict) throw new IOException("Language code not defined: " + language);
	    System.err.print("Language code not defined: " + language);
	    return null;
	}
    }

    
    // subset of POP11 serialization in "datafile"
    // the parsing order in the if-statement is important
    @SuppressWarnings("unchecked")
    private static void datafwrite(Writer w,Object o) throws IOException {
	Stack stk = new Stack();
	stk.push(o);

	while (!stk.empty()) {
	    w.write(" ");
	    o = stk.pop();	
	    if (o instanceof Integer || o instanceof Float || o instanceof Double || o instanceof BigDecimal)
		w.write(o.toString().replace('E', 'e'));
	    else if (o instanceof Pop11Word) {
		// printing the structure takes up more space and is slower than
		// just printing the word but it ensures that words with non printing
		// characters are stored properly (eg -space-) and datafile control
		// words (e.g. zw) are not confused.
		byte[] wordbytes = (((Pop11Word)o).word).getBytes();
		w.write("zw ");
		w.write((new Integer(wordbytes.length)).toString());
		for(int i=0;i<wordbytes.length;i++) {
		    w.write(" ");
		    w.write((new Byte(wordbytes[i]).toString()));
		}	    
	    }
	    else if (o instanceof Pop11List) {
		ArrayList v = (ArrayList)o;
		w.write("zl "); 
		w.write((new Integer(v.size())).toString());
		for(int i = v.size()-1; i>=0; i--)		    
		    stk.push(v.get(i));
	    }	    
	    else if (o instanceof String) {
		byte[] stringbytes = ((String)o).getBytes();
		w.write("zs ");
		w.write((new Integer(stringbytes.length)).toString());
		for(int i=0;i<stringbytes.length;i++) {
		    w.write(" ");
		    w.write((new Byte(stringbytes[i])).toString());
		}
	    }
	    else if (o instanceof ArrayList) {
		ArrayList v = (ArrayList)o;
		w.write("zv "); 
		w.write((new Integer(v.size())).toString());
		for(int i = v.size()-1; i>=0; i--)		    
		    stk.push(v.get(i));
	    }
	    /* NOT IMPLEMENTED
	    else if (o instanceof Array) {		
		pr("za"); datafwrite(boundslist(x)); appdata(arrayArrayList(x),datafwrite);
	    }
	    */
	    /* NOT IMPLEMENTED
	       else if isref(x) then
	       pr("zr"); datafwrite(cont(x));
	    */
	    else if (o instanceof Boolean) {
		w.write("zb ");
		if (((Boolean)o).booleanValue())
		    w.write("true");
		else
		    w.write("false");
	    }
	    /* NOT IMPLEMENTED
	    else if isArrayListclass(x) then       ;;; user defined ArrayList
		spr("zu"); spr(dataword(x)); pr(datalength(x)); appdata(x,datafwrite);
	    */
	    else if (o instanceof Hashtable) {
		Hashtable h = (Hashtable)o;
		w.write("zh ");
		ArrayList ht = new ArrayList(h.size());
		int i = h.size()-1;
		// convert the hashtable to a list
		Enumeration ke = h.keys();
		Enumeration ee = h.elements();
		while (ke.hasMoreElements()) {
		    ArrayList item = new ArrayList(2);
		    item.add(ke.nextElement());
		    item.add(ee.nextElement());
		    ht.set(i,item);
		    i--;
		}
		// put the converted hashtable on the stack
		stk.push(ht);
		stk.push(new Integer(h.size()));
		stk.push("null");
		stk.push(new Boolean(true));
	    }
	    /* NOT IMPLEMENTED
	    elseif isclosure(x) then
 	        spr("zC"); pr(pdprops(pdpart(x))); pr(' ');
 	        datafwrite(datalist(x))
 	    elseif isprocedure(x) then
 	        spr("zP"); pr(pdprops(x));
            */
	    else {                
                // it is a structure we don't know so write the class name		
                w.write("zc ");
                Class co = o.getClass();
                // write the data word
                String key = co.getName();
                // but first, get rid of package names and the turn the rest into lower case
                // this is the naming convention in POPSWAGES
                w.write((key.substring(key.lastIndexOf('.')+1)).toLowerCase());
		// first try the serializer if it's there
		boolean tryserializer = false;
		// check if it has a serialization method defined, language will return its instances in a ArrayList
		try {
		    Method meth = co.getDeclaredMethod("toPop11",(Class[])null);
		    // Method meth = co.getDeclaredMethod("toPop11",null);
		    // found it, put each element of the return ArrayList on the stack in reverse order whatever it is
		    ArrayList pvec = (ArrayList)meth.invoke(o,(Object[])null);;
		    // ArrayList pvec = (ArrayList)meth.invoke(o,null);;
		    for(int i=pvec.size()-1;i>=0;i--) 
			stk.push(pvec.get(i));
		    // if we made it here it was successful
		    tryserializer = true;		    
		} catch (SecurityException e1) {
		    if (debug) System.err.println("Access to pop11 field serializer denied in " + o.getClass());
		} catch (IllegalAccessException e2) {
		    if (debug) System.err.println("Access to pop11 field serializer denied in " + o.getClass());
		} catch (NoSuchMethodException e3) {
		    if (debug) System.err.println("No pop11 field serializer defined in " + o.getClass());
		} catch (InvocationTargetException e4) {
		    if (debug) System.err.println("Cannot invoke field serializer defined in " + o.getClass());
		}
		// try the direct method if the serializer did not work
		if (!tryserializer) {
		    // try to get the declared fields
		    try {
			Field[] fs = co.getDeclaredFields();
			// got the fields, so put them on the stack, one by one--this will fail if they are not all Public
			for(int f=fs.length-1;f>=0;f--) {
			    // put the value of the field in object o on the stack
			    stk.push(fs[f].get(o));
			}
		    } catch (SecurityException e1) {
		    } catch (IllegalAccessException e2) {
		    } catch (IllegalArgumentException e3) {
		    } catch (NullPointerException e4) {
		    } catch (ExceptionInInitializerError e5) {
		    }
		}
            }
	}
    }

    // this sends a message to the server, waits for OK=0, and returns null if no problem occured
    // otherwise it will either throw an IOException if there was problem with the socket or it 
    // will return the error message packet from the receiver
    // the sender needs to take action if the receiver expected a different message
    @SuppressWarnings("unchecked")
    public static void sendMessage(int senderID,int mtype,Object data,Socket comm_sock,int language) throws IOException {	
	// use ArrayList, which is a super class to all specific lists...
	ArrayList v = new ArrayList(3);
	v.add(senderID);
	v.add(mtype);
	v.add(data);
	//	write(comm_sock,[%senderID,mtype,data%]);
	write(comm_sock,v,language);
        Object o = read(comm_sock,language);
        if (o instanceof Integer && ((Integer)o).intValue() != OK)
            throw new IOException("Received non-0 acknowledgement");
    }

    @SuppressWarnings("unchecked")
    public static void sendMessage(int senderID,int mtype,Object data,BufferedReader in,PrintWriter out,int language) throws IOException {	
        // use ArrayList, which is a super class to all specific lists...
	ArrayList v = new ArrayList(3);
	v.add(senderID);
	v.add(mtype);
	v.add(data);	
        //	write(comm_sock,[%senderID,mtype,data%]);        
	write(out,v,language);
        Object o = read(in,language,false);
        
        if (o instanceof Integer && ((language != CL &&((Integer)o).intValue() != OK )|| (language == CL && ((Integer)o).intValue() != 49)))
            throw new IOException("Received non-0 acknowledgement");
        
        ////while(!in.ready());
        //int ack = in.read();
        // MS: this is a hack since I don't know yet how to make CL send 0 or 1 through the socket...'
        //if (ack == NOTOK || (language == CL && ack != 49))
            //throw new IOException("Received NOTOK acknowledgement");
        /*
        //MS: only need to read a character now for acknowledgement
        System.out.println("=======");
        Object o = read(in,language);
        System.out.println(o);
        if (o instanceof Integer && ((Integer)o).intValue() != OK)
            throw new IOException("Received non-0 acknowledgement");
        */
    }
    
    // receives a message and returns it, throws an IO Exception if there was an unrecoverable problem
    public static ArrayList receiveMessage(Socket comm_sock,int language) throws IOException {
	ArrayList message;
	try {
            message = (ArrayList)read(comm_sock,language);
            write(comm_sock,OK,language);
            return message;
        } catch(ClassCastException e) {
            write(comm_sock,NOTOK,language);
            return null;
        }
    }

    public static ArrayList receiveMessage(BufferedReader in,PrintWriter out,int language) throws IOException {
        ArrayList message;
	try {
            Object o = read(in,language);
            message = (ArrayList)o;
            write(out,OK,language);
            return message;
        } catch(ClassCastException e) {
            System.err.println("---------> " + e);
            write(out,NOTOK,language);
            return null;
        }
    }
    
    // waits for a message of a particular kind, if a message of another kind is received 
    // it writes a packet with the specific packet request
    public static ArrayList waitForMessage(int typeofmessage,Socket comm_sock) throws IOException {
	return waitForMessage(typeofmessage,comm_sock,POP11);
    }

    public static ArrayList waitForMessage(int typeofmessage,Socket comm_sock,int language) throws IOException {
	//wait for input or return if the device is closed
	for(;;) {
	    ArrayList message = receiveMessage(comm_sock,language); 
	    if (message.get(2) instanceof Integer && ((Integer)message.get(2)) == typeofmessage) {
		write(comm_sock,OK,language);
                return message;
            }
            else
		write(comm_sock,NOTOK,language);
	}
    }
    

    public static void main(String[] args) {
	/*
	try {
 	    System.out.println(parseExpressionCL("(this is 0.9 `(1 ,\"2\"))"));
	    System.out.println(produceListCL(parseExpressionCL("(this is 0.9 `(1,\"2\"))")));
	} catch(Exception e) {
	    System.out.println("Caught: " + e);
	}
	*/

	/*
	 class testclass {
	    public Integer a = 0;
	    public Integer b = 0;
	    
	    public String toString() {
		return a + " " + b;
	    }
	}
	*/

	// test the list parser
	/*
	String start = "[[5 00:00:00 7.59][1 test 'string']]";
	ArrayList v = (ArrayList)Pop11.parseExpression(start);
	String r = produceList(v);
	System.out.println(r + "    " + r.equals(start));	
	*/
	/*
	try {
	    produceList(new File("parsetestout"),parseExpression(new File("parsetestin")));
	} catch (IOException e1) {
	} catch (Pop11SyntaxError e2) {}
	*/

	// test the serialization
	
	//testclass t;

	/*
	try {	    
	    Object o = read(new File("otest"));
	    //	    o = read(new File("otest"));

	    if (o == null)
		System.out.println("NULL");
	    else
		System.out.println(o);
	    write(new File("testout"),o);
	} catch (Exception e) {System.out.println("Caught: " + e);}
	
	*/
	// test socket connection to pop11
		
	try {
	    System.out.println("STARTING");
	    ServerSocket serverSocket = new ServerSocket(10000);
	    System.out.println("LISTENING ON 10000...");
	    while (true) {	    
		Socket comm_sock = serverSocket.accept();
		System.out.println("Client connecting..");
                BufferedReader in = new BufferedReader(new InputStreamReader(comm_sock.getInputStream()));
                PrintWriter out = new PrintWriter(comm_sock.getOutputStream(),false);
                //System.out.println(receiveMessage(in,out,R));
                //sendMessage(0,1,new Pop11List(),in,out,R);
		while (true) {
			System.out.println("Reading...");
		    System.out.println(in.readLine());
		    out.write("0\n");	
		    out.flush();
		    out.write("list(0,3,list(c(\"compilehere\",\"x<-999\")))");
		    		    out.flush();	
		    System.out.println("wroteit");
		}
                /*
                Object m;
                do {
                    System.out.println("Waiting...");
                    m = receiveMessage(0,in,out);
                    System.out.println("Received " + m);
                    if (!(m instanceof ArrayList)) {
                        System.err.println("CCYC was not a list, but a");
                        System.err.println(m.getClass());
                    }
                } while (!m.equals("DONE"));
                System.out.println("Received " + m);		
		comm_sock.close();
                 */
	    }
	} catch (IOException e) {
	    System.err.println("IOError: " + e);
	}
	
        /*
	    ArrayList l = new ArrayList(2);
    ArrayList m = new ArrayList(2);
    m.add(new Word("initfile"));
    m.add("testfile");
    l.add(m);
    ArrayList m2 = new ArrayList(2);
    m2.add(new Word("statsfile"));
    m2.add("testfile2");
    l.add(m2);
    try {
	System.out.println(produceListR(l));
    } catch(Exception e) {System.out.println(e);}
         */
	/*
	try {
	    ComponentSocket serverSocket = new ComponentSocket(10000);
	    while (true) {	    
		Socket comm_sock = serverSocket.accept();
                do {
                    System.out.println("Waiting...");
		    Object o = read(comm_sock,CL);
                    System.out.println("Received " + o);
                    System.out.println("Returning message...");
		    write(comm_sock,o,CL);
                } while (true);
	    }
	} catch (IOException e) {
	    System.err.println("IOError: " + e);
	}
	*/
    }
}
