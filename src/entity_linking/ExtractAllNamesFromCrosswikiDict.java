package entity_linking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;


// Extracts all the distinct names from p(e|n) dictionary.
// We actually used just the one token names from the entire set of names.
public class ExtractAllNamesFromCrosswikiDict {
	public static void run(String filename)  throws IOException{
		// <string><tab><cprob><space><url>
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = in.readLine();
		int nrLine = 0;
		
		String last_name = null;
		while (line != null && line.length() > 3) {
			if (nrLine % 5000000 == 0)
				System.err.println("loaded " + nrLine);
			nrLine ++;
			StringTokenizer st = new StringTokenizer(line, "\t");

			if (!st.hasMoreTokens()) {
				line = in.readLine();
				continue;
			}
			String name = st.nextToken();
			if (name.length() == 0) {
			    continue;
			}
			
			if (last_name == null || last_name.compareTo(name) != 0) {
				System.out.println(name);
				last_name = name;
			}

			line = in.readLine();
		}
		in.close();		
	}	
}
