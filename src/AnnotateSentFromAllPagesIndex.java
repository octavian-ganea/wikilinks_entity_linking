import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

class Pair {
	String s;
	int num;
	public Pair(String s, int num) {this.num = num; this.s=s;}
}

public class AnnotateSentFromAllPagesIndex implements AnnotateSentences {
	// Num of mentions after string matching. Tries to find in the current page
	// as many strings representing the same entity as possible based 
	// on all names for that entity from the inverted index (computed from the entire 
	// Wikilinks corpus).
	private int total_num_matched_entities_from_index;

	private HashMap<String, TreeMap<String, Integer>> inverted_index;
	
	// Expects to find files uppercase_letter.shard in the index_dir directory.
	public AnnotateSentFromAllPagesIndex(String index_dir) throws IOException {
		total_num_matched_entities_from_index = 0;
		inverted_index = LoadIndexFromFiles(index_dir);
	}
	
	public static HashMap<String, TreeMap<String, Integer>> LoadIndexFromFiles(String index_dir) throws IOException {
		HashMap<String, TreeMap<String, Integer>> inverted_index = new HashMap<String, TreeMap<String, Integer>>();
		for (char c = 'A'; c <= 'Z'; c++) {
			File f = new File(index_dir + c + ".shard");
			if (!f.exists()) {
			    continue;
			}
			BufferedReader in = new BufferedReader(new FileReader(f));
			String line = in.readLine();
			while (line != null && line.length() > 3) {
				line = line.substring(5);
				int x = line.indexOf(" ---> ");
				if (x == -1) {
					System.err.println("Bad formatted index !!");
					continue;
				}
				
				String key = line.substring(0, x);
				int y = key.indexOf(";freeb_id:");
				String wiki_url = key.substring(0, y);
				String freebase_id = key.substring(y + ";freeb_id:".length());
				if (!inverted_index.containsKey(wiki_url)) {
					inverted_index.put(wiki_url, new TreeMap<String,Integer>());
				}
				
				int cur = x + " ---> ".length()+1;
				boolean last = false;
				while (!last) {
					int z = line.indexOf("), (", cur);
					
					if (z != -1) {
						int ii = 1;
						while(Character.isDigit(line.charAt(z-ii))) ii++;
						if (line.charAt(z-ii) != ',') {
							z = line.indexOf("), (", z+1);
						}
					}					
					if (z != -1) {
						int ii = 1;
						while(Character.isDigit(line.charAt(z-ii))) ii++;
						if (line.charAt(z-ii) != ',') {
							z = line.indexOf("), (", z+1);
						}
					}
					
					
					if (z == -1) {
						last = true;
						z = line.lastIndexOf("), ", line.length() - 1);
					}
					String pair = line.substring(cur, z);
					
					int t = pair.lastIndexOf(",");
					String anchor = pair.substring(0, t);

					int num = Integer.parseInt(pair.substring(t+1));
					inverted_index.get(wiki_url).put(anchor,num);
					
					cur = z + "), (".length();
				}
				
				line = in.readLine();
			}
			in.close();
		}
		
		/*
		 * Print index. For debug purposes
		 *
		 *
		for (String key_wiki : inverted_index.keySet()) {
			System.out.print(key_wiki + " --> ");
			for (String anchor : inverted_index.get(key_wiki).keySet()) {
				System.out.print("(" + anchor + "," + inverted_index.get(key_wiki).get(anchor)+"), ");
			}
			System.out.println();
		}
		*/
		return inverted_index;
	}

	
	// Given the input string, it inserts the mentions in it near the corresponding substrings. 
	private Pair insertMentions(String text, TreeMap<String, Mention> mentions) {
		String rez = text;
		int nr_anchors = 0;
		
		for (Mention m : mentions.values()) {
			int lastIndex = 0;
			while(lastIndex != -1){
				lastIndex = rez.indexOf(m.anchor_text, lastIndex);
				if ( lastIndex != -1) {
					int f = rez.indexOf("[",lastIndex), l = rez.indexOf("#>",lastIndex);
					if (l != -1 && (f == -1 || f > l)) { // We are inside an anchor already
						lastIndex += m.anchor_text.length();
					} else if (!Utils.isSeparateWord(rez, lastIndex, m.anchor_text)) {
						lastIndex += m.anchor_text.length();
					} else {						
						StringBuilder sb = new StringBuilder();
						sb.append(rez.substring(0, lastIndex));
						sb.append("[");
						sb.append(rez.substring(lastIndex, lastIndex + m.anchor_text.length()));
						String t = "]<#" + m.wiki_url.substring(7) + ";" + m.freebase_id + "#>";
						sb.append(t);
						sb.append(rez.substring(lastIndex + m.anchor_text.length()));
						rez = sb.toString();
						nr_anchors ++;

						lastIndex += m.anchor_text.length() + 1 + t.length();
					}
				}
			}
		}
		return new Pair(rez, nr_anchors);
	}
	
	// Tries, for each Freebase entity from the mentions, to find in the current page as many strings representing
	// that entity as possible based on all inverted index names for that entity.
	@Override
	public void annotateSentences(WikiLinkItem item) {	
		
		TreeMap<String, Mention> mentions_hashtable = new TreeMap<String,Mention>(new Utils.StringComp());
		for (Mention m : item.mentions) {
			String key = m.wiki_url.substring(m.wiki_url.lastIndexOf("/") + 1);
			
			if (!inverted_index.containsKey(key) || !inverted_index.get(key).containsKey(m.anchor_text)) {
				System.out.println("---- ERROR: Inverted index does not contain: " + key + " @@@" + m.anchor_text + "$$");
				continue;
			}
			for (String anchor : inverted_index.get(key).keySet()) {
				Mention mm = m.clone();
				mm.anchor_text = anchor;
				mentions_hashtable.put(anchor, mm);
			}
		}

		
		TreeMap<String, Mention> mentions_hashtable2 = new TreeMap<String,Mention>(new Utils.StringComp());
		for (Mention m : item.mentions) {
			if (!mentions_hashtable2.containsKey(m.anchor_text)) {
				mentions_hashtable2.put(m.anchor_text, m);
			}
		}		
		
		
		boolean has_one_sent = false;
		// Vector with sentences annotated with their freebase ids.
		for (String s : item.sentences) {
			Pair ss = insertMentions(s, mentions_hashtable);
			Pair st = insertMentions(s, mentions_hashtable2);
			total_num_matched_entities_from_index += ss.num;

			if (ss.num > st.num) {
			//if (ss.num > 0) {
				System.out.println(">>>>>\n" + ss.s);		
			//	System.out.println(">>>st>>\n" + st.s);		
				has_one_sent = true;
			}
		}

		if (has_one_sent) {
			System.out.println("URL: " + item.url);
			System.out.println("Mentions:");
			for (Mention m : item.mentions) {
				System.out.println(m);
			}
			System.out.println("------");
			System.out.println("------- INFO: Finished page " + item.page_num + " ----");	
		}
	}

	@Override
	public int getTotalNumAnnotations() {
		return total_num_matched_entities_from_index;
	} 	
	
	
}
