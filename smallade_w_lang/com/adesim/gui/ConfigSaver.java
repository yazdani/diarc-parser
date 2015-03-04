package com.adesim.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import ade.gui.Util;
import ade.gui.UtilUI;

import com.adesim.config.HeaderFetcher;


/** Class in charge of saving the current state of the simulator. */
public class ConfigSaver {
	private static final String EXTENSION = "xml";

	private ADESimMapVis vis;
	public ConfigSaver(ADESimMapVis vis) {
		this.vis = vis;

		JFileChooser fileChooser = createCustomFileChooser();
		int returnVal = fileChooser.showSaveDialog(vis);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				String output = (String) vis.callComponent("getCurrentStateConfigXML");
				
				File file = getFileBasedOnFileChooserWithExtensionAppended(fileChooser);
				writeOutput(file, output);
				
			} catch (Exception e) {
				showFileSavingError("Could not get XML from the server", e);
			}
		}
	}


	private void writeOutput(File file, String xmlString) {
		Writer output = null;
		try {
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e1) {
			showFileSavingError("Could not open file for writing", e1);
			return;
		}

		try	{
			// need to make sure to write in UTF-8 encoding, since that's what
			//    I have in the XML header.  And apparently StringBuilder can give ITS 
			//    output in different encodings, causing a 
			//    "Invalid byte 1 of 1-byte UTF-8 sequence" error
			
			writeOutputHelper(output, HeaderFetcher.getHeaderInfo());
			writeOutputHelper(output, xmlString);
		} catch (IOException e) {
			showFileSavingError("Could not write to XML file", e);
		}

		finally	{
			try	{
				output.close();
			} catch (IOException e) {
				showFileSavingError("Could not close the XML file output stream", e);
			}
		}
	}


	private void writeOutputHelper(Writer output, String string) throws IOException {
		try {
			output.write(new String(string.getBytes(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IOException("Could not convert string to UTF-8 format");
		}
	}


	private void showFileSavingError(String title, Exception e) {
		vis.showErrorMessage(UtilUI.messageInScrollPane(
				"Could not save config file due to " + Util.stackTraceString(e)),
				title);
	}


	private JFileChooser createCustomFileChooser()
	{
		JFileChooser fc = new JFileChooser();
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(
				new FileFilter()
				{ 

					//Accept all directories and EXTENSION files
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						}

						String extension = GetExtension(f);
						if ( (extension != null) && (extension.equals(EXTENSION)) )
							return true;
						else
							return false;
					}

					private String GetExtension(File f)
					{
						String ext = null;
						String s = f.getName();
						int i = s.lastIndexOf('.');

						if (i > 0 &&  i < s.length() - 1) {
							ext = s.substring(i+1).toLowerCase();
						}
						return ext;
					}

					//The description of this filter
					public String getDescription() {
						return "ADESim XML config file (." + EXTENSION + ")";
					}
				}
		);

		return fc;
	}

	private File getFileBasedOnFileChooserWithExtensionAppended(JFileChooser fc)
	{
		String name = fc.getSelectedFile().getAbsolutePath();
		if (   !(name.substring(name.length()-EXTENSION.length()-1)
				.equals("." + EXTENSION))   )
			name += "." + EXTENSION;

		return new File(name);
	}

}
