import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;


public class Main {
	///////////////////// MAIN ////////////////////////////////////////////
	public static void main(String[] args) throws Exception {
		// Part 2 : extract a set with all entities from the Wikilinks corpus and their doc frequencies
		// INPUT: args[0] = directory that contains all *.data files	
		// OUTPUT: file with (url, doc freq): "args[0]/_2_merge"
		if (args[1].compareTo("[2]") == 0) {
			Part2._2_shard_main(args[0]);
			Part2._2_merge_main(args[0]);
		}	

		if (args[1].compareTo("[2.merge]") == 0) {
			Part2._2_merge_main(args[0]);
		}			
		
		// Code to prune the dictionary index of P(e|n) or the inv.dict of P(n|e).
		// INPUT: args[0] = file that contains dict or invdict; should have _2_merge in the same directory
		// OUTPUT: a file called "prunned_" + args[0]
		if (args[1].compareTo("[3.dict]") == 0) {
			Part3._3_prune_dict(args[0]);
		}	
		if (args[1].compareTo("[3.invdict]") == 0) {
			Part3._3_prune_invdict(args[0]);
		}	
		
		if (args[1].compareTo("[exp]") == 0) {
			BufferedReader in = new BufferedReader(new FileReader("prunned_inv.dict"));
			ArrayList<String> a = new ArrayList<String>();
			
			String line = in.readLine();
			int i = 0;
			while (line != null) {
				a.add(line);
				if (i%1000000 == 0) {
					System.out.println(i/1000000);
					System.out.println("Total memory (bytes): " + 
					        Runtime.getRuntime().totalMemory());
				}
					
				i++;
				line = in.readLine();
			}
		}	
		
	
	
	}
	///////////////////////////////////////////////////////////////////////
	

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
