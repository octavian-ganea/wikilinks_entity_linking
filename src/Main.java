import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
		Utils.loadWikiRedirects("wiki-redirects/wikipedia_redirect.txt");		
		
		// Part 2 : extract a set with all entities from the Wikilinks corpus (*.data file)
		// together with their doc frequencies
		// INPUT: args[0] = directory that contains all *.data files	
		// OUTPUT: args[1] = file with (url, doc freq) 
		if (args.length == 3 && args[2].compareTo("[all_corpus_ents]") == 0) {
			Part2._2_shard_main(args[0]);
			Part2._2_merge_main(args[0], args[1]);
			return;
		}		
		
		// Code to prune the dictionary index of P(e|n) or the inv.dict of P(n|e).
		// INPUT: args[0] = file that contains dict or invdict;
        //        args[1] = file with all entities obtained by running [2]
		// OUTPUT: args[2]
		if (args.length == 4 && args[3].compareTo("[prune_dict]") == 0) {
			Part3._3_prune_dict(args[0], args[1], args[2]);
		}	
		if (args.length == 4 && args[3].compareTo("[prune_invdict]") == 0) {
			Part3._3_prune_invdict(args[0], args[1], args[2]);
		}	
					
		// Extract a set of all names from the dictionary.
		// Input: args[0] = dictionary file
		if (args.length == 2 && args[1].compareTo("[dict_names]") == 0) {
			ExtractNamesFromCrosswikiDict.extract(args[0]);
		}
		
		// Computes p(dummy | n) probabilities for all names n by looking at all files from the
		// Wikilinks corpus and counting the number of docs where n the anchor text of an Wikipedia link
		// over the number of docs in which n appears
		// Input: args[0] = file with all known names
		//        args[1] = directory with corpus data
		if (args.length == 3 && args[2].compareTo("[dummy_probs]") == 0) {
			DummyEntityProbabilities.compute(args[0], args[1]);
		}		
		
		
		// Generate (n,e) candidates with P(n|e) >= theta and n appears in HTML.
		// Input: args[0] = prunned inv.dict P(n|e) containing just entities from the shard file we look at
		//        args[1] = complete dict file P(e|n) 
		//        args[2] = all entities file generated from [2]
		//        args[3] = file containing dummy probabilities p(M.ent != dummy | P.name = n)
		//        args[4] = theta
		//        args[5] = input WikiLinkItems shard file of the Wikilinks corpus
		if (args.length == 7 && args[6].compareTo("[gen_cand]") == 0) {
			GenCandidateEntityNamePairs.run(
					args[0], args[1], args[2], args[3], Double.parseDouble(args[4]), args[5], false);
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
