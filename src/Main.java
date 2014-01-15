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
		
		// Part 2 : extract a set with all entities from the Wikilinks corpus (*.data file)
		// together with their doc frequencies
		// INPUT: args[0] = directory that contains all *.data files OR single input *.data file	
		// OUTPUT: args[1] = file with (url, doc freq) 
		// args[2] = [file_ents]
		if (args.length == 3 && args[2].compareTo("[file_ents]") == 0) {
		    Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");  
		    
		    // args[0] is a single .data file
			Part2._2_shard_main(args[0], args[1]);
			
			// Uncomment the next line to generate the (url, doc freq)  for the entire Wikilinks corpus
			// args[0] is a directory
//			Part2._2_merge_main(args[0], args[1]);
		}
					
		// Extract a set of all names from the dictionary.
		// Input: args[0] = dictionary file
		//        args[1] = [dict-names]
		if (args.length == 2 && args[1].compareTo("[dict_names]") == 0) {
		    Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");    
			ExtractNamesFromCrosswikiDict.extract(args[0]);
		}
		
		// Computes p(dummy | n) probabilities for all names n by looking at all files from the
		// Wikilinks corpus and counting the number of docs where n the anchor text of an Wikipedia link
		// over the number of docs in which n appears
		// Input: args[0] = file with all known names
		//        args[1] = directory with corpus data
		//        args[2] = [dummy_probs]
		if (args.length == 3 && args[2].compareTo("[dummy_probs]") == 0) {
		    Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");    
			DummyEntityProbabilities.compute(args[0], args[1]);
		}		
		
		
		// Main functionality of the code that implements the name-entity matching algorithms from the papers.
		// Input: args[0] = complete inv.dict file P(n|e)
		//        args[1] = complete dict file P(e|n) 
		//        args[2] = all entities file generated with [file_ents] from args[5]
		//        args[3] = file containing dummy probabilities p(M.ent != dummy | P.name = n) from [dummy_probs]
		//        args[4] = theta
		//        args[5] = input WikiLinkItems shard file of the Wikilinks corpus
		//        args[6] = [simple] or [extended-token-span]
		//        args[7] = [dummy] or [no-dummy]
		//        args[8] = [run_main]
		// OUTPUT: stdout
		if (args.length == 9 && args[8].compareTo("[run_main]") == 0) {
		    if (args[7].compareTo("[dummy]") != 0 && args[7].compareTo("[no-dummy]") != 0) {
		        System.err.println("Invalid param " + args[7]);
		        System.exit(1);
		    }
	        if (args[6].compareTo("[simple]") != 0 && args[6].compareTo("[extended-token-span]") != 0) {
	            System.err.println("Invalid param " + args[6]);
	            System.exit(1);
	        }
		    
	        Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");
			GenCandidateEntityNamePairs.run(
					args[0], args[1], args[2], args[3], 
					Double.parseDouble(args[4]), args[5],
					args[6].contains("[extended-token-span]"), !args[7].contains("[no-dummy]"));
		}
	}
}
