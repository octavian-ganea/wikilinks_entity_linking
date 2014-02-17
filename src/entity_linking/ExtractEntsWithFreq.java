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

public class ExtractEntsWithFreq {
	// Extract a set with all entities from the Wikilinks corpus and their doc frequencies
	// CODE : get mentions for each item; add to a file pair (wikipedia url, #docs)
    public static void fromFile(String filename, String outputfile) throws IOException {		
        WikilinksParser p = new WikilinksParser(filename);

        HashMap<String, Integer> freqMap = new HashMap<String, Integer>();

        int doc_index = 0;
        while (p.hasMoreItems()) {
            doc_index++;
            WikilinksSinglePage i = p.nextItem();

            HashSet<String> hs = new HashSet<String>();
            for (TruthMention m : i.truthMentions) {
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

        // Write data to output file:
        PrintWriter writer = new PrintWriter(outputfile, "UTF-8");
        for (Entry<String, Integer> e : freqMap.entrySet()) {
            writer.println(e.getKey() + "\t" + e.getValue());
        }
        writer.println("NR DOCS:");
        writer.println(doc_index);
        writer.flush();
        writer.close();
    }	
	

	// Code to merge all shards into a final file with (url, doc freq)
	// Input: directory that contains all *._2_shard files
	public static void fromDir(String dir_file, String out_file) throws IOException {
		if (!dir_file.endsWith("/")) {
		    dir_file += "/";
		}
		
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
		    fromFile(filename, dir_file + filename + "._2_shard");
		}
	        
		int total_nr_docs = 0;
		HashMap<String, Integer> freqMap = new HashMap<String, Integer>();
		for (String filename : list) {
			if (!filename.endsWith("._2_shard")) {
			    continue;
			}
			BufferedReader in = new BufferedReader(new FileReader(dir_file + filename));
			String nextLine = in.readLine();
			while (!nextLine.startsWith("NR DOCS:")) {
				StringTokenizer st = new StringTokenizer(nextLine, "\t");
				String url = st.nextToken();
				int freq = Integer.parseInt(st.nextToken());
				if (!freqMap.containsKey(url)) {
					freqMap.put(url, 0);
				}
				freqMap.put(url, freqMap.get(url) + freq);
				nextLine = in.readLine();
			}
			nextLine = in.readLine();
			total_nr_docs += Integer.parseInt(nextLine);
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
