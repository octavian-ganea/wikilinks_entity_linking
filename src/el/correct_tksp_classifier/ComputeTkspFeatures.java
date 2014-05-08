package el.correct_tksp_classifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import el.crosswikis.LoadCrosswikisDict;
import el.crosswikis.LoadCrosswikisInvdict;
import el.input_data_pipeline.GenericPagesIterator;
import el.input_data_pipeline.GenericSinglePage;
import el.input_data_pipeline.TruthMention;
import el.input_data_pipeline.iitb.IITBPagesIterator;
import el.input_data_pipeline.wikilinks.WikilinksDirectoryParser;
import el.utils.Utils;
import el.wikilinks_ents_or_names_with_freqs.LoadWikilinksEntsOrNamesWithFreqs;

/*
 * Class to compute features that will be used in a classifier to solve the following problem:
 *  ** given a context and an entity, find its proper name.
 * Examples:
 *  - entity = Antivirus_software ; context = "antivirus software" . 
 *  - What is the best name to choose for this entity ? Is it "antivirus" or "antivirus software" ? 
 */

public class ComputeTkspFeatures {
    
    HashMap<String, Integer> entsDocFreqsInCorpus;
    HashMap<String, TreeMap<String, Double>> invdict;
    HashMap<String, TreeMap<String, Double>> dict;
    
    
    public ComputeTkspFeatures(String invdictFilename,
            String dictFilename,
            String allEntsFilename) throws IOException {
        entsDocFreqsInCorpus = LoadWikilinksEntsOrNamesWithFreqs.load(allEntsFilename, "entities");
        invdict = LoadCrosswikisInvdict.load(invdictFilename, null, entsDocFreqsInCorpus);
        dict = LoadCrosswikisDict.load(dictFilename, null, null, entsDocFreqsInCorpus);
    }
    
    
    public HashMap<Integer, Double> computeFeaturesVectorForOneName(
            String name, 
            String ent,
            String surroundingParagraph,
            String nameToEndOfParagraph) {
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        outputFeaturesForOneName(
                false, 
                name, 
                ent,
                surroundingParagraph,
                nameToEndOfParagraph,
                0,
                new PrintWriter(new StringWriter()),
                pw);
        
        HashMap<Integer, Double> rez = new HashMap<Integer, Double>();
        
        StringTokenizer st = new StringTokenizer(sw.toString(), " \n");
        while (st.hasMoreTokens()) {
            String keyValPair = st.nextToken();
            if (!keyValPair.contains(":")) continue;
            StringTokenizer staux = new StringTokenizer(keyValPair, ":");
            rez.put(Integer.parseInt(staux.nextToken()), Double.parseDouble(staux.nextToken()));
        }
        
        return rez;
    }
    
    
    public void computeForWikilinks(String WikilinksDir, String outputFileRoot) throws IOException {
        System.err.println("[INFO] Processing Wikilinks to extract and compute training data features:");
        GenericPagesIterator inputPagesIterator = new WikilinksDirectoryParser(WikilinksDir);
        computeForGenericPagesIterator(inputPagesIterator, outputFileRoot);
        System.err.println("[INFO] Done processing Wikilinks.");
    }
    
    
    public void computeForIITB(
            String groundTruthAnnotationsFilename, 
            String additionalIITBAnnotationsFile, 
            String IITBDocsDir, 
            String outputFileRoot) throws IOException, SAXException, ParserConfigurationException {
        
        System.err.println("[INFO] Processing IITB to extract and compute test data features:");
        GenericPagesIterator inputPagesIterator = new IITBPagesIterator(
                groundTruthAnnotationsFilename, 
                additionalIITBAnnotationsFile, 
                IITBDocsDir);
        computeForGenericPagesIterator(inputPagesIterator, outputFileRoot);
        System.err.println("[INFO] Done processing IITB.");
    }
    

    //////////////////////////////// Private methods from here /////////////////////////////////////////
    
    private boolean isInBothCrosswikis(
            String name, 
            String ent) {
        
        if (!invdict.containsKey(ent) || !invdict.get(ent).containsKey(name) || !dict.containsKey(name) || !dict.get(name).containsKey(ent)) {
            return false;
        }
        return true;
    }
    
    

