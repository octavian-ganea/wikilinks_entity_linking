import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


// Computes p(M.ent!=dummy | n ) = 1 - p(M.ent=dummy | n) probabilities for all names n by looking at
// all files from the Wikilinks corpus and counting the number of docs where n the anchor text of an 
// Wikipedia link over the number of docs in which n appears.

public class DummyEntityProbabilities {
	public static HashMap<String, DummyNameProb> allNames = null;
	
	private static class DummyNameProb {
		String name;
		int numDocsWithName = 0;
		int numDocsWithAnchorName = 0;

		public DummyNameProb(String name) {
			this.name = name;
			numDocsWithName = 0;
			numDocsWithAnchorName = 0;		
		}
	}	
	public static void compute(String allNamesFile, String corpusDirectory) throws IOException {
		// Loading all names.
		System.err.println("Loading all names ...");
		allNames = new HashMap<String, DummyNameProb>();
		BufferedReader in = new BufferedReader(new FileReader(allNamesFile));

		String line = in.readLine();
		while (line != null && line.length() > 0) {
			allNames.put(line, new DummyNameProb(line));
			line = in.readLine();			
		}
		in.close();						
		System.err.println("Done loading all names.");
		
		System.err.println("Parsing the corpus files and computing probabilities ...");		
		if (!corpusDirectory.endsWith("/")) corpusDirectory += "/";
		File dir = new File(corpusDirectory);
		if(dir.isDirectory()==false){
			System.err.println("[FATAL] Directory does not exists : " + corpusDirectory);
			return;
		}
		String[] list = dir.list();
		for (String filename : list) {
			if (!filename.endsWith(".data")) continue;
			System.err.println("Processing file " + corpusDirectory + filename);
			WikilinksParser p = new WikilinksParser(corpusDirectory + filename);
			int nr_page = -1;
			while (p.hasMoreItems()) {
				nr_page++;
				if (nr_page % 10000 == 0) {
					System.err.println("page nr " + nr_page);
				}
				
				WikiLinkItem i = p.nextItem();
				
				HashSet<String> mentionNames = new HashSet<String>();
				for (Mention m : i.mentions) {
					mentionNames.add(m.anchor_text);
				}
				for (String s : mentionNames) {
					if (allNames.containsKey(s)) {
						allNames.get(s).numDocsWithAnchorName++;
					}
				}
				
				HashSet<String> docWords = new HashSet<String>();
				StringBuilder sbOneWord = new StringBuilder();
				for (int j = 0; j < i.all_text.length(); ++j) {
					char c = i.all_text.charAt(j);
					if (Utils.isWordSeparator(c)) {
						if (sbOneWord.length() > 0) {
							docWords.add(sbOneWord.toString());
						}
						sbOneWord = new StringBuilder();
					} else {
						sbOneWord.append(c);
					}
				}
				
				for (String s : docWords) {
					if (allNames.containsKey(s)) {
						allNames.get(s).numDocsWithName++;
					}					
				}
			}
		}
		System.err.println("Done parsing the corpus files and computing probabilities.");		
		
		System.err.println("Writing probabilities to file ...");		
		for (DummyNameProb d : allNames.values()) {
			// These numbers might be 0 ...
			System.out.println(d.name + "\t" + d.numDocsWithAnchorName + "\t" + d.numDocsWithName);
		}
		System.err.println("Done.");
	}
}
