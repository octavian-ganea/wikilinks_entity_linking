package entity_linking.input_data_pipeline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class WikilinksParser {
	private BufferedReader in;
	private String nextLine;
	
	public WikilinksParser(String filename) throws IOException {
		in = new BufferedReader(new FileReader(filename));
		nextLine = in.readLine();
	}

	public WikilinksSinglePage nextItem() throws IOException {
	    WikilinksSinglePage rez = new WikilinksSinglePage();
		if (!nextLine.startsWith("------------")) {
			System.err.println("Error parsing the current file!!! " + nextLine);
			System.exit(1);
		}
		nextLine = in.readLine();
		while (!nextLine.startsWith("--- Page num:")) {
			nextLine = in.readLine();			
		}
		
		rez.pageNum = Integer.toString(Integer.parseInt(nextLine.substring(nextLine.indexOf(":") + 2)));
		nextLine = in.readLine();
		rez.docId = Integer.parseInt(nextLine.substring(nextLine.indexOf(":") + 2));
		nextLine = in.readLine();
		rez.pageUrl = nextLine.substring(nextLine.indexOf(": ") + 2);

		nextLine = in.readLine(); // --- Mentions:
		nextLine = in.readLine();

		while(!nextLine.startsWith("--- ")) {
			rez.truthMentions.add(new TruthMention(nextLine));
			nextLine = in.readLine();	
			if (nextLine == null) {
				System.err.println("Not proper formatted item.");
				System.exit(1);
			}
		}	
		nextLine = in.readLine();			

		
		StringBuilder allTextSb = new StringBuilder();
		while (nextLine != null && nextLine.compareTo("-------------------------------------") != 0) {
			allTextSb.append(nextLine + "\n");
			nextLine = in.readLine();			
		}
		rez.setAnnotatedText(allTextSb.toString());
				
		return rez;
	}
	
	public boolean hasMoreItems() {
		return nextLine != null && nextLine.startsWith("-----------");
	}
}
