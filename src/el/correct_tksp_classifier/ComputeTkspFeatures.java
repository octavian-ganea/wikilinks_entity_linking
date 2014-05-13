package el.correct_tksp_classifier;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.stanford.nlp.ling.TaggedWord;
import el.TokenSpan;
import el.crosswikis.LoadCrosswikisDict;
import el.crosswikis.LoadCrosswikisInvdict;
import el.input_data_pipeline.GenericPagesIterator;
import el.input_data_pipeline.GenericSinglePage;
import el.input_data_pipeline.TruthMention;
import el.input_data_pipeline.iitb.IITBPagesIterator;
import el.input_data_pipeline.wikilinks.WikilinksDirectoryParser;
import el.utils.PairOfInts;
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
    
    private HashMap<String, Integer> entsDocFreqsInCorpus;
    private HashMap<String, TreeMap<String, Double>> invdict;
    private HashMap<String, TreeMap<String, Double>> dict;
    
    /*
     * 
     ****** feature headers: 
     * 1:p_n_cond_e 2:p_e_cond_n 
     * 3:numTokens_1 4:numTokens_2 5:numTokens_3 6:numTokens_4 7:numTokens_5 8:numTokens_6 9:numTokens_7 10:numTokens_8 11:numTokens_9 12:numTokens_10 
     * 13:prevTag_NNP 14:prevTag_, 15:prevTag_CD 16:prevTag_NNS 17:prevTag_JJ 18:prevTag_MD 19:prevTag_VB 20:prevTag_DT 21:prevTag_NN 22:prevTag_IN 23:prevTag_. 
         24:prevTag_.$$. 25:prevTag_VBZ 26:prevTag_VBG 27:prevTag_CC 28:prevTag_VBD 29:prevTag_VBN 30:prevTag_RB 31:prevTag_TO 32:prevTag_PRP 33:prevTag_RBR 
         34:prevTag_WDT 35:prevTag_VBP 36:prevTag_RP 37:prevTag_PRP$ 38:prevTag_JJS 39:prevTag_POS 40:prevTag_`` 41:prevTag_EX 42:prevTag_'' 43:prevTag_WP 
         44:prevTag_: 45:prevTag_JJR 46:prevTag_WRB 47:prevTag_$ 48:prevTag_NNPS 49:prevTag_WP$ 50:prevTag_-LRB- 51:prevTag_-RRB- 52:prevTag_PDT 53:prevTag_RBS 
         54:prevTag_FW 55:prevTag_UH 56:prevTag_SYM 57:prevTag_LS 58:prevTag_# 
     * 59:nextTag_NNP 60:nextTag_, 61:nextTag_CD 62:nextTag_NNS 63:nextTag_JJ 64:nextTag_MD 65:nextTag_VB 66:nextTag_DT 67:nextTag_NN 68:nextTag_IN 69:nextTag_. 
         70:nextTag_.$$. 71:nextTag_VBZ 72:nextTag_VBG 73:nextTag_CC 74:nextTag_VBD 75:nextTag_VBN 76:nextTag_RB 77:nextTag_TO 78:nextTag_PRP 79:nextTag_RBR 
         80:nextTag_WDT 81:nextTag_VBP 82:nextTag_RP 83:nextTag_PRP$ 84:nextTag_JJS 85:nextTag_POS 86:nextTag_`` 87:nextTag_EX 88:nextTag_'' 89:nextTag_WP 
         90:nextTag_: 91:nextTag_JJR 92:nextTag_WRB 93:nextTag_$ 94:nextTag_NNPS 95:nextTag_WP$ 96:nextTag_-LRB- 97:nextTag_-RRB- 98:nextTag_PDT 99:nextTag_RBS
         100:nextTag_FW 101:nextTag_UH 102:nextTag_SYM 103:nextTag_LS 104:nextTag_# 
     * 105:firstTag_NNP 106:firstTag_, 107:firstTag_CD 108:firstTag_NNS 109:firstTag_JJ 110:firstTag_MD 111:firstTag_VB 112:firstTag_DT 113:firstTag_NN
         114:firstTag_IN 115:firstTag_. 116:firstTag_.$$. 117:firstTag_VBZ 118:firstTag_VBG 119:firstTag_CC 120:firstTag_VBD 121:firstTag_VBN 122:firstTag_RB 
         123:firstTag_TO 124:firstTag_PRP 125:firstTag_RBR 126:firstTag_WDT 127:firstTag_VBP 128:firstTag_RP 129:firstTag_PRP$ 130:firstTag_JJS 131:firstTag_POS 
         132:firstTag_`` 133:firstTag_EX 134:firstTag_'' 135:firstTag_WP 136:firstTag_: 137:firstTag_JJR 138:firstTag_WRB 139:firstTag_$ 140:firstTag_NNPS 
         141:firstTag_WP$ 142:firstTag_-LRB- 143:firstTag_-RRB- 144:firstTag_PDT 145:firstTag_RBS 146:firstTag_FW 147:firstTag_UH 148:firstTag_SYM 149:firstTag_LS 
         150:firstTag_# 
     * 151:lastTag_NNP 152:lastTag_, 153:lastTag_CD 154:lastTag_NNS 155:lastTag_JJ 156:lastTag_MD 157:lastTag_VB 158:lastTag_DT 159:lastTag_NN 160:lastTag_IN 
         161:lastTag_. 162:lastTag_.$$. 163:lastTag_VBZ 164:lastTag_VBG 165:lastTag_CC 166:lastTag_VBD 167:lastTag_VBN 168:lastTag_RB 169:lastTag_TO 170:lastTag_PRP 
         171:lastTag_RBR 172:lastTag_WDT 173:lastTag_VBP 174:lastTag_RP 175:lastTag_PRP$ 176:lastTag_JJS 177:lastTag_POS 178:lastTag_`` 179:lastTag_EX 180:lastTag_'' 
         181:lastTag_WP 182:lastTag_: 183:lastTag_JJR 184:lastTag_WRB 185:lastTag_$ 186:lastTag_NNPS 187:lastTag_WP$ 188:lastTag_-LRB- 189:lastTag_-RRB- 
         190:lastTag_PDT 191:lastTag_RBS 192:lastTag_FW 193:lastTag_UH 194:lastTag_SYM 195:lastTag_LS 196:lastTag_# 
     * 197:first_other_p_n_cond_e 198:first_other_p_e_cond_n 199:second_other_p_n_cond_e 200:second_other_p_e_cond_n
     */
    public String headers;
    
    private int NUM_NULL_POS_TAGS = 0;
    private int NUM_CORRECT_DATA_INSTANCES = 0;
    private int NUM_TKSPS_WITH_WRONG_OFFSETS = 0;
    private int NUM_TKSPS_ENDING_WITH_DASH = 0;
    private int NUM_TKSPS_ENDING_WITH_DOT = 0;
    private int NUM_TKSPS_ENDING_WITH_COMMA = 0;
    private int NUM_TKSPS_ENDING_WITH_APOSTROPHE = 0;
    private int NUM_TKSPS_ENDING_WITH_LETTER_OR_DIGIT = 0;
    private int NUM_TKSPS_ENDING_WITH_SOMETHING_ELSE = 0;
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////    
    ///////////////////////////// *** Public methods from here *** /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////    

    public ComputeTkspFeatures(String invdictFilename,
            String dictFilename,
            String allEntsFilename) throws IOException {
        initCounters();
        headers = featuresHeaders();
        System.err.println("[INFO] feature headers: " + headers);

        entsDocFreqsInCorpus = LoadWikilinksEntsOrNamesWithFreqs.load(allEntsFilename, "entities");
        invdict = LoadCrosswikisInvdict.load(invdictFilename, null, entsDocFreqsInCorpus);
        dict = LoadCrosswikisDict.load(dictFilename, null, null, entsDocFreqsInCorpus);
    }
    
    public void initCounters() {
        NUM_NULL_POS_TAGS = 0;
        NUM_CORRECT_DATA_INSTANCES = 0;
        NUM_TKSPS_WITH_WRONG_OFFSETS = 0;
        NUM_TKSPS_ENDING_WITH_DASH = 0;
        NUM_TKSPS_ENDING_WITH_DOT = 0;
        NUM_TKSPS_ENDING_WITH_COMMA = 0;
        NUM_TKSPS_ENDING_WITH_APOSTROPHE = 0;
        NUM_TKSPS_ENDING_WITH_LETTER_OR_DIGIT = 0;
        NUM_TKSPS_ENDING_WITH_SOMETHING_ELSE = 0;

    }
    
    public void printCounters() {
        System.err.println("[INFO] NUM_CORRECT_DATA_INSTANCES so far = " + NUM_CORRECT_DATA_INSTANCES);
        System.err.println("[INFO] NUM_TKSPS_WITH_WRONG_OFFSETS so far = " + NUM_TKSPS_WITH_WRONG_OFFSETS);
        System.err.println("[INFO] NUM_NULL_POS_TAGS so far = " + NUM_NULL_POS_TAGS);

        System.err.println("[INFO] NUM_TKSPS_ENDING_WITH_DASH so far = " + NUM_TKSPS_ENDING_WITH_DASH);
        System.err.println("[INFO] NUM_TKSPS_ENDING_WITH_DOT so far = " + NUM_TKSPS_ENDING_WITH_DOT);
        System.err.println("[INFO] NUM_TKSPS_ENDING_WITH_COMMA so far = " + NUM_TKSPS_ENDING_WITH_COMMA);
        System.err.println("[INFO] NUM_TKSPS_ENDING_WITH_APOSTROPHE so far = " + NUM_TKSPS_ENDING_WITH_APOSTROPHE);
        System.err.println("[INFO] NUM_TKSPS_ENDING_WITH_LETTER_OR_DIGIT so far = " + NUM_TKSPS_ENDING_WITH_LETTER_OR_DIGIT);
        System.err.println("[INFO] NUM_TKSPS_ENDING_WITH_SOMETHING_ELSE so far = " + NUM_TKSPS_ENDING_WITH_SOMETHING_ELSE);
    }
    
    public TreeMap<Integer, Double> computeFeaturesVectorForOneName(
            String ent,
            TokenSpan tksp, 
            String surroundingParagraph) {
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        outputFeaturesForOneName(
                false, 
                ent,
                tksp,
                surroundingParagraph,
                new PrintWriter(new StringWriter()),
                pw);
        
        TreeMap<Integer, Double> rez = new TreeMap<Integer, Double>();
        
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
    
  
    // Output each meaning of each feature index.
    public static String featuresHeaders() {
        int indexFeatures = 1;
        StringBuilder sb = new StringBuilder();
        
        sb.append(indexFeatures + ":p_n_cond_e ");
        indexFeatures ++;

        sb.append(indexFeatures + ":p_e_cond_n ");
        indexFeatures++;
        sb.append("\n");

        for (int i = 1; i <= 10; i++, indexFeatures++) {
            sb.append(indexFeatures + ":numTokens_" + i + " ");
        }
        sb.append("\n");
        for (int i = 0; i < Utils.posTagger.numTags(); i++, indexFeatures++) {
            sb.append(indexFeatures + ":prevTag_" + Utils.posTagger.getTag(i) + " ");
        }
        sb.append("\n");
        for (int i = 0; i < Utils.posTagger.numTags(); i++, indexFeatures++) {
            sb.append(indexFeatures + ":nextTag_" + Utils.posTagger.getTag(i) + " ");
        }
        sb.append("\n");
        for (int i = 0; i < Utils.posTagger.numTags(); i++, indexFeatures++) {
            sb.append(indexFeatures + ":firstTag_" + Utils.posTagger.getTag(i) + " ");
        }
        sb.append("\n");
        for (int i = 0; i < Utils.posTagger.numTags(); i++, indexFeatures++) {
            sb.append(indexFeatures + ":lastTag_" + Utils.posTagger.getTag(i) + " ");
        }
        sb.append("\n");
        
        sb.append(indexFeatures + ":first_other_p_n_cond_e ");
        indexFeatures++;

        sb.append(indexFeatures + ":first_other_p_e_cond_n ");
        indexFeatures++;

        sb.append(indexFeatures + ":second_other_p_n_cond_e ");
        indexFeatures++;

        sb.append(indexFeatures + ":second_other_p_e_cond_n ");
        indexFeatures++;

        return sb.toString();
    }
    
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////    
    //////////////////////////////// Private methods from here /////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////    
    
     
    private void computeForGenericPagesIterator(GenericPagesIterator inputPagesIterator, String outputFileRoot) throws IOException {
        initCounters();
        
        PrintWriter writerVerbose = new PrintWriter(outputFileRoot + ".features_verbose", "UTF-8");
        PrintWriter writerJustData = new PrintWriter(outputFileRoot + ".features_data", "UTF-8");
        
        int nr_doc = 0;
        while (inputPagesIterator.hasNext()) {
            nr_doc ++;
            GenericSinglePage doc = inputPagesIterator.next();
            
            if (nr_doc % 10000 == 0) {
                System.err.println("\n[INFO] num docs so far = " + nr_doc);
                printCounters();
            }

            for (TruthMention m : doc.truthMentions) {
                String name = m.anchorText;
                String ent = m.wikiUrl;
                int offset = m.mentionOffsetInText;
                if (doc.getRawText().indexOf(name, offset) != offset) {
                    NUM_TKSPS_WITH_WRONG_OFFSETS++;
                    continue;
                }
            
                if (!isInBothCrosswikis(name, ent)) {
                    continue;
                }
                
                int prevIndexOfEOL = offset;
                while (prevIndexOfEOL >= 0 && doc.getRawText().charAt(prevIndexOfEOL) != '\n') {
                    prevIndexOfEOL--;
                }
                if (prevIndexOfEOL >= 0) prevIndexOfEOL++;
                else prevIndexOfEOL = 0;
                
                int nextIndexOfEOL =  doc.getRawText().indexOf('\n', offset);
                if (nextIndexOfEOL < 0) nextIndexOfEOL = doc.getRawText().length();
                
                String surroundingParagraph = doc.getRawText().substring(prevIndexOfEOL, nextIndexOfEOL); 
                
                outputFeaturesForOneName(
                        true,
                        ent,
                        new TokenSpan(offset - prevIndexOfEOL, name), // The new index from the surrounding paragraph.
                        surroundingParagraph,
                        writerVerbose, 
                        writerJustData);
            }
        }
        
        writerVerbose.flush();
        writerVerbose.close(); 
        writerJustData.flush();
        writerJustData.close();
        
        printCounters();
    }
    
    private boolean isInBothCrosswikis(String name, String ent) {
        
        if (!invdict.containsKey(ent) || !invdict.get(ent).containsKey(name) || !dict.containsKey(name) || !dict.get(name).containsKey(ent)) {
            return false;
        }
        return true;
    }

    // Find all other possible token spans that might be candidates for being linked with this entity.
    private Vector<TokenSpan> getOtherOverlappingValidMentionsInTheSameParagraph(
            String ent, 
            TokenSpan tksp, 
            ArrayList<TaggedWord> surroundingParagraphTags, 
            String surroundingParagraph) {
        
        Vector<TokenSpan> result = new Vector<TokenSpan>();

        if (!isInBothCrosswikis(tksp.name, ent)) return result;

        for (String otherName : invdict.get(ent).keySet()) {
            if (!otherName.equals(tksp.name) && otherName.length() > 0 && isInBothCrosswikis(otherName, ent)) {
                int indexOfOtherName = surroundingParagraph.indexOf(otherName);
                
                while (indexOfOtherName >= 0) {
                    PairOfInts tagIndexesOfOtherName = Utils.getStartAndEndIndexesOfTkspInContextTags(
                            surroundingParagraph, surroundingParagraphTags, new TokenSpan(indexOfOtherName, otherName));

                    // check if token spans are overlapping and if otherName is a separate word in the text
                    if (tagIndexesOfOtherName.x >= 0 && tagIndexesOfOtherName.y >= 0 &&
                            indexOfOtherName < tksp.offset + tksp.name.length() &&
                            tksp.offset < indexOfOtherName + otherName.length()) {
                        result.add(new TokenSpan(indexOfOtherName, otherName));
                    }

                    indexOfOtherName = surroundingParagraph.indexOf(otherName, indexOfOtherName + otherName.length());
                }
            }
        }        
        return result;
    }
    
    // If this is positive, then it will output also the negative examples that 
    private void outputFeaturesForOneName(
            boolean positive,
            String ent,
            TokenSpan tksp,
            String surroundingParagraph,
            PrintWriter writerVerbose,
            PrintWriter writerJustData) {

        if (!isInBothCrosswikis(tksp.name, ent)) {
            return;
        }

        ArrayList<TaggedWord> surroundingParagraphTags = Utils.getPosTags(surroundingParagraph);

        // Find all other possible token spans that might be candidates for being linked with this entity.
        Vector<TokenSpan> otherTksps = getOtherOverlappingValidMentionsInTheSameParagraph(ent, tksp, surroundingParagraphTags, surroundingParagraph);
        
        // Check if this is not the only name candidate for this entity in this sentence (in which case we have nothing to do).
        // If we don't have other examples in this sentence, we skip.
        if (otherTksps.size() == 0) return;
        

        // Represent features in the format : label [index:value]*  required by LibSVM and LibLinear
        StringBuilder featuresDataLine = new StringBuilder();
        StringWriter featuresVerboseLines = new StringWriter();
        PrintWriter featuresVerboseWriter = new PrintWriter(featuresVerboseLines);
        
        NUM_CORRECT_DATA_INSTANCES++;        
        featuresVerboseWriter.println("trainingDataIndex : " + NUM_CORRECT_DATA_INSTANCES);

        int indexFeatures = 1;
        
        if (positive) {
            featuresVerboseWriter.println("Positive");
        } else {
            featuresVerboseWriter.println("Negative");            
        }
        featuresDataLine.append(positive ? 1 : 0);
        
        featuresVerboseWriter.println("ent : " + ent);
        featuresVerboseWriter.println("name : " + tksp.name);
        
        featuresVerboseWriter.println("p_n_cond_e : " + invdict.get(ent).get(tksp.name));
        featuresDataLine.append(" " + indexFeatures + ":" + invdict.get(ent).get(tksp.name));
        indexFeatures ++;
        
        featuresVerboseWriter.println("p_e_cond_n : " + dict.get(tksp.name).get(ent));
        featuresDataLine.append(" " + indexFeatures + ":" + dict.get(tksp.name).get(ent));
        indexFeatures++;
        
        int numTokens = Utils.getTokens(tksp.name).size();
        featuresVerboseWriter.println("num_tokens : " + numTokens);
        if (numTokens > 10) numTokens = 10;
        for (int i = 1; i <= 10; i++, indexFeatures++) {
            if (i == numTokens) {
                featuresDataLine.append(" " + indexFeatures + ":1");
            } else {
                featuresDataLine.append(" " + indexFeatures + ":0");                
            }
        }
        
        ArrayList<TaggedWord> posTags = Utils.getFirstLastPreviousAndNextTokensAndPosTags(surroundingParagraph, surroundingParagraphTags, tksp);
        if (posTags == null) {
            NUM_CORRECT_DATA_INSTANCES--;

            if (positive) {
                NUM_NULL_POS_TAGS++;
                int nextChar = tksp.offset + tksp.name.length();
                if (nextChar < surroundingParagraph.length()) {
                    if (surroundingParagraph.charAt(nextChar) == '-') {
                        NUM_TKSPS_ENDING_WITH_DASH ++;
                        return;
                    }
                    if (surroundingParagraph.charAt(nextChar) == '.') {
                        NUM_TKSPS_ENDING_WITH_DOT ++;
                        return;
                    }
                    if (surroundingParagraph.charAt(nextChar) == ',') {
                        NUM_TKSPS_ENDING_WITH_COMMA ++;
                        return;
                    }
                    if (surroundingParagraph.charAt(nextChar) == '\'') {
                        NUM_TKSPS_ENDING_WITH_APOSTROPHE ++;
                        return;
                    }
                    if (Character.isLetterOrDigit(surroundingParagraph.charAt(nextChar))) {
                        NUM_TKSPS_ENDING_WITH_LETTER_OR_DIGIT ++;
                        return;
                    }
                }
                int prevChar = tksp.offset  - 1;
                if (prevChar >= 0) {
                    if (surroundingParagraph.charAt(prevChar) == '-') {
                        NUM_TKSPS_ENDING_WITH_DASH ++;
                        return;
                    }
                    if (surroundingParagraph.charAt(prevChar) == '.') {
                        NUM_TKSPS_ENDING_WITH_DOT ++;
                        return;
                    }
                    if (surroundingParagraph.charAt(prevChar) == ',') {
                        NUM_TKSPS_ENDING_WITH_COMMA ++;
                        return;
                    }
                    if (surroundingParagraph.charAt(prevChar) == '\'') {
                        NUM_TKSPS_ENDING_WITH_APOSTROPHE ++;
                        return;
                    }
                    if (Character.isLetterOrDigit(surroundingParagraph.charAt(prevChar))) {
                        NUM_TKSPS_ENDING_WITH_LETTER_OR_DIGIT ++;
                        return;
                    }                    
                }
                NUM_TKSPS_ENDING_WITH_SOMETHING_ELSE++;
                System.err.println("[NULL POSTAGS] name " + tksp.name + " context=" + surroundingParagraph.substring(Math.max(0, tksp.offset - 20), 
                        Math.min(tksp.offset + 20 + tksp.name.length(), surroundingParagraph.length() - 1)));
                System.err.println("[NULL POSTAGS] surrounding = " + surroundingParagraph);
            }
            return;
        }
        
        featuresVerboseWriter.println("prev_token : " + posTags.get(0).word());
        featuresVerboseWriter.println("prev_tag : " + posTags.get(0).tag());
        for (int i = 0; i < Utils.posTagger.numTags(); i++, indexFeatures++) {
            if (Utils.posTagger.getTag(i).equals(posTags.get(0).tag())) {
                featuresDataLine.append(" " + indexFeatures + ":1");
            } else {
                featuresDataLine.append(" " + indexFeatures + ":0");                
            }
        }
        
        featuresVerboseWriter.println("next_token : " + posTags.get(1).word());
        featuresVerboseWriter.println("next_tag : " + posTags.get(1).tag());
        for (int i = 0; i < Utils.posTagger.numTags(); i++, indexFeatures++) {
            if (Utils.posTagger.getTag(i).equals(posTags.get(1).tag())) {
                featuresDataLine.append(" " + indexFeatures + ":1");
            } else {
                featuresDataLine.append(" " + indexFeatures + ":0");                
            }
        }
        
        featuresVerboseWriter.println("first_token : " + posTags.get(2).word());
        featuresVerboseWriter.println("first_tag : " + posTags.get(2).tag());
        for (int i = 0; i < Utils.posTagger.numTags(); i++, indexFeatures++) {
            if (Utils.posTagger.getTag(i).equals(posTags.get(2).tag())) {
                featuresDataLine.append(" " + indexFeatures + ":1");
            } else {
                featuresDataLine.append(" " + indexFeatures + ":0");                
            }
        }
        
        featuresVerboseWriter.println("last_token : " + posTags.get(3).word());
        featuresVerboseWriter.println("last_tag : " + posTags.get(3).tag());
        for (int i = 0; i < Utils.posTagger.numTags(); i++, indexFeatures++) {
            if (Utils.posTagger.getTag(i).equals(posTags.get(3).tag())) {
                featuresDataLine.append(" " + indexFeatures + ":1");
            } else {
                featuresDataLine.append(" " + indexFeatures + ":0");                
            }
        }
        
        // Other token spans that might be candidates will be sorted by p(e|n)*p(n|e) and top 2 will be considered:
        TreeMap<Double, String> otherNamesAndScores = new TreeMap<Double, String>();
        
        for (TokenSpan otherTksp : otherTksps) {
            double score = invdict.get(ent).get(otherTksp.name) * dict.get(otherTksp.name).get(ent);
            otherNamesAndScores.put(score, otherTksp.name);
        }

        double firstOtherScore = otherNamesAndScores.lastKey();
        String firstOtherName = otherNamesAndScores.get(firstOtherScore);
        featuresVerboseWriter.println("first_other_name : " + firstOtherName);
        featuresVerboseWriter.println("first_other_p_n_cond_e : " + invdict.get(ent).get(firstOtherName));
        featuresDataLine.append(" " + indexFeatures + ":" + invdict.get(ent).get(firstOtherName));
        indexFeatures ++;
        featuresVerboseWriter.println("first_other_p_e_cond_n : " + dict.get(firstOtherName).get(ent));
        featuresDataLine.append(" " + indexFeatures + ":" + dict.get(firstOtherName).get(ent));
        indexFeatures ++;
        
        // The second one might be null.
        Double secondOtherScore = otherNamesAndScores.lowerKey(firstOtherScore);
        String secondOtherName = (secondOtherScore == null ? null : otherNamesAndScores.get(secondOtherScore));
        featuresVerboseWriter.println("second_other_name : " + secondOtherName);
        double second_other_p_n_cond_e = (secondOtherName == null ? 0 : invdict.get(ent).get(secondOtherName));
        featuresVerboseWriter.println("second_other_p_n_cond_e : " + second_other_p_n_cond_e);
        featuresDataLine.append(" " + indexFeatures + ":" + second_other_p_n_cond_e);
        indexFeatures ++;
        double second_other_p_e_cond_n = (secondOtherName == null ? 0 : dict.get(secondOtherName).get(ent));
        featuresVerboseWriter.println("second_other_p_e_cond_n : " + second_other_p_e_cond_n);
        featuresDataLine.append(" " + indexFeatures + ":" + second_other_p_e_cond_n);
        indexFeatures ++;
        
        featuresVerboseWriter.println();
        
        // Print final strings:
        writerVerbose.print(featuresVerboseLines.toString());
        writerJustData.println(featuresDataLine.toString());
        
        if (!positive) return;

        for (TokenSpan otherTksp : otherTksps) {
            outputFeaturesForOneName(false, ent, otherTksp, surroundingParagraph, writerVerbose, writerJustData);
        }
        return;
    }
}
