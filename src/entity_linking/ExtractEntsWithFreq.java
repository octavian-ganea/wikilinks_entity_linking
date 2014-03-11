package entity_linking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import entity_linking.input_data_pipeline.*;

// Extract a set with all entities from the Wikilinks corpus and their doc frequencies
public class ExtractEntsWithFreq {
    private static void fromFile(String filename, HashMap<String, Integer> freqMap) throws IOException {		
        System.err.println("Processing file " + filename);

        WikilinksParser p = new WikilinksParser(filename);

        int doc_index = 0;
        while (p.hasNext()) {
            doc_index++;
            WikilinksSinglePage doc = p.next();

            HashSet<String> hs = new HashSet<String>();
            for (TruthMention m : doc.truthMentions) {
                if (m.wikiUrl.length() > 0)
                    hs.add(m.wikiUrl);
            }

            Iterator<String> it = hs.iterator();
            while(it.hasNext()) {
                String url = it.next();
                if (!freqMap.containsKey(url)) {
                    freqMap.put(url, 0);
                }
                freqMap.put(url, freqMap.get(url) + 1);
            }
        }
    }	
	

	// Code to merge all shards into a final file with (url, doc freq)
	public static void fromDir(String dir_file, String out_file) throws IOException {
		if (!dir_file.endsWith("/")) {
		    dir_file += "/";
		}

		int total_nr_docs = 0;
        HashMap<String, Integer> freqMap = new HashMap<String, Integer>();

		File dir = new File(dir_file);
		if(dir.isDirectory()==false) {
		    System.out.println("Directory does not exists : " + dir_file);
		    return;
		}
		String[] list = dir.list();
		for (String filename : list) {
		    if (!filename.endsWith(".data")) {
		        continue;       
		    }
		    fromFile(dir_file + filename, freqMap);
		    total_nr_docs++;
		}

		PrintWriter writer = new PrintWriter(out_file, "UTF-8");		
		for (Entry<String, Integer> e : freqMap.entrySet()) {
			writer.println(e.getKey() + "\t" + e.getValue());
		}
		writer.println("NR DOCS:");
		writer.println(total_nr_docs);		
		writer.flush();
		writer.close();		
	}
}
