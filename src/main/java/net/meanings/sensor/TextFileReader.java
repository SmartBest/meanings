/**
 * 
 */
package net.meanings.sensor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author seliv
 *
 */
public class TextFileReader {
	
	BufferedReader br = null;
	
	public TextFileReader(String FileName) {
		try {
			br = new BufferedReader(new FileReader(FileName));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getNextLine() throws IOException {
		String line = br.readLine();
		if (line != null) {
			return line;
		}
		return "<end>";
	}

}
