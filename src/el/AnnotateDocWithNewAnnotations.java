/*
 * Extract sentences from one document and include annotate them with 
 * annotations from a given input list.
 */
package el;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import el.input_data_pipeline.GenericSinglePage;
import el.input_data_pipeline.TruthMention;

public class AnnotateDocWithNewAnnotations {
    static public Vector<String> extractSentencesWithStanfordNLP(String text) {
        Vector<String> rez = new Vector<String>();
        StringTokenizer st = new StringTokenizer(text, "\n");
        while (st.hasMoreTokens()) {
            String par = st.nextToken();
            int dot = par.lastIndexOf('.');
            int exclm = par.lastIndexOf('!');
            int interog = par.lastIndexOf('?');
            int x = Math.max(dot, exclm);
            x = Math.max(x, interog);
            if (x == -1) continue;
            par = par.substring(0, x+1);
            if (par.contains(" ")) {
                PTBTokenizer ptbt = new PTBTokenizer(
                        new StringReader(par), new CoreLabelTokenFactory(), "ptb3Escaping=false");

                List<List<CoreLabel>> sents = (new WordToSentenceProcessor()).process(ptbt.tokenize());
                for (List<CoreLabel> sent : sents) {
                    StringBuilder sb = new StringBuilder("");
                    for (CoreLabel w : sent) sb.append(w + " ");
                    rez.add(sb.toString());
                }               
            }           
        }
        return rez;
    }
    
    
    static public void run(GenericSinglePage doc, Vector<Candidate> matchings) {

        HashMap<String,String> wikiFreebaseMap = new HashMap<String,String>();
        
        for (TruthMention m : doc.truthMentions) {
            wikiFreebaseMap.put(m.wikiUrl, m.freebaseId);
        }
        
        TreeMap<Integer, Candidate> sortedByOffsetMatchings = new TreeMap<Integer, Candidate>();
        for (Candidate c : matchings) {
            if (wikiFreebaseMap.containsKey(c.entityURL)) {
                c.freebaseID = wikiFreebaseMap.get(c.entityURL);
            }
            if (!sortedByOffsetMatchings.containsKey(c.textIndex) ||
                    sortedByOffsetMatchings.get(c.textIndex).name.length() < c.name.length()) {
                sortedByOffsetMatchings.put(c.textIndex, c);
            }
        }
        Vector<Candidate> finalMatchings = new Vector<Candidate>();
        for (Candidate c : sortedByOffsetMatchings.values()) {
            if (finalMatchings.isEmpty()) {
                finalMatchings.add(c);
            } else {
                Candidate last = finalMatchings.lastElement(); 
                if (last.textIndex + last.name.length() <= c.textIndex) {
                    finalMatchings.add(c);
                }
            }
        }
        
        int finalMatchingsIndex = 0;
        if (finalMatchings.size() == 0) return;
        
        Candidate currentCandidate = finalMatchings.get(finalMatchingsIndex);
        
        StringBuilder allTextBuilder = new StringBuilder();
        for (int off = 0; off < doc.getRawText().length(); off++) {
            if (currentCandidate != null && currentCandidate.textIndex == off) {
                allTextBuilder.append("[[[]]]" + currentCandidate.name + "{{{" + finalMatchingsIndex + "}}}");

                off += currentCandidate.name.length() - 1;
                
                finalMatchingsIndex++;
                if (finalMatchings.size() == finalMatchingsIndex) {
                    currentCandidate = null;
                } else {
                    currentCandidate = finalMatchings.get(finalMatchingsIndex);                            
                }
            } else {
                allTextBuilder.append(doc.getRawText().charAt(off));
            }
        }
        
        // Use Stanford NLP framework to extract sentences from the text.
        Vector<String> properSentences = extractSentencesWithStanfordNLP(allTextBuilder.toString());
        
        for (String sentence : properSentences) {
            if (sentence.contains("[ [ [") && sentence.contains("] ] ]")) {
                if (sentence.indexOf("{ { {") <= sentence.indexOf("[ [ [") ||
                        sentence.lastIndexOf("{ { {") <= sentence.lastIndexOf("[ [ [") ) {
                    continue;
                }
                
                StringBuilder sb = new StringBuilder();
                boolean good = true; 
                for (int off = 0; off < sentence.length(); off++) {
                    if (off + "{ { {".length() <= sentence.length() &&
                            sentence.substring(off, off + "{ { {".length()).compareTo("{ { {") == 0) {
                        
                        if (sentence.indexOf("} } }", off) < 0) {
                            good = false;
                            break;
                        }
                        int index = -1;
                        try {
                            index = Integer.parseInt(
                                sentence.substring(
                                        off + "{ { {".length(), 
                                        sentence.indexOf("} } }", off)).trim());
                        } catch (NumberFormatException e) {
                            sb.append(sentence.charAt(off));
                            continue;
                        }
                        
                        if (index == -1 || index >= finalMatchings.size()) {
                            sb.append(sentence.charAt(off));
                            continue;
                        }
                        
                        currentCandidate = finalMatchings.get(index);
                        
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
                            sb.replace(sb.length() - 1, sb.length(), "");
                        }
                        
                        sb.append("{{{" + currentCandidate.posteriorProb + ";" + 
                                currentCandidate.freebaseID + ";wiki/" + currentCandidate.entityURL + "}}} ");
                        
                        off = sentence.indexOf("} } }", off) + "} } }".length();
                    } else if (off + "[ [ [".length() <= sentence.length() &&
                            sentence.substring(off, off + "[ [ [".length()).compareTo("[ [ [") == 0) {
                        sb.append("[[[]]]");
                        
                        if (sentence.indexOf("] ] ]", off) < 0) {
                            good = false;
                            break;
                        }
                        off = sentence.indexOf("] ] ]", off) + "] ] ]".length();
                    } else {
                        sb.append(sentence.charAt(off));
                    }
                }

                if (good) System.out.println(sb.toString());
            }
        }
    }
    
}
