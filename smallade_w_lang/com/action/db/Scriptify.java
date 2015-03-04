/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * Scriptify.java
 *
 * @author Paul Schermerhorn
 */
package com.action.db;
import java.io.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

import org.xml.sax.InputSource;

//import com.sun.org.apache.xml.internal.serialize.OutputFormat;
//import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

// This simple parser allows you to specify Action scripts as non-xml text.
// Comment lines start with '#'.  All tags, except for <var>, <control>, and
// <actspec>, are represented by ":tag [value]" at the start of a line.
// A var is represented as ":var [varname] [vartype]" at the start of a
// line.  Any line that isn't matched to one of the other tags is assumed to
// be an actspec or control--no tag required.
//
// The result is printed to System.out.  It can then be pasted into an
// appropriate xml ActionDB file.  Alternatively, to output a full file,
// pass the "-s" flag for a new script, or the "-p" flag for a new
// primitive, and redirect the output to a new xml file.
//
// Finally, to enter the script via the keyboard:
//
//   java Scriptify -
//
// Enter ^D at the start of a new line to send EOF when finished entering
// script/primitive.
//
// Quickly, the elements that are required for a script are:
//
// :name -- the name
// :var [varname] [vartype] -- the "variables" for the script
// :actspec -- the actions the script calls (primitives or other scripts);
//             note that the ":actspec" tag is optional for Scriptify
// :control -- flow control elements (e.g., if, while)
//             note that the ":control" tag is optional for Scriptify
//
// Other possible elements are:
//
// :locks -- specify which resource locks should be required before executing
// :cost -- the cost of running the script
// :benefit -- the expected benefit
// :posaff -- initial value for positive affect associated with this action
// :negaff -- initial value for negative affect associated with this action
// :minurg -- minimum urgency for this action
// :maxurg -- maximum urgency for this action
// :timeout -- time allotted in which to complete action
// :taskname -- printable description, more informative than :name
// :precond -- predicate list specifying preconditions
// :postcond -- predicate list specifying postconditions
//
// Most of these have default values and need only be specified if you want
// to tweak the goal selection process, etc.
//
// Remember that when referring to parameter variables in the 
// actspecs, they must be preceded with a '?'.  Also note that "local"
// variables (ones that the Action Interpreter should not expect to find in
// the call stack) should be defined with a leading '!'.
// 
// It's possible to specify a script to run using the :exec tag; just give
// :exec [name] [param1] [param2] ...
//
// See the file testScriptify.as for an example that can be parsed by
// Scriptify.
//
public class Scriptify
{
    @SuppressWarnings("deprecation")
    public static void main (String[] args) throws Exception {
        boolean script = false;
        boolean primitive = false;
        boolean gotTask = false;
        boolean initial = true;
        BufferedReader br = null;
        String nextLine, token, execs = new String();
        StringTokenizer strTok;
        StringBuilder sb = new StringBuilder();
        int line = 0;
        int filearg = 0;
        if (args.length != 1) {
            if (args.length != 2 || (! args[0].equals("-s") && ! args[0].equals("-p"))) {
                System.err.println("Usage: java Scriptify [-s|-p] <filename>");
                System.exit(-1);
            } else {
                filearg++;
                if (args[0].equals("-p")) {
                    primitive = true;
                } else if (args[0].equals("-s")) {
                    script = true;
                }
            }
        }
        if (args[filearg].equals("-")) {
            // Read from stdin
            br = new BufferedReader(new InputStreamReader(System.in));
        } else {
            File file = new File(args[filearg]);
            try {
                // First try to read it from the filesystem
                FileReader fr = new FileReader(file);
                br = new BufferedReader(fr);
            } catch (FileNotFoundException fnfe) {
                System.err.println("File not found: " + file.getPath());
                System.exit(-1);
            }
        }
        if (script || primitive) {
            sb.append("\n<type>");
            sb.append("\n<name>entity</name>");
            sb.append("\n<subtypes>");
            sb.append("\n<type>");
            sb.append("\n<name>action</name>");
            sb.append("\n<subtypes>");
            sb.append("\n<type>");
            if (script)
                sb.append("\n<name>script</name>");
            else
                sb.append("\n<name>primitive</name>");
            sb.append("\n<subtypes>");
        }
        try {
            while ((nextLine = br.readLine()) != null) {


		int comment = nextLine.indexOf("#");
		if(comment >= 0) {
			sb.append("\n<!-- ").append(nextLine.substring(comment+1).trim()).append(" -->");
			nextLine = nextLine.substring(0,comment);
		}


                strTok = new StringTokenizer(nextLine);
                line++;
                if (!strTok.hasMoreTokens())
                    continue;
                token = strTok.nextToken();
                if (token.equals(":exec")) {
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no task specified.");
                        System.exit(-1);
                    }
                    gotTask = true;
                    execs = execs + "\n<exec>";
                    while (strTok.hasMoreTokens()) {
                        execs = execs + strTok.nextToken() + " ";
                    }
                    execs = execs + "</exec>";
                } else if (token.equals(":name")) {
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no name specified.");
                        System.exit(-1);
                    }
                    if (! script && ! primitive) {
                        if (sb.length() > 0) {
                            sb.append("\n</type>");
                            print(sb.toString(), true);
                            sb = new StringBuilder();
                        }
                    } else if (initial) {
                        initial = false;
                    } else {
                        sb.append("\n</type>");
                    }
                    sb.append("\n<type>");
                    String name = strTok.nextToken();
                    sb.append("\n<name>").append(name).append("</name>");
                } else if (token.equals(":desc")) {
                    String desc;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no desc specified.");
                        System.exit(-1);
                    }
                    desc = strTok.nextToken();
                    sb.append("\n<desc>").append(desc);
                    while (strTok.hasMoreTokens()) {
                        desc = strTok.nextToken();
                        sb.append(" ").append(desc);
                    }
                    //sb.append("\n</desc>");
                    sb.append("</desc>");
                } else if (token.equals(":var")) {
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no variable name specified.");
                        System.exit(-1);
                    }
                    String name = strTok.nextToken();
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no variable type specified.");
                        System.exit(-1);
                    }
                    String type = strTok.nextToken();
                    sb.append("\n<var>");
                    sb.append("\n<varname>").append(name).append("</varname>");
                    sb.append("\n<vartype>").append(type).append("</vartype>");
                    if (strTok.hasMoreTokens()) {
                        String role = strTok.nextToken();
                        sb.append("\n<role>").append(role).append("</role>");
                    }
                    sb.append("\n</var>");
                } else if (token.equals(":locks")) {
                    String lock;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no lock specified.");
                        System.exit(-1);
                    }
                    lock = strTok.nextToken();
                    sb.append("<locks>").append(lock);
                    while (strTok.hasMoreTokens()) {
                        lock = strTok.nextToken();
                        sb.append(" ").append(lock);
                    }
                    sb.append("\n</locks>");
                } else if (token.equals(":cost")) {
                    Double cost = 0.0;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no cost specified.");
                        System.exit(-1);
                    }
                    try {
                        cost = Double.parseDouble(strTok.nextToken());
                    } catch (NumberFormatException nfe) {
                        System.err.println("ERROR [" + line + "]: error parsing cost.");
                        System.exit(-1);
                    }
                    sb.append("\n<cost>").append(cost).append("</cost>");
                } else if (token.equals(":benefit")) {
                    Double benefit = 0.0;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no benefit specified.");
                        System.exit(-1);
                    }
                    try {
                        benefit = Double.parseDouble(strTok.nextToken());
                    } catch (NumberFormatException nfe) {
                        System.err.println("ERROR [" + line + "]: error parsing benefit.");
                        System.exit(-1);
                    }
                    sb.append("\n<benefit>").append(benefit).append("</benefit>");
                } else if (token.equals(":posaff")) {
                    Double posaff = 0.0;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no posaff specified.");
                        System.exit(-1);
                    }
                    try {
                        posaff = Double.parseDouble(strTok.nextToken());
                    } catch (NumberFormatException nfe) {
                        System.err.println("ERROR [" + line + "]: error parsing posaff.");
                        System.exit(-1);
                    }
                    sb.append("\n<posaff>").append(posaff).append("</posaff>");
                } else if (token.equals(":negaff")) {
                    Double negaff = 0.0;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no negaff specified.");
                        System.exit(-1);
                    }
                    try {
                        negaff = Double.parseDouble(strTok.nextToken());
                    } catch (NumberFormatException nfe) {
                        System.err.println("ERROR [" + line + "]: error parsing negaff.");
                        System.exit(-1);
                    }
                    sb.append("\n<negaff>").append(negaff).append("</negaff>");
                } else if (token.equals(":minurg")) {
                    Double minurg = 0.0;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no minurg specified.");
                        System.exit(-1);
                    }
                    try {
                        minurg = Double.parseDouble(strTok.nextToken());
                    } catch (NumberFormatException nfe) {
                        System.err.println("ERROR [" + line + "]: error parsing minurg.");
                        System.exit(-1);
                    }
                    sb.append("\n<minurg>").append(minurg).append("</minurg>");
                } else if (token.equals(":maxurg")) {
                    Double maxurg = 0.0;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no maxurg specified.");
                        System.exit(-1);
                    }
                    try {
                        maxurg = Double.parseDouble(strTok.nextToken());
                    } catch (NumberFormatException nfe) {
                        System.err.println("ERROR [" + line + "]: error parsing maxurg.");
                        System.exit(-1);
                    }
                    sb.append("\n<maxurg>").append(maxurg).append("</maxurg>");
                } else if (token.equals(":timeout")) {
                    Long timeout = 0L;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no timeout specified.");
                        System.exit(-1);
                    }
                    try {
                        timeout = Long.parseLong(strTok.nextToken());
                    } catch (NumberFormatException nfe) {
                        System.err.println("ERROR [" + line + "]: error parsing timeout.");
                        System.exit(-1);
                    }
                    sb.append("\n<timeout>").append(timeout).append("</timeout>");
                } else if (token.equals(":taskname")) {
                    String taskname;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no taskname specified.");
                        System.exit(-1);
                    }
                    taskname = strTok.nextToken();
                    sb.append("<taskname>").append(taskname);
                    while (strTok.hasMoreTokens()) {
                        taskname = strTok.nextToken();
                        sb.append(" ").append(taskname);
                    }
                    sb.append("\n</taskname>");
                } else if (token.equals(":precond")) {
                    String precond;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no precond specified.");
                        System.exit(-1);
                    }
                    precond = strTok.nextToken();
                    sb.append("<precond>").append(precond);
                    while (strTok.hasMoreTokens()) {
                        precond = strTok.nextToken();
                        sb.append(" ").append(precond);
                    }
                    sb.append("\n</precond>");
                } else if (token.equals(":postcond")) {
                    String postcond;
                    if (!strTok.hasMoreTokens()) {
                        System.err.println("ERROR [" + line + "]: no postcond specified.");
                        System.exit(-1);
                    }
                    postcond = strTok.nextToken();
                    sb.append("<postcond>").append(postcond);
                    while (strTok.hasMoreTokens()) {
                        postcond = strTok.nextToken();
                        sb.append(" ").append(postcond);
                    }
                    sb.append("\n</postcond>");
                } else if (nextLine.startsWith("#")) {
                    // PWS: duplicate?
                    nextLine = nextLine.substring(1);
                    sb.append("\n<!-- ").append(nextLine).append(" -->");
                } else {
                    String tag = "actspec";
                    if (token.equals("if") || token.equals("then") ||
                        token.equals("elseif") || token.equals("else") ||
                        token.equals("endif") || token.equals("while") ||
                        token.equals("do") || token.equals("endwhile") ||
                        token.equals("return") || token.equals("endreturn") ||
                        token.equals("and") || token.equals("or"))
                        tag = "control";
                    sb.append("\n<").append(tag).append(">").append(nextLine).append("</").append(tag).append(">");
                }
            }
            sb.append("\n</type>");
        } catch (IOException ioe) {
            System.err.println("Exception parsing config file line " + line + ": " + ioe);
            System.exit(-1);
        } 
        if (script || primitive) {
            sb.append("\n</subtypes>");
            sb.append("\n</type>");
            sb.append("\n</subtypes>");
            sb.append("\n</type>");
            if (gotTask) {
                sb.append("\n<type>");
                sb.append("\n<name>tasks</name>");
                sb.append(execs);
                sb.append("\n</type>");
            }
            sb.append("\n</subtypes>");
            sb.append("\n</type>");
        }
        print(sb.toString(), (!script && !primitive));
    }

    static void print(String in, boolean decl) throws Exception {
        StringReader s = new StringReader(in);
        Document d = null;
        try {
            d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(s));
        } catch (Exception e) {
            System.err.append("Exception parsing generated XML: "+e);
            System.out.println(in);
            return;
        }
        OutputFormat f = new OutputFormat(d);
        f.setIndenting(true);
        f.setIndent(2);
        f.setStandalone(true);
        f.setEncoding("ISO-8859-1");
        f.setOmitXMLDeclaration(decl);
        XMLSerializer x = new XMLSerializer(System.out, f);
        System.out.println();
        x.serialize(d);
    }
}
