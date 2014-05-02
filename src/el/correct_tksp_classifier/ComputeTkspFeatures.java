package el.correct_tksp_classifier;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Vector;

import el.Candidate;
import el.context_probs.OverlappingTriplet;
import el.crosswikis.LoadCrosswikisDict;
import el.crosswikis.LoadCrosswikisInvdict;
import el.input_data_pipeline.GenericPagesIterator;
import el.input_data_pipeline.GenericSinglePage;
import el.input_data_pipeline.TruthMention;
import el.input_data_pipeline.wikilinks.WikilinksDirectoryParser;
import el.wikilinks_ents_or_names_with_freqs.LoadWikilinksEntsOrNamesWithFreqs;

public class ComputeTkspFeatures {
    
    public static void compute(
            String WikilinksDir,
            String invdictFilename, 
            String dictFilename, 
            String allEntsFilename) throws IOException, InterruptedException {
        
//        HashMap<String, Integer> entsDocFreqsInCorpus = LoadWikilinksEntsOrNamesWithFreqs.load(allEntsFilename, "entities");
        
//        HashMap<String, TreeMap<String, Double>> invdict = LoadCrosswikisInvdict.load(invdictFilename, null, entsDocFreqsInCorpus);
//        HashMap<String, TreeMap<String, Double>> dict = LoadCrosswikisDict.load(dictFilename, null, null, entsDocFreqsInCorpus);
        
        
        System.err.println("[INFO] Processing Wikilinks to extract and compute training data features:");
        WikilinksDirectoryParser inputPagesIterator = new WikilinksDirectoryParser(WikilinksDir);

        while (inputPagesIterator.hasNext()) {
            GenericSinglePage doc = inputPagesIterator.next();

            for (TruthMention m : doc.truthMentions) {

            }
        }
        System.err.println("[INFO] Done processing Wikilinks.");

    }
}
