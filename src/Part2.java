import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;


public class Part2 {
	// Part 2 : extract a set with all entities from the Wikilinks corpus and their doc frequencies
	// CODE : get mentions for each item; add to a file pair (wikipedia url without wikipedia.org/wiki/, #docs)
    public static void _2_shard_main(String filename, String outputfile) throws IOException {		
        WikilinksParser p = new WikilinksParser(filename);

        HashMap<String, Integer> freq_map = new HashMap<String, Integer>();

        int doc_index = 0;
        while (p.hasMoreItems()) {
            doc_index++;
            WikiLinkItem i = p.nextItem();

            HashSet<String> hs = new HashSet<String>();
            for (Mention m : i.mentions) {
                if (m.wiki_url.length() > 0)
                    hs.add(m.wiki_url);
            }

            Iterator<String> it = hs.iterator();
            while(it.hasNext()) {
                String url = it.next();
                if (!freq_map.containsKey(url)) {
                    freq_map.put(url, 0);
                }
                freq_map.put(url, freq_map.get(url) + 1);
            }
        }

        // Write data to output file:
        PrintWriter writer = new PrintWriter(outputfile, "UTF-8");
        for (Entry<String, Integer> e : freq_map.entrySet()) {
            writer.println(e.getKey() + "\t" + e.getValue());
        }
        writer.println("NR DOCS:");
        writer.println(doc_index);
        writer.flush();
        writer.close();
    }	
	

	// Code to merge all shards into a final file with (url, doc freq)
	// Input: directory that contains all *._2_shard files
	public static void _2_merge_main(String dir_file, String out_file) throws IOException {
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
		    _2_shard_main(filename, dir_file + filename + "._2_shard");
		}
	        
		int total_nr_docs = 0;
		HashMap<String, Integer> freq_map = new HashMap<String, Integer>();
		for (String filename : list) {
			if (!filename.endsWith("._2_shard")) {
			    continue;
			}
			BufferedReader in = new BufferedReader(new FileReader(dir_file + filename));
			String next_line = in.readLine();
			while (!next_line.startsWith("NR DOCS:")) {
				StringTokenizer st = new StringTokenizer(next_line, "\t");
				String url = st.nextToken();
				int freq = Integer.parseInt(st.nextToken());
				if (!freq_map.containsKey(url)) {
					freq_map.put(url, 0);
				}
				freq_map.put(url, freq_map.get(url) + freq);
				next_line = in.readLine();
			}
			next_line = in.readLine();
			total_nr_docs += Integer.parseInt(next_line);
		}

		PrintWriter writer = new PrintWriter(out_file, "UTF-8");		
		for (Entry<String, Integer> e : freq_map.entrySet()) {
			writer.println(e.getKey() + "\t" + e.getValue());
		}
		writer.println("NR DOCS:");
		writer.println(total_nr_docs);		
		writer.flush();
		writer.close();		
	}
}
