package com.hrilab;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * This is a custom Ant task for building .proto files into source code.
 *
 * It assumes you have protoc installed.
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class Protoc extends Task {
	private String protobufCompiler = "protoc";
	private boolean failOnError = false;
	private FileResource srcFile = null;
	private Path destDir = null;
	private Path destDirCpp = null;
	private Path destDirPython = null;
	private String minVersion = "0.0.0";

	@Override
	public void execute() throws BuildException {
		if (protocFound()) {
			if (srcFile != null) {
				compileProto(srcFile.toString());
			}
		} else if (failOnError) {
			throw new BuildException("Couldn't find appropriate version of protoc.",
															 getLocation());
		}
	}

//	private String relativizePath(String path) {
//		String pwd = getProject().getBaseDir().toString();
//		if (path.startsWith(pwd)) {
//			return path.substring(1 + pwd.length());
//		}
//		return null;
//	}
	private boolean compileProto(String filename) throws BuildException {
		ArrayList<String> components = new ArrayList<String>();
		components.add(protobufCompiler);
		if (destDir != null) {
			components.add("--java_out=" + destDir);
		}
		if (destDirCpp != null) {
			components.add("--cpp_out=" + destDirCpp);
		}
		if (destDirPython != null) {
			components.add("--python_out=" + destDirPython);
		}
		components.add("--proto_path=" + getProject().getBaseDir().toString());
		components.add(filename);
		String[] temp = components.toArray(new String[0]);

		try {
			Execute exec = new Execute();
			exec.setCommandline(temp);
			int returnCode = exec.execute();
			if (returnCode != 0 && failOnError) {
				throw new BuildException("Failed to compile " + filename, getLocation());
			}
		} catch (IOException ex) {
			Logger.getLogger(Protoc.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		return true;
	}

	/**
	 * Run "protoc --version", both to check for protoc's existence and
	 * to make sure it is a new enough version.
	 * @return
	 */
	private boolean protocFound() {
		final String[] results = {""};
		// Set up a call to protoc
		Execute exec = new Execute(new ExecuteStreamHandler() {
			InputStream cmdOutputStream;

			public void setProcessInputStream(OutputStream out) throws IOException {
			}

			public void setProcessErrorStream(InputStream in) throws IOException {
			}

			public void setProcessOutputStream(InputStream in) throws IOException {
				cmdOutputStream = in;
			}

			public void start() throws IOException {
			}

			public void stop() {
				try {
					results[0] = new java.util.Scanner(cmdOutputStream).useDelimiter("\\A").next();
				} catch (NoSuchElementException _) {
					// protoc not found, no need to panic
				}
			}
		});
		// Run protoc to get the version number
		try {
			String[] line = {protobufCompiler, "--version"};
			exec.setCommandline(line);
			exec.execute();
		} catch (IOException ex) {
//			Logger.getLogger(Protoc.class.getName()).log(Level.INFO, null, ex);
			return false;
		}

		if (results[0].isEmpty()) {
			return false;
		} else if (minVersion.isEmpty()) {
			return true;
		}

		String version = results[0].split(" ")[1].trim();
		String[] foundVer = version.split("\\.");
		String[] requiredVer = minVersion.split("\\.");

		for (int i = 0; i < requiredVer.length; i++) {
			int vReq = Integer.parseInt(requiredVer[i]);
			if (i == foundVer.length && vReq > 0) {
				return false;
			}
			int vFound = Integer.parseInt(foundVer[i]);
			if (vFound > vReq) {
				return true;
			} else if (vFound < vReq) {
				return false;
			}
		}
		return true;
	}

	public void setProtobufcompiler(String val) {
		protobufCompiler = val;
	}

	public void setFailonerror(boolean val) {
		failOnError = val;
	}

	public void setSrcfile(FileResource val) {
		srcFile = val;
	}

	public void setDestdir(Path val) {
		destDir = val;
	}

	public void setDestdirjava(Path val) {
		destDir = val;
	}

	public void setDestdircpp(Path val) {
		destDirCpp = val;
	}

	public void setDestdirpython(Path val) {
		destDirPython = val;
	}

	public void setMinversion(String val) {
		minVersion = val;
	}
}
