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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

class Candidate {
	String entityURL;
	String freebaseID;
	String name;
	int textIndex;
	
	double invdictProb; // probability P(n|e)
	double posteriorProb; // as from formula (8)
	
	public Candidate(String entityURL, 	String freebaseID, 	String name, int textIndex, double invdictProb) {
		this.entityURL = entityURL;
		this.freebaseID= freebaseID ;
		this.name = name;
		this.textIndex= textIndex ;
		this.invdictProb = invdictProb;
	}
}

public class GenCandidateEntityNamePairs {
	/////////////////////////////// Fields ////////////////////////////////////////////////////////////
	
	// invdict[url] = treemap<name, cprob>
	private static HashMap<String, TreeMap<String, Double>> invdict = null;

	// dict [name] = treemap<url, cprob>
	private static HashMap<String, TreeMap<String, Double>> dict = null;
	
	// all Wikipedia entities (excluding redirects)
	// allEnts[name] = doc_frequency
	private static HashMap<String, Integer> allEnts = null;
	
	// Total number of docs from the Wikilinks corpus.
	private static int totalNumDocs = 0;
	
	//////////////// Methods ////////////////////////////////////////////////////////////////////////////

	// invdict: P(n|e)
	private static void LoadInvdict(String filename) throws IOException{
		// <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]
		
		invdict =  new HashMap<String, TreeMap<String, Double>>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
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
			url = Utils.pruneURL(url);
			
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

	// dict: P(e|n)
	// TODO: load just e,n with n in the candidates
	private static void LoadDict(String filename, HashSet<String> allCandidateNames) throws IOException{
		// <string><tab><cprob><space><url>
		dict =  new HashMap<String, TreeMap<String, Double>>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = in.readLine();
		int nr_line = 0;
		while (line != null && line.length() > 3) {
			if (nr_line % 5000000 == 0)
				System.out.println("loaded " + nr_line);
			nr_line ++;
			StringTokenizer st = new StringTokenizer(line, "\t");

			if (!st.hasMoreTokens()) {
				line = in.readLine();
				continue;
			}
			String name = st.nextToken();
			
			if (!allCandidateNames.contains(name) || !st.hasMoreTokens()) {
				line = in.readLine();
				continue;
			}

			String left = st.nextToken();
			double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));				
			String url = new StringTokenizer(left.substring(left.indexOf(" ") + 1), " ").nextToken();			
			url = Utils.pruneURL(url);
			
			if (!dict.containsKey(name)) {
				dict.put(name, new TreeMap<String, Double>());
			}
			dict.get(name).put(url, cprob);
			
			line = in.readLine();
		}
		in.close();		
	}	
	
	private static void LoadAllEntities(String filename) throws IOException {
		allEnts = new HashMap<String, Integer>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = in.readLine();
		int nr_line = 0;
		while (!line.startsWith("NR DOCS:")) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			String url = st.nextToken();
			if (!st.hasMoreTokens()) {
				System.err.println("[ERROR] wrong line format in all entities file :" + line + "::::::");
				line = in.readLine();
				continue;
			}
			int freq = Integer.parseInt(st.nextToken());
			allEnts.put(url, freq);
			line = in.readLine();
		}
		totalNumDocs = Integer.parseInt(in.readLine());
	}
	
	public static void generate(
			String invdictFilename, 
			String dictFilename, 
			String allEntitiesFileName, 
			Double theta, 
			String webpagesFilename) throws IOException, InterruptedException {
		
		System.out.println("loading inv index P(n|e) ...");
		LoadInvdict(invdictFilename);
		System.out.println("Done loading index. Size = " + invdict.size());

		HashSet<String> allCandidateNames = new HashSet<String>();
		Vector<Vector<Candidate>> allCandidates = new Vector<Vector<Candidate>>();
		
		System.out.println("Generating all candidates and their names ...");
		WikilinksParser p = new WikilinksParser(webpagesFilename);
		int nr_page = -1;
		while (p.hasMoreItems()) {
			nr_page++;
			if (nr_page % 1000 == 0)
				System.out.println(nr_page);
			
			WikiLinkItem i = p.nextItem();
			HashSet<String> hs = new HashSet<String>();
			for (Mention m : i.mentions) {
				if (m.wiki_url.length() > 0)
					hs.add(m.wiki_url);
			}
			
			Vector<Candidate> currentPageCandidates = new Vector<Candidate>();
			
			Iterator<String> it = hs.iterator();
			while(it.hasNext()) {
				String url = it.next();
				
				if (!invdict.containsKey(url)) {
					continue;
				}
				
				Set<Entry<String,Double>> set = invdict.get(url).entrySet();
				for (Entry<String, Double> entry : set) {
					if (entry.getValue() >= theta) {
						String name = entry.getKey();
						int index = i.all_text.indexOf(name);
						while (index != -1) {
							// Keep just candidates that are separate words.
							if (index > 0 && !Utils.isWordSeparator(i.all_text.charAt(index-1))) {
								index = i.all_text.indexOf(name, index + 1);
								continue;
							}
							if (index + name.length() < i.all_text.length() &&
									!Utils.isWordSeparator(i.all_text.charAt(index + name.length()))) {
								index = i.all_text.indexOf(name, index + 1);
								continue;
							}
							
							allCandidateNames.add(name);
							currentPageCandidates.add(
								new Candidate(url, null, name, index, entry.getValue()));
							
							//System.out.println("# CANDIDATE # -- url=" + url + " name=" + name + ":::  index=" + index);
							
							index = i.all_text.indexOf(name, index + 1);							
						}
					}
				}
			}
			
			allCandidates.add(currentPageCandidates);
		}
		System.out.println("Done. Num all candidate names = " + allCandidateNames.size());

		// Free memory:
		invdict = null;
		
		System.out.println("loading dict P(e|n) index...");
		LoadDict(dictFilename, allCandidateNames);
		System.out.println("Done. Size = " + dict.size());
		
		System.out.println("loading all entities...");
		LoadAllEntities(allEntitiesFileName);
		System.out.println("All ents size : " + allEnts.size() + " ; total num docs = " + totalNumDocs);
		
		// Compute the l(n,e) values, group by n and find the winning candidate.
		System.out.println("Winner entities:");
		p = new WikilinksParser(webpagesFilename);
		nr_page = -1;
		while (p.hasMoreItems()) {
			nr_page++;
			WikiLinkItem i = p.nextItem();
			
			// compute M.doc.E
			HashSet<String> docEntities = new HashSet<String>();
			for (Mention m : i.mentions) {
				if (m.wiki_url.length() > 0)
					docEntities.add(m.wiki_url);
			}			
			
			Vector<Candidate> candidates = allCandidates.get(nr_page);
			
			// Winning candidates grouped by their starting index in all_text
			// Key of the hash map: all_text offset + " " + name
			HashMap<String, Candidate> winners = new HashMap<String,Candidate>();
			
			// For each candidate, compute the score as described in formula (8).
			for (Candidate cand : candidates) {
				double denominator = 0.0;
				
				if (!dict.containsKey(cand.name)) {
					System.err.println("[Warning] Dict does not contain :::" + cand.name + "::::");
					continue;
				}
				TreeMap<String, Double> allContributingWikiUrlsToDenominator = dict.get(cand.name);
				 
				Set<Entry<String,Double>> set = allContributingWikiUrlsToDenominator.entrySet();
				for (Entry<String, Double> entry : set) {
					if (docEntities.contains(entry.getKey()))
						denominator += entry.getValue() / allEnts.get(entry.getKey());
					else if (allEnts.containsKey(entry.getKey())) {
						denominator += entry.getValue() / (totalNumDocs - allEnts.get(entry.getKey()));
					} else {
						denominator += entry.getValue() / totalNumDocs;						
					}
				}
				
				// if P(cand.wiki_url | cand.name) = 0 , we ignore this candidate.
				if (!allContributingWikiUrlsToDenominator.containsKey(cand.entityURL)) {
					continue;
				}
				
				double score = dict.get(cand.name).get(cand.entityURL) / allEnts.get(cand.entityURL);
				score /= denominator;
				cand.posteriorProb = score;
				
				String key = cand.textIndex + " " + cand.name;
				if (!winners.containsKey(key) || winners.get(key).posteriorProb < score) {
					winners.put(key, cand);
				}
			}

			
			System.out.println("------- Page: " + nr_page);

			for (Mention m : i.mentions) {
				System.out.println("## MENTION ## " + m.toString() + " " + m.text_offset);
			}
			
			for (Candidate c : winners.values()) {
				System.out.println();
				System.out.println("url=" + c.entityURL + ";name=" + c.name + ";text offset=" +
						c.textIndex + ";prob=" + c.invdictProb);
				
				
				for (Mention m : i.mentions) {
					if (m.text_offset >= 0 && m.text_offset == c.textIndex && m.anchor_text.trim().compareTo(c.name.trim()) == 0) {
						System.out.println("## GOOD RESULT ##");
						break;
					}
				}
				/*
				int start = c.textIndex - 50;
				if (start < 0) start = 0;
				int end = c.textIndex + 50;
				if (end >= i.all_text.length()) end = i.all_text.length() - 1;
				
				System.out.println(";context=" + i.all_text.substring(start, end).replace('\n', ' '));
				*/
			}
		}
	}

}
