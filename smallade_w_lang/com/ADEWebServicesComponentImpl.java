/** 
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 * Last update: May 2010
 *
 * ADEWebServicesComponentImpl.java
 */
package com;
//import sun.misc.BASE64Decoder; //I know this is not a 'good' thing to do
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.lang.reflect.InvocationTargetException;


import ade.ADEComponentImpl;

/**
 * The implementation of the {@link ade.ADEWebServicesComponent ADEWebServicesComponent} interface.
 * The <tt>ADEWebServicesComponentImpl</tt> provides the minimum functionality necessary
 * for an <tt>ADEComponent</tt>, to expose it's methods through WebServices (namely XML-RPC)
 * <p>
 * Infrastructure mechanisms include:
 * <ul>
 * <li>Providing an minimal internal HTTP server for transport of XML-RPC messaging
 * <li>Command-line execution of XML-RPC calls (For Scripting/Configuring ADEComponent Startup)
 * <li>Processing XML-RPC messages
 * <li>Controlling Access via: Basic Access Authentication or URI GET variables(username/password), IP ACLs, per user function access
 * </ul>
 * @author Jack Harris
 * @see ade.ADEComponent ADEComponent
 *
 */
abstract public class ADEWebServicesComponentImpl extends ADEComponentImpl implements ADEWebServicesComponent {

	protected boolean webverbose = false;
	private static int webServicesPort = 8080;
	private static boolean autostartWebServices = false;
	private static String cli_XMLRPC_request = "";
	private static boolean cli_exit = false;
	private XMLRPC_HttpServer XMLRPC_HttpServer = null;
	private final String xmlrpctypes = "nil,i4,int,boolean,double,string,dateTime.iso8601,base64,struct,array";

	/**
	 * This method determines whether or not a method is allowed to be executed by a given person from a particual IP
	 * @param methodName
	 * @param ip
	 * @param username Requesting Username
	 * @param password Password provided by the user
	 * @return
	 */
	abstract protected boolean isAuthorized(String methodName, String ip, String username, String password);

	/**
	 * Helper function that will parse an XML String and return the inner string of a tag
	 * @param entry Source xml string (haystack)
	 * @param tagName Name of the tag without '<','>' (needle)
	 * @param start offset in the entry string
	 * @return the string within the tag
	 * @throws RemoteException
	 */
	private final String innerXML(String entry, String tagName, int start) throws RemoteException {
		int startIndex = entry.indexOf("<" + tagName, start);
		if (startIndex == -1) {
			throw new RemoteException("A " + tagName + " tag was missing for: " + entry + " starting at position " + start);
		}
		int startIndexEnd = entry.indexOf(">", startIndex);
		int startIndexEnd2 = entry.indexOf("/>", startIndex);
		if ((startIndexEnd2 != -1) && (startIndexEnd2 < startIndexEnd)) {
			return "";
		}
		int endIndex = entry.indexOf("</" + tagName + ">", startIndex);
		if (endIndex == -1) {
			throw new RemoteException("A closing " + tagName + " tag was missing for: " + entry);
		}

		return entry.substring(startIndexEnd + 1, endIndex);
	}

