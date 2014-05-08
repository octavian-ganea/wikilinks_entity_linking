package el.main;

import java.util.HashMap;

import el.GenCandidateEntityNamePairs;
import el.context_probs.ComputeContextProbsFromWikilinks;
import el.correct_tksp_classifier.ComputeTkspFeatures;
import el.crosswikis.SortInvdictByName;
import el.entity_existence_probs.ComputeCrosswikisExistenceProbs;
import el.input_data_pipeline.iitb.IITBPagesIterator;
import el.unittests.AllUnittests;
import el.wikipedia_redirects.WikiRedirects;

public class Main {
    
    ///////////////////// MAIN ////////////////////////////////////////////
    public static void main(String[] args) throws Exception {
        AllUnittests.run();	    
        
        if (args.length == 0) {
            System.err.println("[ERROR] Invalid command line parameters!");
            return;
        }
        HashMap<String,String> config = LoadConfigFile.load(args[0]);

        ///////////// All code starts from here //////////////////////////

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
        // Training data: Wikilinks
        // Test data: IITB
        // OUTPUT: STDOUT
        if (args[0].compareTo("[tksp-classifier]") == 0) {  
            WikiRedirects.loadWikiRedirects(config.get("wikiRedirectsFile"));
            
            ComputeTkspFeatures compTkspFeatures = new ComputeTkspFeatures(
                    config.get("invdictFilename"), 
                    config.get("dictFilename"),
                    config.get("allEntsFilename"));

            // Test example:
            System.err.println("[INFO] Test example:");
            System.err.println(compTkspFeatures.computeFeaturesVectorForOneName("dog", "Dog", "My dog is good.", "dog is good."));
            
            // Training data:
            System.err.println("[INFO] Computing training data ...");
            compTkspFeatures.computeForWikilinks(config.get("WikilinksDir"), config.get("trainingOutputFileRoot"));

            // Test data:
            System.err.println("[INFO] Computing test data ...");
            boolean improvedIITB = Boolean.parseBoolean(config.get("improvedIITB"));
            String additionalIITBAnnotationsFile = improvedIITB ? "evalData/IITB/iitb_foundbyme0_0001_final.xml" : null;
            compTkspFeatures.computeForIITB(
                    config.get("groundTruthAnnotationsFilename"), 
                    additionalIITBAnnotationsFile, 
                    config.get("IITBDocsDir"), 
                    config.get("testOutputFileRoot"));

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
        // args[4] = [CompleteCrosswikisUsingOtherCorpus]
        if (args.length == 5 && args[4].compareTo("[CompleteCrosswikisUsingOtherCorpus]") == 0) {            
            Utils.loadWikiRedirects(config.get("wikiRedirectsFile"));
            CompleteCrosswikisUsingOtherCorpus.fromDir(args[0], args[1], args[2], args[3]);
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

        // Main functionality of the code that implements the name-entity matching algorithms from the papers.
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
