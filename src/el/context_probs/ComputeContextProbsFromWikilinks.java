/*
 * Computes p(context | name , ent) from Wiklinks corpus.
 * 
 */
package el.context_probs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import el.context_probs.OverlappingTriplet;
import el.input_data_pipeline.GenericPagesIterator;
import el.input_data_pipeline.GenericSinglePage;
import el.input_data_pipeline.TruthMention;
import el.input_data_pipeline.iitb.IITBPagesIterator;
import el.input_data_pipeline.wikilinks.WikilinksDirectoryParser;
import el.input_data_pipeline.wikilinks.WikilinksShardParser;
import el.utils.Utils;
import el.wikilinks_ents_or_names_with_freqs.LoadWikilinksEntsOrNamesWithFreqs;
import el.wikipedia_redirects.WikiRedirects;
import static org.junit.Assert.*;


// Just for entities appearing in Wikilinks and IITB (it takes too long time & RAM to run on the entire Wikilinks ents)
public class ComputeContextProbsFromWikilinks { 

    private static TreeMap<String, OverlappingTriplet> parseInvdictAndFindOverlappingTriplets(
            String invdictFilename, 
            String allEntitiesFilename, 
            IITBPagesIterator iitbIterator) throws IOException {

        HashMap<String, Integer> entsDocFreqsInCorpus = LoadWikilinksEntsOrNamesWithFreqs.load(allEntitiesFilename, "entities");

        HashSet<String> entsIITB = new HashSet<String>();
        while (iitbIterator.hasNext()) {
            GenericSinglePage doc = iitbIterator.next();
            for (TruthMention m : doc.truthMentions) {
                if (m.wikiUrl.length() > 0) {
                    entsIITB.add(m.wikiUrl);
                }    
            }
        }
        System.err.println("[INFO] Num IITB ents = " + entsIITB.size());


        TreeMap<String, OverlappingTriplet> map = new TreeMap<String, OverlappingTriplet>();

        System.err.println("[INFO] Parsing invdict file to find possible triplets (contexts, name, ent) ...");
        BufferedReader in = new BufferedReader(new FileReader(invdictFilename));
        String line = in.readLine();
        int nr_line = 0;

        String currentURL = "";
        boolean processCurrentURL = false;
        Vector<String> mentions = new Vector<String>();
        while (line != null && line.length() > 0) {
            nr_line ++;
            if (nr_line % 1000000 == 0) {
                System.err.println("loaded " + nr_line + " ; overlapping map size = " + map.size());
            }

            StringTokenizer st = new StringTokenizer(line, "\t");
            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            String rawURL = st.nextToken();
            String url = WikiRedirects.pruneURL(rawURL);
            if (!url.equals(currentURL)) {
                if (processCurrentURL) {
                    for (String mention : mentions) {
                        OverlappingTriplet ot = null;
                        for (String otherMention : mentions) {
                            String context = Utils.getContext(mention, otherMention);
                            if (!context.equals("") && !context.equals(mention)) {
                                if (ot == null) {
                                    ot = new OverlappingTriplet(mention, currentURL);
                                }
                                ot.addContext(context);
                            }
                        }
                        if (ot != null){
                            map.put(ot.serialize(), ot);
                        }
                    }
                }
                currentURL = url;
                mentions = new Vector<String>();
                if (entsIITB.contains(currentURL) && entsDocFreqsInCorpus.containsKey(currentURL)) {
                    processCurrentURL = true;
                } else {
                    processCurrentURL = false;
                }
            }

            if (url.length() == 0 || !processCurrentURL) {
                line = in.readLine();
                continue;
            }
            String left = st.nextToken();
            String mention = left.substring(left.indexOf(" ") + 1);
            mentions.add(mention);

            line = in.readLine();
        }

        if (processCurrentURL) {
            for (String mention : mentions) {
                OverlappingTriplet ot = null;
                for (String otherMention : mentions) {
                    String context = Utils.getContext(mention, otherMention);
                    if (!context.equals("") && !context.equals(mention)) {
                        if (ot == null) {
                            ot = new OverlappingTriplet(mention, currentURL);
                        }
                        ot.addContext(context);
                    }
                }
                if (ot != null){
                    map.put(ot.serialize(), ot);
                }
            }
        }
        in.close();  

        System.err.println("[DONE] Num keys: " + map.keySet().size());
        /*
        for (OverlappingTriplet ot : map.values()) {
            ot.toSTDOUT();
        }
         */
        return map;
    }

    public static void run(
            String invdictFilename,
            String WikilinksDir,
            String allEntitiesFilename,
            IITBPagesIterator iitbIterator) throws IOException, InterruptedException {

        TreeMap<String, OverlappingTriplet> tripletMap = parseInvdictAndFindOverlappingTriplets(invdictFilename, allEntitiesFilename, iitbIterator);

        System.err.println("[INFO] Processing Wikilinks and update P(context | n,e) ...");
        WikilinksDirectoryParser inputPagesIterator = new WikilinksDirectoryParser(WikilinksDir);


        while (inputPagesIterator.hasNext()) {
            GenericSinglePage doc = inputPagesIterator.next();

            for (TruthMention m : doc.truthMentions) {
                String key = new OverlappingTriplet(m.anchorText, m.wikiUrl).serialize();
                if (tripletMap.containsKey(key)) {
                    OverlappingTriplet ot = tripletMap.get(key);
                    ot.increment_num_n_e();

                    int startContextIndexInDoc = Math.max(0, m.mentionOffsetInText - 30);
                    int stopContextIndexInDoc = Math.min(doc.getRawText().length(), m.mentionOffsetInText + 60);
                    String overallContexts = doc.getRawText().substring(startContextIndexInDoc, stopContextIndexInDoc);

                    for (String context : ot.allContexts()) {
                        int nameIndexInContext = context.indexOf(m.anchorText);
                        assertTrue(nameIndexInContext >= 0);
                        int index = overallContexts.indexOf(context);
                        if (index >= 0 && index == m.mentionOffsetInText - startContextIndexInDoc - nameIndexInContext) {
                            ot.increment_num_context_n_e(context);
                        }
                    }
                }
            }
        }
        System.err.println("[INFO] Done processing Wikilinks. Now writing output.");

        for (String key : tripletMap.keySet()) {
            OverlappingTriplet ot = tripletMap.get(key);
            if (ot.get_num_n_e() < 5) {
                continue;
            }
            for (String context : ot.allContexts()) {
                System.out.println(key + "\t" + context + "\t" + ot.elems_of_context_cond_n_e(context) + "\t" + ot.prob_context_cond_n_e(context));
            }
        }
        System.err.println("[INFO] Done.");
    }
}
