package entity_linking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import entity_linking.input_data_pipeline.*;

public class Main {    
	///////////////////// MAIN ////////////////////////////////////////////
	public static void main(String[] args) throws Exception {
	    
        // INPUT: args[0] = directory that contains all Wikilinks *.data files OR single input *.data file 
        //        args[1] = file with Crosswikis inv.dict
	    //        args[2] = file with all entities from Wikilinks generated with [ExtractEntsWithFreq]
        // OUTPUT: args[3] = files with (name, doc freq)
        // args[4] = [ComputeCrosswikisProbs]
        if (args.length == 5 && args[4].compareTo("[ComputeCrosswikisProbs]") == 0) {            
            Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");  
            ComputeCrosswikisProbs.fromDir(args[0], args[1], args[2], args[3]);
            return;
        }
	    
	    
        // Extract a set with all 1-ngrams and 2-ngrams from the Wikilinks corpus (*.data file)
        // together with their doc frequencies. Just names appearing in Crosswikis dict are considered.
	    // This was used to compute p(name) and to evaluate
	    // p(exist ent | n) = p(n|e)/p(e|n,exist e)  * p(e)/p(n)
        // INPUT: args[0] = directory that contains all Wikilinks *.data files OR single input *.data file 
	    //        args[1] = file with Crosswikis dict
        // OUTPUT: args[2] = files with (name, doc freq)
	    //         args[3] = num shards for output files (RAM restrictions impose this)
        // args[2] = [ExtractNamesWithDocFreq]
        if (args.length == 5 && args[4].compareTo("[ExtractNamesWithDocFreq]") == 0) {            
            Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");  
            ExtractNamesWithDocFreqs.fromDir(args[0], args[1], args[2], Integer.parseInt(args[3]));
            return;
        }
	    
		// Extract a set with all entities from the Wikilinks corpus (*.data file)
		// together with their doc frequencies
		// INPUT: args[0] = directory that contains all Wikilinks *.data files OR single input *.data file	
		// OUTPUT: args[1] = file with (url, doc freq) 
		// args[2] = [ExtractEntsWithFreq]
		if (args.length == 3 && args[2].compareTo("[ExtractEntsWithFreq]") == 0) {
		    Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");  
			ExtractEntsWithFreq.fromDir(args[0], args[1]);
		    return;
		}
					
		// Extracts all the distinct one-token names from p(e|n) dictionary.
		// Input: args[0] = dictionary file
		//        args[1] = [ExtractAllNamesFromCrosswikiDict]
		if (args.length == 2 && args[1].compareTo("[ExtractAllNamesFromCrosswikiDict]") == 0) {
		    Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");    
			ExtractAllNamesFromCrosswikiDict.run(args[0]);
			return;
		}
		
		// Computes p(dummy | n) probabilities for all names n by looking at all files from the
		// Wikilinks corpus and counting the number of docs where n the anchor text of an Wikipedia link
		// over the number of docs in which n appears
		// Input: args[0] = file with all known names extracted using [ExtractAllNamesFromCrosswikiDict]
		//        args[1] = directory with corpus data
		//        args[2] = [dummyProbs]
		if (args.length == 3 && args[2].compareTo("[dummyProbs]") == 0) {
		    Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");    
			DummyEntityProbabilities.compute(args[0], args[1]);
			return;
		}		
		
		
		// Main functionality of the code that implements the name-entity matching algorithms from the papers.
		// Input: args[0] = complete inv.dict file P(n|e)
		//        args[1] = complete dict file P(e|n) 
		//        args[2] = all entities file generated with [ExtractEntsWithFreq] from (Wikilinks corpus) args[5]
		//        args[3] = file containing dummy probabilities p(M.ent != dummy | P.name = n) from [dummyProbs]
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
	        
	        GenericPagesIterator inputPagesIterator = new WikilinksParser(args[5]);

			GenCandidateEntityNamePairs.run(
					args[0], args[1], args[2], args[3], 
					Double.parseDouble(args[4]), inputPagesIterator,
					args[6].contains("[extended-token-span]"), !args[7].contains("[no-dummy]"));
			return;
		}
		

		
        // IITB testing.
        // Input: args[0] = complete inv.dict file P(n|e)
        //        args[1] = complete dict file P(e|n) 
        //        args[2] = all entities file generated with [ExtractEntsWithFreq] from Wikilinks (args[5])
        //        args[3] = file containing dummy probabilities p(M.ent != dummy | P.name = n) from [dummyProbs]
        //        args[4] = theta
        //        args[5] = IITB ground truth annotations XML filename
		//        args[6] = IITB directory containing all the text documents
        //        args[7] = [simple] or [extended-token-span]
        //        args[8] = [dummy] or [no-dummy]
        //        args[9] = [IITB-testing]
        // OUTPUT: stdout
        if (args.length == 10 && args[9].compareTo("[IITB-testing]") == 0) {
            if (args[8].compareTo("[dummy]") != 0 && args[8].compareTo("[no-dummy]") != 0) {
                System.err.println("Invalid param " + args[8]);
                System.exit(1);
            }
            if (args[7].compareTo("[simple]") != 0 && args[7].compareTo("[extended-token-span]") != 0) {
                System.err.println("Invalid param " + args[7]);
                System.exit(1);
            }            
            Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");
            
            IITBPagesIterator iitbIterator = new IITBPagesIterator(args[5], args[6]);

            int totalnrdocs = 0, totalnrmentions = 0;
            while (iitbIterator.hasNext()) {
                totalnrdocs ++;
                totalnrmentions += iitbIterator.next().truthMentions.size();
            }
            System.err.println("[NRDOCS in IITB] " + totalnrdocs);
            System.err.println("[TOTAL GROUND TRUTH MENTIONS IN IITB] " + totalnrmentions);
            
            iitbIterator = new IITBPagesIterator(args[5], args[6]);
            
            
            GenCandidateEntityNamePairs.run(
                    args[0], args[1], args[2], args[3], 
                    Double.parseDouble(args[4]), iitbIterator,
                    args[7].contains("[extended-token-span]"), !args[8].contains("[no-dummy]"));
            return;
        }		
		
		System.err.println("[ERROR] Invalid command line parameters!");
	}
}