	/**
	 * Creates a String representation of an XML-RPC response as a reply the a methodCall
	 * @param o Object to be encoded
	 * @param fault whether or not this is a fault (error) message
	 * @return a xml-rpc response string 
	 */
	protected String serializeXMLRPC_Response(Object o, boolean fault) {
		String xmlrpc_type = "";
		//http://ws.apache.org/xmlrpc/types.html
		if (o instanceof Integer) {
			xmlrpc_type = "int";
		} else if (o instanceof Boolean) {
			xmlrpc_type = "boolean";
			if ((Boolean) o == true) {
				o = 1;
			} else {
				o = 0;
			}
		} else if (o instanceof Double) {
			xmlrpc_type = "double";
		} else if (o instanceof String) {
			xmlrpc_type = "string";
			o = o.toString().replaceAll("<", "("); //TODO encode this string properly
			o = o.toString().replaceAll(">", ")");
		} else if (o instanceof java.util.Date) {
			xmlrpc_type = "dateTime.iso8601";
		} else if (o instanceof byte[]) {
			xmlrpc_type = "base64";
			try {
				o = new Base64Coder().encode((byte[]) o);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else if (o instanceof java.util.Map) {
			xmlrpc_type = "struct";
		} else if (o instanceof java.util.List) {
			xmlrpc_type = "array";
		} else {
			xmlrpc_type = "string"; //default to the toString
		}
		String retxml = "<?xml version=\"1.0\"?><methodResponse>";
		if (fault) {
			retxml += "<fault><value>";
		} else {
			retxml += "<params><param><value>";
		}
		if (o == null) {
			retxml += "<nil/>";
		} else {
			retxml += "<" + xmlrpc_type + ">" + o + "</" + xmlrpc_type + ">";
		}
		if (fault) {
			retxml += "</value></fault></methodResponse>";
		} else {
			retxml += "</value></param></params></methodResponse>";
		}
		//System.out.println(retxml);
		return retxml;

	}

	/**
	 * Transforms the value from the XML-RPC type to the respective Java type
	 * @param type i4,int,boolean,double,string,dateTime.iso8601(not implemented),base64,struct(not implemented),array(not implemented) 
	 * @param value value to be converted
	 * @return an Object of the type specified in the xmlrpctype parameter
	 */
	protected Object XML_RPC_Type_To_JavaType(String xmlrpctype, String value) {
		if (xmlrpctype.equals("i4") || xmlrpctype.equals("int")) {
			return new Integer(value);
		} else if (xmlrpctype.equals("boolean")) {
			if (value.trim().equals("1")) {
				return new Boolean(true);
			}
			return new Boolean(value);
		} else if (xmlrpctype.equals("double")) {
			return new Double(value);
		} else if (xmlrpctype.equals("nil")) {
			return null;
		} else if (xmlrpctype.equals("string")) {
			return value;
		} else if (xmlrpctype.equals("dateTime.iso8601")) {
			//DateFormat df = new DateFormat();
			//return new DateFormat().parse(value);
		} else if (xmlrpctype.equals("base64")) {
			try {
				return new Base64Coder().decode(value.trim().replaceAll("\n", ""));// new sun.misc.BASE64Decoder().decodeBuffer(value.trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return value;
			}
		} else if (xmlrpctype.equals("struct")) {
			//TODO: Parse struct and create Map
			//      params.add(new java.util.Map(value));
		} else if (xmlrpctype.equals("array")) {
			//TODO: Parse array and create List
			//      params.add(new java.util.List(value));
		}
		return value;
	}

	/**
	 * Parses a methodCall XML-RPC encoded string and returns back a container class MethodCall
	 * @param xml xml-rpc methodCall string
	 * @return MethodCall has the name, parameters values and types of the method
	 */
	@SuppressWarnings("rawtypes")
	protected MethodCall parse_XML_RPC_Method_Call(String xml) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, RemoteException {
		/*<?xml version="1.0"?>
		<methodCall>
		<methodName>examples.getStateName</methodName>
		<params>
		<param><value><i4>40</i4></value></param>
		</params>
		</methodCall>
		 */
		String strMethodCall = innerXML(xml, "methodCall", 0);
		String strMethodName = innerXML(strMethodCall, "methodName", 0);
		String strParams = innerXML(strMethodCall, "params", 0);
		ArrayList<Object> params = new ArrayList<Object>();
		ArrayList<Class> paramTypes = new ArrayList<Class>();
		if (!strParams.trim().isEmpty()) {
			int lastFound = -1;
			while (strParams.indexOf("param", lastFound) > -1) {
				String p = innerXML(innerXML(strParams, "param", lastFound), "value", 0);
				for (String type : xmlrpctypes.split(",")) {
					if (p.indexOf("<" + type + ">") > -1) {
						String value = innerXML(p, type, 0);
						if (type.equals("i4") || type.equals("int")) {
							paramTypes.add(Integer.TYPE);
						} else if (type.equals("boolean")) {
							paramTypes.add(Boolean.TYPE);
						} else if (type.equals("double")) {
							paramTypes.add(Double.TYPE);
						} else if (type.equals("string")) {
							paramTypes.add(String.class);
						} else if (type.equals("dateTime.iso8601")) {
							paramTypes.add(java.util.Date.class);
						} else if (type.equals("base64")) {
							paramTypes.add(byte[].class);
							//} else if (type.equals("struct")){
							//  paramTypes.add(java.util.Map.class);
							//} else if (type.equals("array")){
							//  paramTypes.add(java.util.List.class);
						} else {
							params.clear();
							params.add(type + " is not supported by ADEWebServices");
							System.err.println(type + " is not supported by ADEWebServices");
							return new MethodCall("fault", null, params.toArray());
						}
						Object b = XML_RPC_Type_To_JavaType(type, value);
						if (b == null) {
							System.out.println("something was null....");
						}
						params.add(b);
						break;
					}
				}
				if (lastFound == strParams.indexOf("param", lastFound)) {
					break;
				}
				lastFound = strParams.indexOf("/param", lastFound) + 6;
			}
		}
		if (webverbose) {
			System.out.println("Method: " + strMethodName + "\nparameter types: " + paramTypes + "\nparameter values: " + params);
		}
		Class[] par = new Class[params.size()];
		for (int i = 0; i < params.size(); i++) {
			par[i] = (Class) paramTypes.get(i);
		}
		return new MethodCall(strMethodName, par, params.toArray());
	}

	/**
	 * Container class the can hold a specification for a method to execute.
	 * @author jackharris
	 *
	 */
	@SuppressWarnings("rawtypes")
	protected class MethodCall {

		public String methodName = "";
		Object[] parameters = null;
		Class[] parameterTypes = null;

		public MethodCall(String methodName, Class[] parameterTypes, Object[] parameters) {
			this.methodName = methodName;
			this.parameterTypes = parameterTypes;
			this.parameters = parameters;
		}
	}

	/**
	 * Uses reflection to call a method matching the characteristics of the provide MethodCall 
	 * @param methodCall
	 * @return an Object of return value of the requested method
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws RemoteException
	 */
	protected Object exectuteMethodCall(MethodCall methodCall) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, RemoteException {
		if (methodCall.methodName.equals("fault")) {
			return methodCall.parameters[0]; //return the error value
		}
		String myType = (String) System.getProperties().get("component");
		@SuppressWarnings("rawtypes")
		Class myclass = null;
		try {
			myclass = Class.forName(myType);
		} catch (Exception e) {
			throw new RemoteException("Class definition for server " + myType + " not found, aborting.");
		}
		@SuppressWarnings("unchecked")
		java.lang.reflect.Method mthd = myclass.getMethod(methodCall.methodName, methodCall.parameterTypes);
		return mthd.invoke(this, methodCall.parameters);
	}

	/**
	 * 1. Parse XML -> MethodCall (If parse fails return fault message)
	 * 2. Check Authorization (return xml-rpc fault message on failure)
	 * 3. Execute Method Call (return xml-rpc fault message on exception)
	 * 4. Encode return value and return response
	 * @param request xml-rpc methodCall string
	 * @param ip requesting IP address
	 * @param user requesting username
	 * @param password provided password of the requesting user  
	 * @return xml-rpc response string
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws RemoteException
	 */
	protected String process_XML_RPC_Method_Call(String request, String ip, String user, String password) throws RemoteException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		String xmlresponse = "";
		MethodCall methodCall = parse_XML_RPC_Method_Call(request);
		if (methodCall.methodName.equals("fault")) {
			xmlresponse = serializeXMLRPC_Response(methodCall.parameters[0], true);
		} else {
			if (isAuthorized(methodCall.methodName, ip, user, password)) {
				try {
					Object responseObject = exectuteMethodCall(methodCall);
					xmlresponse = serializeXMLRPC_Response(responseObject, false);
				} catch (Exception e) {
					xmlresponse = serializeXMLRPC_Response(e, true);
				}
			} else {
				xmlresponse = serializeXMLRPC_Response("Unauthorized", true);
			}
		}
		if (webverbose) {
			System.out.println("Sending:" + xmlresponse);
		}
		return xmlresponse;
	}

	/**
	 * Starts a HTTP service on the provided port that brokers XML-RPC requests
	 * @param port the port to run the web server on
	 * @throws IOException
	 */
	protected void startWebServices(int port) throws RemoteException, IOException {
		webServicesPort = port;
		final int p = port;
		new Thread() {

			@Override
			public void run() {
				XMLRPC_HttpServer = new XMLRPC_HttpServer(p);
			}
		}.start();
	}

	/**
	 * If is a HTTP service running, it is stopped (freeing the port binding)
	 */
	protected void stopWebServices() {
		if (XMLRPC_HttpServer != null) {
			XMLRPC_HttpServer.stop();
			XMLRPC_HttpServer = null;
		}
	}

	//Modified version of
	//http://java.sun.com/developer/technicalArticles/Security/secureinternet/
	private class XMLRPC_HttpServer {

		private ServerSocket listen = null;

		/**
		 * unbinds the port used by the XMLRPC_HttpServer
		 */
		public void stop() {
			if (listen != null) {
				try {
					listen.close();
				} catch (IOException e) {
					System.err.println("Exception (XMLRPC_HttpServer.stop): " + e.getMessage());
					e.printStackTrace();
				}
			}
		}

		/**
		 * Starts listening and processing request on the provided port
		 * @param port
		 */
		public XMLRPC_HttpServer(int port) { //throws Exception{
			try {
				listen = new ServerSocket(port);
				while (true) {
					Socket client = listen.accept();
					new ProcessConnection(client); //new thread
				}
			} catch (Exception e) {
				System.err.println("Exception (XMLRPC_HttpServer.run): " + e.getMessage());
			}
		}
	}
	static Integer sessionID = 0;
  final Boolean mutex_sessionID = new Boolean(true);
	private class ProcessConnection extends Thread {

		Socket client;
		BufferedReader is;
		InputStreamReader isr;
		DataOutputStream os;

		public ProcessConnection(Socket s) { // constructor
			client = s;
			try {
				isr = new InputStreamReader(client.getInputStream());
				is = new BufferedReader(isr);
				os = new DataOutputStream(client.getOutputStream());
			} catch (IOException e) {
				System.out.println("Exception (ProcessConnection): " + e.getMessage());
			}
			this.start(); // Thread starts here...this start() will call run()
		}

		/**
		 * Split the source into two strings at the first occurrence of the splitter
		Subsequent occurrences are not treated specially, and may be part of the second string.
		 *
		 * @param source
		 *        The string to split
		 * @param splitter
		 *        The string that forms the boundary between the two string returned.
		 * @return An array of two strings split from source by splitter.
		 */
		private String[] splitFirst(String source, String splitter) {
			// hold the results as we find them
			Vector rv = new Vector();
			int last = 0;
			int next = 0;
			// find first splitter in source
			next = source.indexOf(splitter, last);
			if (next != -1) { // isolate from last thru before next
				rv.add(source.substring(last, next));
				last = next + splitter.length();
			}
			if (last < source.length()) {
				rv.add(source.substring(last, source.length()));
			}
			// convert to array
			return (String[]) rv.toArray(new String[rv.size()]);
		}

		/**
		 * Processes XMLRPC Method calls
		 * Method Calls: GET/POST of xml-rpc document
		 *               QueryString: xmlrpc=xml-rpc-document-string
		 * Security: Basic Access Authentication (user:pass base64 encoding in HTTP packet)
		 *           QueryString: username=name&password=passs
		 *
		 */
		@Override
		public void run() {
			int mysession = 0;
			try {
				synchronized (mutex_sessionID) {
					if (sessionID > 999999) {
						sessionID = 0;
					} else {
						sessionID++;
					}
					mysession = sessionID.intValue();
				}
				// get a request and parse it.
				StringBuffer requestBuff = new StringBuffer();
				String request = "";
				String strLine = null;
				String queryString = "";
				String user = "";
				String password = "";
				String ip = client.getRemoteSocketAddress().toString();
				if (webverbose) {
					System.out.println(mysession + " -- " + ip + " sending information");
				}
				int timeout = 10;
				while (!is.ready() && timeout > 0) { //give the sender some time to send
					try {
						Thread.sleep(100);
						timeout--;
					} catch (Exception ex) {
					}
				}
				if (timeout == 0) {
					System.err.println("timed out");
				}
				while (is.ready()) {
					try {
						strLine = is.readLine();
					} catch (Exception ex) {
						System.err.println(ex);
					}
					if (strLine != null) {
						if (webverbose) {
							System.out.println(mysession + " -- " + "--> " + strLine);
						}
						if (strLine.length() > 3) {
							if ((strLine.length() > 2 && strLine.substring(0, 3).equals("GET"))
											|| (strLine.length() > 3 && strLine.substring(0, 4).equals("POST"))) {
								queryString = strLine.split(" ")[1];
							} else if (strLine.length() > 19 && strLine.substring(0, 20).equals("Authorization: Basic")) {
								String authorization = strLine.split("Basic")[1].trim();
								String decodedAuthorization = new String(new Base64Coder().decode(authorization)); //new BASE64Decoder().decodeBuffer(authorization));
								String[] userpass = decodedAuthorization.split(":");
								user = userpass[0];
								password = userpass[1];
							} else if (strLine.contains("Content-Length:")) {
								requestBuff.append(strLine);
								requestBuff.append("\n");

								//try to get the content
								int totalLeftToRead = new Integer(strLine.split(":")[1].trim()) - 2; //2 required for "\r\n between header and content";
								System.out.println ("clear \\ r \\ n " +  is.readLine() + "done");
								int bytecount = 0;
								int x = 0;
								//while ( (x = client.getInputStream().read()) != -1 && bytecount < len){
								//	requestBuff.append(x);
								//	bytecount++;
								//	System.out.print(x);
								//}
								if (webverbose) {
									System.out.println("------attempting to read " + totalLeftToRead  );
								}

								char[] data = new char[totalLeftToRead];
								is.read(data, 0, totalLeftToRead); //attempt to read it all
								String strContent = new String(data).trim();
								requestBuff.append(strContent);
								if (webverbose) {
									System.out.println(mysession + " ++ " + "-->" + strContent);
								}
								totalLeftToRead = totalLeftToRead - strContent.length();
								System.out.println("Read: " + strContent.length());
								while (strContent.length() > 0 && totalLeftToRead > 0) { //continue if you got at least something last time and there is more
									if (webverbose) {
										System.out.println("------attempting to read more----- totalLeftToRead:" + totalLeftToRead);
									}

									Thread.sleep(300); //give the buffer time to refill
									is.read(data, 0, totalLeftToRead); //attempt to read the rest
									strContent = new String(data).trim();
									System.out.println("Read: " + strContent.length());
									totalLeftToRead = totalLeftToRead - strContent.length();
									requestBuff.append(strContent);
									if (webverbose) {
										System.out.println(mysession + " ++ " + "-->" + strContent);
									}
								}
								break;
							}
						}
					}
					requestBuff.append(strLine);
					requestBuff.append("\n");
				}
				request = requestBuff.toString();
				requestBuff.setLength(0); //Explicitly free
				//Pattern p = Pattern.compile("\\<base64\\>.*</base64\\>",Pattern.DOTALL);
				//System.out.println("queryString: " +	p.matcher(queryString).replaceAll("base 64 data omitted"));

				//
				//Empty request
				//if (request.isEmpty()) return;

				boolean corruptedUpload = false;

				int b64start = queryString.indexOf("<base64>");
				int b64end = queryString.indexOf("</base64>");
				if (b64start > -1 && b64end > -1) {
					if (webverbose) {
						System.out.print(mysession + " -- " + "queryString: " + queryString.substring(0, b64start));
						System.out.print("base 64 data omitted");
						System.out.println(queryString.substring(b64end));
					}
				} else if (b64start > -1) { //correputed upload
					if (webverbose) {
						System.out.print(mysession + " -- " + queryString.substring(0, b64start));
						System.out.print("base 64 data omitted");
						System.out.println("NO CLOSING base 64 tag -- corrupted queryString upload");
					}
					corruptedUpload = true;
				} else {
					if (webverbose) {
						System.out.println(mysession + " -- " + "queryString: " + queryString);
					}
				}

				if (queryString.indexOf("=") > -1) {
					String[] items = queryString.split("&");
					for (int i = 0; i < items.length; i++) {
						String[] item = splitFirst(items[i], "=");
						String name = item[0].toLowerCase();
						String value = item[0];
						if (name.equals("user") || name.equals("username")) {
							user = value;
						} else if (name.equals("pass") || name.equals("password")) {
							password = value;
						} else if (name.equals("xmlrpc")) {
							request = items[i];
						}
					}
					request = request.replaceAll("%3E", ">");
					request = request.replaceAll("%2F", "/");
					request = request.replaceAll("%3C", "<");
				} else {
					int b64start2 = request.indexOf("<base64>");
					int b64end2 = request.indexOf("</base64>");
					if (b64start2 > -1 && b64end2 > -1) {
						if (webverbose) {
							System.out.print(mysession + " -- " + request.substring(0, b64start2));
							System.out.print("base 64 data omitted");
							System.out.println(request.substring(b64end2));
						}
					} else if (b64start2 > -1) { //correputed upload
						if (webverbose) {
							System.out.print(mysession + " -- " + request.substring(0, b64start2));
							System.out.print("base 64 data omitted");
							System.out.println("NO CLOSING base 64 tag -- corrupted upload");
						}
						corruptedUpload = true;
					} else {
						if (webverbose) {
							System.out.println(mysession + " -- " + request);
						}
					}

					//System.out.println(p.matcher(request).replaceAll("base 64 data omitted"));
					//System.out.println(request);
				}

				if (corruptedUpload) {
					try {
						System.out.println(mysession + " -- " + "Reportinging Error");
						shipHttpMessage(os, serializeXMLRPC_Response("Corrupted Upload", true));
					} catch (Exception ex) {
						System.err.println(mysession + "Could not report the corrupted upload due to :" + ex);
					}
				} else {
					//Python's ServerProxy.xmlrpclib seems to leave off the
					//closing methodcall tag
					if (request.indexOf("<methodCall>") > -1 && request.indexOf("</methodCall>") == -1) {
						request += "</methodCall>";
						shipHttpMessage(os, process_XML_RPC_Method_Call(request, ip, user, password));
					} else if (request.indexOf("<methodCall>") == -1) {
						System.err.println(mysession + " -- " + "Missing Method Call Tag????");
						System.err.println(mysession + " -- " + request);
						shipHttpMessage(os, serializeXMLRPC_Response("Missing MethodCall Tag", true));
					} else {
						shipHttpMessage(os, process_XML_RPC_Method_Call(request, ip, user, password));
					}
				}
				client.close();
			} catch (Exception e) {
				try {
					shipHttpMessage(os, serializeXMLRPC_Response(e.getMessage(), true));
				} catch (Exception ex2) {
					System.err.println(mysession + " -- " + "Could not report the following error."
									+ e.getMessage() + "\nDue to :" + ex2);
				}
				System.err.println(mysession + " -- " + "Exception (ProcessConnection:run): " + e.getMessage());
				e.printStackTrace();
			}
		}
		/*
		public void shipUnauthorizedHttpMessage(DataOutputStream out, String reply) throws Exception {
		out.writeBytes("HTTP/1.0 401 Authorization Required\r\n");
		out.writeBytes("Server: HTTPd/1.0\r\n");
		//Date: Sat, 27 Nov 2004 10:18:15 GMT
		out.writeBytes("WWW-Authenticate: Basic realm=\"Secure Area\"\r\n");
		out.writeBytes("Content-Type: text/html\r\n");
		String msg = serializeXMLRPC_Response("401 Unauthorized", true);
		out.writeBytes("Content-Length: " + msg.length() + "\r\n\r\n");
		out.write(msg.getBytes());
		out.flush();
		}
		 */

		public void shipHttpMessage(DataOutputStream out, String reply) {
			try {
				out.writeBytes("HTTP/1.0 200 OK\r\n");
				out.writeBytes("Content-Length: " + reply.length() + "\r\n");
				out.writeBytes("Content-Type: text/xml\r\n\r\n");
				out.write(reply.getBytes());
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					out.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/** Utility class that encodes and decodes byte arrays in Base64 representation.
	 * Uses underlying functionality in the java.util.prefs package of the Java API.
	 * Note that this is slightly different from RFC 2045; ie, there are no CRLFs in the encoded string.
	 * Should be thread safe.
	 * Requires Java 1.4 or better.
	 *
	 * http://forums.sun.com/thread.jspa?threadID=477461&start=15&tstart=0
	 */
	private class Base64Coder extends java.util.prefs.AbstractPreferences {

		private String store;
		//private static Base64Coder instance=new Base64Coder();

		/**Hide the constructor; this is a singleton. */
		public Base64Coder() {
			super(null, "");
		}

		/**Given a byte array, return its Base64 representation as a String. */
		public synchronized String encode(byte[] b) {
			this.putByteArray(null, b);
			return this.get(null, null);
		}

		/**Given a String containing a Base64 representation, return the corresponding byte array. */
		public synchronized byte[] decode(String base64String) {
			this.put(null, base64String);
			return this.getByteArray(null, null);
		}

		@Override
		public String get(String key, String def) {
			return store;
		}

		@Override
		public void put(String key, String value) {
			store = value;
		}

		//Other methods required to implement the abstract class;  these methods are not used.
		@Override
		protected java.util.prefs.AbstractPreferences childSpi(String name) {
			return null;
		}

		@Override
		protected void putSpi(String key, String value) {
		}

		@Override
		protected String getSpi(String key) {
			return null;
		}

		@Override
		protected void removeSpi(String key) {
		}

		@Override
		protected String[] keysSpi() throws java.util.prefs.BackingStoreException {
			return null;
		}

		@Override
		protected String[] childrenNamesSpi() throws java.util.prefs.BackingStoreException {
			return null;
		}

		@Override
		protected void syncSpi() throws java.util.prefs.BackingStoreException {
		}

		@Override
		protected void removeNodeSpi() throws java.util.prefs.BackingStoreException {
		}

		@Override
		protected void flushSpi() throws java.util.prefs.BackingStoreException {
		}
		/** Just used for simple unit testing. Remove as desired.
		public static void main(String[] args)throws Exception
		{
		String s=args[0];
		System.out.println("Start:"+s);
		String es=Base64Coder.encode(s.getBytes("UTF8"));
		System.out.println("Encoded:"+es);
		System.out.println("Decoded:"+new String(Base64Coder.decode(es)));

		}
		 */
	}

	/** Parse additional command-line arguments. Needs to return <tt>true</tt>
	 * if parse is successful, <tt>false</tt> otherwise. If additional arguments
	 * are enabled, please make sure to add them to the {@link
	 * #additionalUsageInfo} method to display them from the command-line help.
	 * @param args The additional arguments for this server
	 * @return <tt>true</tt> if additional arguments exist and were parsed
	 * correctly, <tt>false</tt> otherwise
	 *
	 *ADEWebServices looks for:
	 *<ul>
	 *	<li>--web-services (-www) port</li>
	 *	<li>--cli-xmlrpc (-cli) requestXMLRPCString</li>
	 * <li>--cli-xmlrcp-exit (-clix) requestXMLRPCString</li>
	 *</ul>
	 */
	@Override
	protected boolean parseadditionalargs(String[] args) {
		try {
			for (int i = 0; i < args.length; i++) {
				if ((args[i].toLowerCase().indexOf("--web-services") > -1)
								|| (args[i].toLowerCase().indexOf("-www") > -1)) {
					if (args[i].indexOf("=") > -1) {
						String[] portInfo = args[i].split("=");
						webServicesPort = new Integer(portInfo[1]);
					} else {
						i++;
						webServicesPort = new Integer(args[i]);
					}
					autostartWebServices = true;
				} else if ((args[i].toLowerCase().indexOf("--cli-xmlrpc-exit") > -1)
								|| (args[i].toLowerCase().indexOf("-clix") > -1)) {
					i++;
					cli_XMLRPC_request = args[i];
					cli_exit = true;
				} else if ((args[i].toLowerCase().indexOf("--cli-xmlrpc") > -1)
								|| (args[i].toLowerCase().indexOf("-cli") > -1)) {
					i++;
					cli_XMLRPC_request = args[i];
					cli_exit = false;
				}
			}
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	/** Provides command line argument descriptions.
	 * @return command line argument switches and descriptions */
	@Override
	protected String additionalUsageInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("     -cli   --cli-xmlrpc xml-request-string      <executes the xml-request method and responds on stdout>\n");
		sb.append("     -clix  --cli-xmlrpc-exit xml-request-string <executes the xml-request method, responds on stdout and exits>\n");
		sb.append("     -www   --web-services port                  <starts a xml-rpc http server on the provided port>\n");
		return sb.toString();
	}

	/** Constructor: start an httpserver to listen for XML-RPC communication or
	 * processes command-line XML-RPC messages depending on startup parameters used
	 * (see additionalUsageInfo).
	 */
	protected ADEWebServicesComponentImpl() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, RemoteException {
		super();
		try {
			if (!cli_XMLRPC_request.isEmpty()) {
				System.out.println(process_XML_RPC_Method_Call(cli_XMLRPC_request, "127.0.0.1", "", ""));
				if (cli_exit) {
					System.exit(0);
				}
			} else if (autostartWebServices) {
				this.startWebServices(webServicesPort);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RemoteException(ioe.getMessage());
		}
	}
}

