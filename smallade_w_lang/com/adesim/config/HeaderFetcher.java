package com.adesim.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Utility to return SIM_CONFIG_HEADER.txt as a String */
public class HeaderFetcher {
	
	public static String getHeaderInfo() {
		StringBuilder out = new StringBuilder();
		
		try {
			BufferedReader headerReader = new BufferedReader(
					new InputStreamReader(HeaderFetcher.class.getResourceAsStream("SIM_CONFIG_HEADER.txt")));
			String tmp;
			while ( (tmp = headerReader.readLine()) != null ) {
				out.append(tmp + "\n");
			}
			return out.toString();

		} catch (Exception e) {
			return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + 
					"\n<!-- {could not get instruction text from com/adesim/config/SIM_CONFIG_HEADER.txt} -->" +
					"\n\n";
		}
	}
	
}
