import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;


public class Main {
	
	// Crosswikis evaluation of the size of the prunned data.
	public static void index_main(String[] args) throws IOException, InterruptedException {	
		//  <string><tab><cprob><space><url>[<space><score>]*
		BufferedReader in = new BufferedReader(new FileReader(new File(args[0])));
		
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
			String mention = st.nextToken();
			
			if (!st.hasMoreTokens()) {
				line = in.readLine();
				continue;
			}

			StringTokenizer st2 = new StringTokenizer(st.nextToken(), " ");
			double cprob = Double.parseDouble(st2.nextToken());
			if (cprob < 0.0001) {
				line = in.readLine();
				continue;
			}
				
			String url = st2.nextToken();			
			//InvertedIndexEntry iie = new InvertedIndexEntry(mention, url, cprob);
			
			if (cprob > 0.01) {
				System.out.println(line);
			}
			
			line = in.readLine();
		}
		in.close();
	}
	

	public static void main(String[] args) throws IOException {		
		//WikilinksParser p = new WikilinksParser(args[0]);
		WikilinksParser p = new WikilinksParser("bla001");
		
		while (p.hasMoreItems()) {
			WikiLinkItem i = p.nextItem();
			System.out.println(i);
		}
	}
	
	
	public static void thrift_main(String[] args) throws IOException {		
		boolean create_index_from_termdocid_pairs = false;
		if (create_index_from_termdocid_pairs) {
			InvertedIndex.createDistributedIndexFromTermDocidPairs();
			return;
		}

		
		// Number of mentions coming with each WikiLinkItem object. 
		int total_num_raw_mentions = 0;

		boolean annotate_from_current_page = false;		
		boolean annotate_from_all_pages_index = true;
		
		WikilinksParser p = new WikilinksParser(args[0]);

		boolean write_term_docids = false;
		if (write_term_docids) {
			InvertedIndex.outputTermDocidPairs(p);
			return;
		}

		AnnotateSentences annotator = null;
		if (annotate_from_current_page) {
			annotator = new AnnotateSentFromCurrentPage();
		}
		if (annotate_from_all_pages_index) {
			annotator = new AnnotateSentFromAllPagesIndex("inverted_index_v2/index_shards/");
		}
		
		// ---------------- Parsing input data ----------------------
		while (p.hasMoreItems()) {
			WikiLinkItem i = p.nextItem();
			total_num_raw_mentions += i.mentions.size();
			
			annotator.annotateSentences(i);
		}
		// ----------------------------------------------------------

		System.out.println("Num raw mentions: " + total_num_raw_mentions);

		if (annotate_from_current_page) {
			System.out.println("Total num local matched entities: ");
		}
		if (annotate_from_all_pages_index) {
			System.out.println("Total num mentions from inverted index: ");
		}
		System.out.println(annotator.getTotalNumAnnotations());
	}
}
