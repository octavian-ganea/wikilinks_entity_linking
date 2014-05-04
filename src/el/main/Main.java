package el.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import el.GenCandidateEntityNamePairs;
import el.context_probs.ComputeContextProbsFromWikilinks;
import el.correct_tksp_classifier.ComputeTkspFeatures;
import el.crosswikis.CompleteCrosswikisUsingOtherCorpus;
import el.crosswikis.ExtractAllNamesFromCrosswikiDict;
import el.crosswikis.SortInvdictByName;
import el.entity_existence_probs.ComputeCrosswikisExistenceProbs;
import el.entity_existence_probs.ComputeKeyphrasenessDummyProbs;
import el.input_data_pipeline.*;
import el.input_data_pipeline.iitb.IITBPagesIterator;
import el.input_data_pipeline.wikilinks.WikilinksShardParser;
import el.unittests.AllUnittests;
import el.utils.Utils;
import el.wikilinks_ents_or_names_with_freqs.ExtractWikilinksEntsWithDocFreqs;
import el.wikilinks_ents_or_names_with_freqs.ExtractWikilinksNamesWithDocFreqs;
import el.wikipedia_redirects.WikiRedirects;

public class Main {
    
    ///////////////////// MAIN ////////////////////////////////////////////
    public static void main(String[] args) throws Exception {
        String s = "I love to feede.";
        
        Properties props = new Properties();
        props.setProperty("outputFormat", "tsv");
        MaxentTagger tagger = new MaxentTagger("lib/stanford-postagger-2014-01-04/models/english-bidirectional-distsim.tagger",
                props);
        System.out.println(tagger.tagString(s));
        
        
        AllUnittests.run();	    
        HashMap<String,String> config = LoadConfigFile.load(args[0]);

        ///////////// all code starts from here //////////////////////////

        //Utils.WriteIITBGroundTruthFileInXMLFormat();


        // Compute p(\exist ent | name) in a different fashion using the Crosswikis data.
        // OUTPUT: STDOUT
        if (args[0].compareTo("[ComputeExistenceOfNameAsEntity]") == 0) {  
            WikiRedirects.loadWikiRedirects(config.get("wikiRedirectsFile"));
            ComputeCrosswikisExistenceProbs.compute(
                    config.get("allEntsFilename"), 
                    config.get("allNamesFilename"), 
                    config.get("invdictSortedFilename"), 
                    config.get("dictFilename"));
            return;
        }

        // Classifier for selecting the correct tksp when entity is given.
        // OUTPUT: STDOUT
        if (args[0].compareTo("[tksp-classifier]") == 0) {  
            WikiRedirects.loadWikiRedirects(config.get("wikiRedirectsFile"));
            ComputeTkspFeatures.compute(
                    config.get("WikilinksDir"),
                    config.get("invdictFilename"), 
                    config.get("dictFilename"),
                    config.get("allEntsFilename"));
            return;
        }
        
        
        // Sort invdict by name.
        // OUTPUT: STDOUT
        if (args[0].compareTo("[SortInvdictByName]") == 0) {            
            SortInvdictByName.run(config.get("invdictFilename"));
            return;
        }

        /*
        // INPUT: args[0] = directory that contains all Wikilinks *.data files OR single input *.data file 
        //        args[1] = file with Crosswikis inv.dict
	    //        args[2] = file with all entities from Wikilinks generated with [ExtractEntsWithFreq]
        // OUTPUT: args[3] = files with (name, doc freq)
        // args[4] = [ComputeCrosswikisProbs]
        if (args.length == 5 && args[4].compareTo("[ComputeCrosswikisProbs]") == 0) {            
            Utils.loadWikiRedirects(config.get("wikiRedirectsFile"));
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
            Utils.loadWikiRedirects(config.get("wikiRedirectsFile"));
            ExtractWikilinksNamesWithDocFreqs.fromDir(args[0], args[1], args[2], Integer.parseInt(args[3]));
            return;
        }

		// Extract a set with all entities from the Wikilinks corpus (*.data file)
		// together with their doc frequencies
		// INPUT: args[0] = directory that contains all Wikilinks *.data files OR single input *.data file	
		// OUTPUT: args[1] = file with (url, doc freq) 
		// args[2] = [ExtractEntsWithFreq]
		if (args.length == 3 && args[2].compareTo("[ExtractEntsWithFreq]") == 0) {
            Utils.loadWikiRedirects(config.get("wikiRedirectsFile"));
			ExtractWikilinksEntsWithFreq.fromDir(args[0], args[1]);
		    return;
		}

		// Extracts all the distinct one-token names from p(e|n) dictionary.
		// Input: args[0] = dictionary file
		//        args[1] = [ExtractAllNamesFromCrosswikiDict]
		if (args.length == 2 && args[1].compareTo("[ExtractAllNamesFromCrosswikiDict]") == 0) {
            Utils.loadWikiRedirects(config.get("wikiRedirectsFile"));
			ExtractAllNamesFromCrosswikiDict.run(args[0]);
			return;
		}

		// Computes keyphraseness(n) = p(dummy | n) probabilities for all names n by looking at all files 
		// from the Wikilinks corpus and counting the number of docs where n the anchor text of an Wikipedia link
		// over the number of docs in which n appears
		// Input: args[0] = file with all known names extracted using [ExtractAllNamesFromCrosswikiDict]
		//        args[1] = directory with corpus data
		//        args[2] = [dummyProbs]
		if (args.length == 3 && args[2].compareTo("[dummyProbs]") == 0) {
            Utils.loadWikiRedirects(config.get("wikiRedirectsFile"));
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

            Utils.loadWikiRedirects(config.get("wikiRedirectsFile"));
	        GenericPagesIterator inputPagesIterator = new WikilinksParser(args[5]);

			GenCandidateEntityNamePairs.run(
					args[0], args[1], args[2], args[3], "contextsProbsIITB/probs.txt", "weightedByDictScore", 1.0,
					Double.parseDouble(args[4]), inputPagesIterator,
					args[6].contains("[extended-token-span]"), !args[7].contains("[no-dummy]"), false);
			return;
		}
         */

        // Compute all pairs (n1,e), (n2,e) of overlapping token spans.
        // For each such pairs, let n* = n1 U n2. Then compute also p(n*|n1,e) and p(n*|n2,e) based on Wikilinks
        // OUTPUT: stdout
        if (args[0].equals("[compute-context-probs]")) {
            WikiRedirects.loadWikiRedirects(config.get("wikiRedirectsFile"));

            String additionalIITBAnnotationsFile = null;
            IITBPagesIterator iitbIterator = 
                new IITBPagesIterator(config.get("groundTruthAnnotationsFilename"), additionalIITBAnnotationsFile, config.get("IITBDocsDir"));

            ComputeContextProbsFromWikilinks.run(config.get("invdictFilename"), config.get("WikilinksDir"), config.get("allEntsFilename"), iitbIterator);
            return;
        }

        // IITB testing.
        // OUTPUT: stdout
        if (args[0].compareTo("[IITB-testing]") == 0) {
            WikiRedirects.loadWikiRedirects(config.get("wikiRedirectsFile"));

            boolean improvedIITB = Boolean.parseBoolean(config.get("improvedIITB"));
            String additionalIITBAnnotationsFile =  improvedIITB ? "evalData/IITB/iitb_foundbyme0_0001_final.xml" : null;
            IITBPagesIterator iitbIterator = new IITBPagesIterator(config.get("groundTruthAnnotationsFilename"), additionalIITBAnnotationsFile, config.get("IITBDocsDir"));

            int totalnrdocs = 0, totalnrmentions = 0;
            while (iitbIterator.hasNext()) {
                totalnrdocs ++;
                totalnrmentions += iitbIterator.next().truthMentions.size();
            }
            System.err.println("[NRDOCS in IITB] " + totalnrdocs);
            System.err.println("[TOTAL GROUND TRUTH MENTIONS IN IITB] " + totalnrmentions);

            GenCandidateEntityNamePairs.run(
                    config.get("invdictFilename"), 
                    config.get("dictFilename"),
                    config.get("allEntsFilename"), 
                    config.get("existenceProbsFilename"),
                    config.get("contextProbsFilename"),
                    config.get("valueToKeep"),
                    Double.parseDouble(config.get("multiplyConst")), 
                    Double.parseDouble(config.get("theta")),
                    new IITBPagesIterator(config.get("groundTruthAnnotationsFilename"), additionalIITBAnnotationsFile, config.get("IITBDocsDir")),
                    Boolean.parseBoolean(config.get("extendedTokenSpan")),
                    Boolean.parseBoolean(config.get("includeDummyEnt")));
            return;
        }		

        System.err.println("[ERROR] Invalid command line parameters!");
    }
}