    // If this is positive, then it will output also the negative examples that 
    private int outputFeaturesForOneName(
            boolean positive,
            String name, 
            String ent,
            String surroundingParagraph,
            String nameToEndOfParagraph,
            int trainingDataIndex,
            PrintWriter writerVerbose,
            PrintWriter writerJustData) {
 
        if (positive) {
            // Check if this is not the only name candidate for this entity in this sentence (in which case we have nothing to do).
            int nrOtherCandidates = 0;
            for (String otherName : invdict.get(ent).keySet()) {
                if (surroundingParagraph.contains(otherName) &&
                        !otherName.equals(name) &&
                        isInBothCrosswikis(otherName, ent) &&
                        // check if token spans are overlapping
                        surroundingParagraph.indexOf(otherName) <  surroundingParagraph.indexOf(name) + name.length() &&
                        surroundingParagraph.indexOf(name) <  surroundingParagraph.indexOf(otherName) + otherName.length() ) {
                    nrOtherCandidates++;
                }
            }
            // If we don't have other examples in this sentence, we skip.
            if (nrOtherCandidates == 0) return trainingDataIndex;
        }
        
        if (!isInBothCrosswikis(name, ent)) {
            return trainingDataIndex;
        }
        
        writerVerbose.println("trainingDataIndex : " + trainingDataIndex);
        trainingDataIndex++;

        // Represent features in the format : label [index:value]*  required by LibSVM and LibLinear
        StringBuilder FeaturesDataLine = new StringBuilder();
        int indexFeatures = 1;
        
        if (positive) {
            writerVerbose.println("Positive");
        } else {
            writerVerbose.println("Negative");            
        }
        FeaturesDataLine.append(positive ? 1 : 0);
        
        writerVerbose.println("ent : " + ent);
        writerVerbose.println("name : " + name);
        
        writerVerbose.println("p_n_cond_e : " + invdict.get(ent).get(name));
        FeaturesDataLine.append(" " + indexFeatures + ":" + invdict.get(ent).get(name));
        indexFeatures ++;
        
        writerVerbose.println("p_e_cond_n : " + dict.get(name).get(ent));
        FeaturesDataLine.append(" " + indexFeatures + ":" + dict.get(name).get(ent));
        indexFeatures++;
        
        int numTokens = Utils.numTokensUsingStanfordNLP(name);
        writerVerbose.println("num_tokens : " + numTokens);
        if (numTokens > 10) numTokens = 10;
        for (int i = 1; i <= 10; i++, indexFeatures++) {
            if (i == numTokens) {
                FeaturesDataLine.append(" " + indexFeatures + ":1");
            } else {
                FeaturesDataLine.append(" " + indexFeatures + ":0");                
            }
        }
        
        Vector<String> posTags = Utils.getPreviousAndNextTokensAndPosTags(surroundingParagraph, name, nameToEndOfParagraph);
        writerVerbose.println("prev_token : " + posTags.get(0));
        writerVerbose.println("prev_tag : " + posTags.get(1));
        for (int i = 0; i < Utils.tagger.numTags(); i++, indexFeatures++) {
            if (Utils.tagger.getTag(i).equals(posTags.get(1))) {
                FeaturesDataLine.append(" " + indexFeatures + ":1");
            } else {
                FeaturesDataLine.append(" " + indexFeatures + ":0");                
            }
        }
        
        writerVerbose.println("next_token : " + posTags.get(2));
        writerVerbose.println("next_tag : " + posTags.get(3));
        for (int i = 0; i < Utils.tagger.numTags(); i++, indexFeatures++) {
            if (Utils.tagger.getTag(i).equals(posTags.get(3))) {
                FeaturesDataLine.append(" " + indexFeatures + ":1");
            } else {
                FeaturesDataLine.append(" " + indexFeatures + ":0");                
            }
        }
        
        
        // Other token spans that might be candidates will be sorted by p(e|n)*p(n|e) and top 2 will be considered:
        TreeMap<Double, String> otherNamesAndScores = new TreeMap<Double, String>();
        
        TreeMap<String, Double> namesPool = invdict.get(ent);
        for (String otherName : namesPool.keySet()) {
            if (surroundingParagraph.contains(otherName) && !otherName.equals(name) && isInBothCrosswikis(otherName, ent)) {
                double score = invdict.get(ent).get(otherName) * dict.get(otherName).get(ent);
                otherNamesAndScores.put(score, otherName);
            }
        }
        
        double firstOtherScore = otherNamesAndScores.lastKey();
        String firstOtherName = otherNamesAndScores.get(firstOtherScore);
        writerVerbose.println("first_other_name : " + firstOtherName);
        writerVerbose.println("first_other_p_n_cond_e : " + invdict.get(ent).get(firstOtherName));
        FeaturesDataLine.append(" " + indexFeatures + ":" + invdict.get(ent).get(firstOtherName));
        indexFeatures ++;
        writerVerbose.println("first_other_p_e_cond_n : " + dict.get(firstOtherName).get(ent));
        FeaturesDataLine.append(" " + indexFeatures + ":" + dict.get(firstOtherName).get(ent));
        indexFeatures ++;
        
        double secondOtherScore = otherNamesAndScores.lowerKey(firstOtherScore);
        String secondOtherName = otherNamesAndScores.get(secondOtherScore);
        writerVerbose.println("second_other_name : " + secondOtherName);
        writerVerbose.println("second_other_p_n_cond_e : " + invdict.get(ent).get(secondOtherName));
        FeaturesDataLine.append(" " + indexFeatures + ":" + invdict.get(ent).get(secondOtherName));
        indexFeatures ++;        
        writerVerbose.println("second_other_p_e_cond_n : " + dict.get(secondOtherName).get(ent));
        FeaturesDataLine.append(" " + indexFeatures + ":" + dict.get(secondOtherName).get(ent));
        indexFeatures ++;
        
        writerVerbose.println();
        
        // Print final feature vector in the fle containing just this data.
        writerJustData.println(FeaturesDataLine.toString());
        
        if (!positive) return trainingDataIndex;

        for (String otherName : namesPool.keySet()) {
            if (surroundingParagraph.contains(otherName) &&
                    !otherName.equals(name) &&
                    isInBothCrosswikis(otherName, ent) &&
                    // check if token spans are overlapping
                    surroundingParagraph.indexOf(otherName) <  surroundingParagraph.indexOf(name) + name.length() &&
                    surroundingParagraph.indexOf(name) <  surroundingParagraph.indexOf(otherName) + otherName.length()) {
                String otherNameToEnd = surroundingParagraph.substring(surroundingParagraph.indexOf(otherName), surroundingParagraph.length());
                trainingDataIndex = outputFeaturesForOneName(
                        false, 
                        otherName, 
                        ent,
                        surroundingParagraph,
                        otherNameToEnd,
                        trainingDataIndex,
                        writerVerbose,
                        writerJustData);
            }
        }
        return trainingDataIndex;
    }
    
    
    private void computeForGenericPagesIterator(GenericPagesIterator inputPagesIterator, String outputFileRoot) throws IOException {
        PrintWriter writerVerbose = new PrintWriter(outputFileRoot + ".features_verbose", "UTF-8");
        PrintWriter writerJustData = new PrintWriter(outputFileRoot + ".features_data", "UTF-8");
        
        int trainingDataIndex = 1;
        
        while (inputPagesIterator.hasNext()) {
            GenericSinglePage doc = inputPagesIterator.next();

            for (TruthMention m : doc.truthMentions) {
                String name = m.anchorText;
                String ent = m.wikiUrl;
                int offset = m.mentionOffsetInText;
            
                if (!isInBothCrosswikis(name, ent)) {
                    continue;
                }
                
                int prevIndexOfEOL = offset;
                while (prevIndexOfEOL >= 0 && doc.getRawText().charAt(prevIndexOfEOL) != '\n') {
                    prevIndexOfEOL--;
                }
                if (prevIndexOfEOL > 0) prevIndexOfEOL++;
                int nextIndexOfEOL =  doc.getRawText().indexOf('\n', offset);
                if (nextIndexOfEOL < 0) nextIndexOfEOL = doc.getRawText().length();
                
                String surroundingParagraph = doc.getRawText().substring(prevIndexOfEOL, nextIndexOfEOL); 
                String nameToEndOfParagraph = doc.getRawText().substring(offset, nextIndexOfEOL);
                
                trainingDataIndex = outputFeaturesForOneName(
                        true, 
                        name, 
                        ent, 
                        surroundingParagraph, 
                        nameToEndOfParagraph, 
                        trainingDataIndex, 
                        writerVerbose, 
                        writerJustData);
            }
        }
        
        writerVerbose.flush();
        writerVerbose.close(); 
        writerJustData.flush();
        writerJustData.close(); 
    }
 
}
