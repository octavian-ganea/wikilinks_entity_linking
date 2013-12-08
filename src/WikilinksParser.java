import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class WikilinksParser {
	private BufferedReader in;
	private String next_line;
	
	public WikilinksParser(String filename) throws IOException {
		in = new BufferedReader(new FileReader(filename));
		next_line = in.readLine();
	}

	WikiLinkItem nextItem() throws IOException {
		WikiLinkItem rez = new WikiLinkItem();
		if (!next_line.startsWith("------------")) {
			System.err.println("Error parsing the current file!!! " + next_line);
			System.exit(1);
		}
		next_line = in.readLine();
		while (!next_line.startsWith("--- Page num:")) {
			next_line = in.readLine();			
		}
		
		rez.page_num = Integer.parseInt(next_line.substring(next_line.indexOf(":") + 2));
		next_line = in.readLine();
		rez.doc_id = Integer.parseInt(next_line.substring(next_line.indexOf(":") + 2));
		next_line = in.readLine();
		rez.url = next_line.substring(next_line.indexOf(": ") + 2);

		/*
		next_line = in.readLine(); // --- Num sentences:
		int num_sentences = Integer.parseInt(next_line.substring(next_line.indexOf(":") + 2));
		
		next_line = in.readLine(); // --- Total num sentences so far;
		*/
		next_line = in.readLine(); // --- Mentions:
		next_line = in.readLine();

		while(!next_line.startsWith("--- ")) {
			rez.mentions.add(new Mention(next_line));
			next_line = in.readLine();	
			if (next_line == null) {
				System.err.println("Not proper formatted item.");
				System.exit(1);
			}
		}	
		next_line = in.readLine();			
		/*
		for (int i = 0; i < num_sentences; ++i) {
			if (next_line == null) {
				System.err.println("Not proper formatted item.");
				System.exit(1);			
			}			
			rez.sentences.add(next_line);
			next_line = in.readLine();						
		}
		while(next_line != null && !next_line.startsWith("-----------")) {
			// Phrases like: "unable to determine cannonical charset name for ISO-7659-1 - using ISO-8859-1"
			next_line = in.readLine();						
		}
		*/
		
		StringBuilder allTextSb = new StringBuilder();
		while (next_line != null && next_line.compareTo("-------------------------------------") != 0) {
			allTextSb.append(next_line + "\n");
			next_line = in.readLine();			
		}
		rez.all_text = allTextSb.toString();
		
		rez.cleanText();
		
		return rez;
	}
	
	boolean hasMoreItems() {
		return next_line != null && next_line.startsWith("-----------");
	}
}
