import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;

class Candidate {
	String entityURL;
	String freebaseID;
	String name;
	int textIndex;
	
	public Candidate(String entityURL, 	String freebaseID, 	String name, int textIndex) {
		this.entityURL = entityURL;
		this.freebaseID= freebaseID ;
		this.name = name;
		this.textIndex= textIndex ;
	}
}

public class GenCandidateEntityNamePairs {
	// invdict[url] = treemap<name, cprob>
	private static HashMap<String, TreeMap<String, Double>> invdict = null;

	// dict [name] = treemap<url, cprob>
	private static HashMap<String, TreeMap<String, Double>> dict = null;
	
	
	// invdict: P(n|e)
	private static void LoadInvdict(String filename) throws IOException{
		// <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]
		
		invdict =  new HashMap<String, TreeMap<String, Double>>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		in.readLine();
		String line = in.readLine();
		int nr_line = 0;
		while (line != null && line.length() > 3) {
			nr_line ++;
			StringTokenizer st = new StringTokenizer(line, "\t");

			if (!st.hasMoreTokens()) {
				line = in.readLine();
				continue;
			}
			String url = st.nextToken();

			if (!st.hasMoreTokens()) {
				line = in.readLine();
				continue;
			}

			String left = st.nextToken();
			double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));				
			String mention = left.substring(left.indexOf(" ") + 1);			

			if (!invdict.containsKey(url)) {
				invdict.put(url, new TreeMap<String, Double>());
			}
			invdict.get(url).put(mention, cprob);
			
			line = in.readLine();
		}
		in.close();		
	}	

	// dict P(e|n)
	private static void LoadDict(String filename) throws IOException{
		// <string><tab><cprob><space><url>
		dict =  new HashMap<String, TreeMap<String, Double>>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		in.readLine();
		String line = in.readLine();
		int nr_line = 0;
		while (line != null && line.length() > 3) {
			nr_line ++;
			StringTokenizer st = new StringTokenizer(line, "\t");

			if (!st.hasMoreTokens()) {
				line = in.readLine();
				continue;
			}
			String name = st.nextToken();

			if (!st.hasMoreTokens()) {
				line = in.readLine();
				continue;
			}

			String left = st.nextToken();
			double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));				
			String url = new StringTokenizer(left.substring(left.indexOf(" ") + 1), " ").nextToken();			

			if (!dict.containsKey(name)) {
				dict.put(name, new TreeMap<String, Double>());
			}
			dict.get(name).put(url, cprob);
			
			line = in.readLine();
		}
		in.close();		
	}	
	
	
	public static void generate(String invdictFilename, String dictFilename, Double theta, String webpagesFilename) throws IOException, InterruptedException {
		// Load the inverted index first.
		LoadInvdict(invdictFilename);
		System.out.println("Done loading index.");

		WikilinksParser p = new WikilinksParser(webpagesFilename);
		while (p.hasMoreItems()) {
			WikiLinkItem i = p.nextItem();
			
			System.out.println("----------- Page:" + i.page_num);
			HashSet<String> hs = new HashSet<String>();
			for (Mention m : i.mentions) {
				if (m.wiki_url.length() > 0)
					hs.add(m.wiki_url);
				System.out.println(m);
			}
			System.out.println("---");
			
			Iterator<String> it = hs.iterator();
			while(it.hasNext()) {
				String url = it.next();
				
				if (!invdict.containsKey(url)) {
					System.out.println("Inv dict does not contain URL:::" + url + "::::");
					continue;
				}
				for (Entry<String, Double> entry : invdict.get(url).entrySet()) {
					if (entry.getValue() >= theta && i.all_text.contains(entry.getKey())) {
						System.out.println("url=" + url + ";text=" + entry.getKey() + ";prob=" + entry.getValue());
					}
				}
			}
		}
	}

}
