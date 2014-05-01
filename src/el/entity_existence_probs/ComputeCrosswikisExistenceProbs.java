package el.entity_existence_probs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import el.utils.Utils;
import el.wikilinks_ents_or_names_with_freqs.LoadWikilinksEntsOrNamesWithFreqs;
import el.wikipedia_redirects.WikiRedirects;

// Computes p(\exists e | n) by looking at avg_e {p(n|e)/p(e|n,\exits e) * p(e)/p(n)} over all entities e
public class ComputeCrosswikisExistenceProbs {
    private static BufferedReader invdictReader = null;
    private static String invdictLine = null;
    
    private static BufferedReader dictReader = null;
    private static String dictLine = null;
    
   
    private static HashMap<String, TreeMap<String, Double>> loadNextChunckOfDictForOneName() throws IOException {
        HashMap<String, TreeMap<String, Double>> map = new HashMap<String, TreeMap<String, Double>>();
        
        String name = "";

        // <string><tab><cprob><space><url>[<space><score>]*
        while (dictLine != null && dictLine.length() > 0 ) {
            StringTokenizer st = new StringTokenizer(dictLine, "\t");

            if (!st.hasMoreTokens()) {
                dictLine = dictReader.readLine();
                continue;
            }

            String mention = st.nextToken();
            
            String left = st.nextToken();
            StringTokenizer stLeft = new StringTokenizer(left, " ");
            
            double cprob = Double.parseDouble(stLeft.nextToken());             
            String url = WikiRedirects.pruneURL(stLeft.nextToken());  
            
            if (url.length() == 0) {
                dictLine = dictReader.readLine();
                continue;
            }

            if (cprob < 0.00001) {
                dictLine = dictReader.readLine();
                continue;             
            }

            if (name.compareTo("") == 0) {
                name = mention;
                map.put(name, new TreeMap<String,Double>());
            }

            if (name.compareTo(mention) != 0) {
                return map;
            }

            map.get(name).put(url, cprob);
            dictLine = dictReader.readLine();
        }
        
        return null;
    }
    
    
    private static HashMap<String, TreeMap<String, Double>> loadNextChunckOfInvdictForOneName() throws IOException {
        HashMap<String, TreeMap<String, Double>> map = new HashMap<String, TreeMap<String, Double>>();
        
        String name = "";

        // <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]
        while (invdictLine != null && invdictLine.length() > 0 ) {
            StringTokenizer st = new StringTokenizer(invdictLine, "\t");

            if (!st.hasMoreTokens()) {
                invdictLine = invdictReader.readLine();
                continue;
            }

            String rawURL = st.nextToken();
            String url = WikiRedirects.pruneURL(rawURL);

            if (url.length() == 0) {
                invdictLine = invdictReader.readLine();
                continue;
            }

            String left = st.nextToken();
            double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));  

            if (cprob < 0.00001) {
                invdictLine = invdictReader.readLine();
                continue;             
            }

            String mention = left.substring(left.indexOf(" ") + 1);
            if (name.compareTo("") == 0) {
                name = mention;
                map.put(name, new TreeMap<String,Double>());
            }

            if (name.compareTo(mention) != 0) {
                return map;
            }

            map.get(name).put(url, cprob);
            invdictLine = invdictReader.readLine();
        }
        
        return null;
    }
    
    public static void compute(String allEntsFilename, String allNamesFilename, String invdictSortedFilename, String dictFilename) throws IOException {
        HashMap<String, Integer> entsDocFreqsInCorpus = LoadWikilinksEntsOrNamesWithFreqs.load(allEntsFilename, "entities");
        HashMap<String, Integer> namesDocFreqsInCorpus = LoadWikilinksEntsOrNamesWithFreqs.load(allNamesFilename, "names");
        
        invdictReader = new BufferedReader(new FileReader(invdictSortedFilename));
        invdictLine = invdictReader.readLine();

        dictReader = new BufferedReader(new FileReader(dictFilename));
        dictLine = dictReader.readLine();

        System.err.println("[INFO] Starting to compute the p(\\exist e | n ) ... ");
        
        HashMap<String, TreeMap<String, Double>> invdictMap;
        HashMap<String, TreeMap<String, Double>> dictMap;
        
        invdictMap = loadNextChunckOfInvdictForOneName();
        dictMap = loadNextChunckOfDictForOneName();

        
        int num1 = 0, num2 = 0, num3 = 0, num410=0, num11 = 0;
        
        // We look at names appearing in both invdictMap and dictMap
        while (invdictMap != null && dictMap != null) {
            String nameInvdict = invdictMap.entrySet().iterator().next().getKey();
            String nameDict = dictMap.entrySet().iterator().next().getKey();
            
            if (nameInvdict.compareTo(nameDict) < 0) {
                invdictMap = loadNextChunckOfInvdictForOneName();
            } else if (nameInvdict.compareTo(nameDict) > 0) {
                dictMap = loadNextChunckOfDictForOneName();
            } else {
                String name = nameDict;
                if (namesDocFreqsInCorpus.containsKey(name)) {
                    double avgScore = 0;
                    int numEnts = 0;

                    double weightedByDictScore = 0;
                    double totalSumWeightsByDict = 0;

                    double weightedByNumEntsScore = 0;
                    double totalSumWeightsByNumEnts = 0;

                    
                    double maxScore = 0.0;
                    double minScore = 1.0;
                    
                    for (String url : dictMap.get(name).keySet()) {
                        if (invdictMap.get(name).containsKey(url) && entsDocFreqsInCorpus.containsKey(url) && namesDocFreqsInCorpus.containsKey(name)) {
                            double score = invdictMap.get(name).get(url) * entsDocFreqsInCorpus.get(url) / (dictMap.get(name).get(url) * namesDocFreqsInCorpus.get(name));
                            numEnts++;
                            avgScore += score;
                            
                            weightedByDictScore += score * dictMap.get(name).get(url);
                            totalSumWeightsByDict += dictMap.get(name).get(url);
                            
                            maxScore = Math.max(maxScore, score);
                            minScore = Math.min(minScore, score);
                            
                            weightedByNumEntsScore += score * entsDocFreqsInCorpus.get(url);
                            totalSumWeightsByNumEnts += entsDocFreqsInCorpus.get(url);
                            
                            //System.out.println("name=" + name + "; url=" + url + ";score=" + score + "  ;p(n|e)=" + invdictMap.get(name).get(url) + " ;p(e|n,exist)=" + dictMap.get(name).get(url) + " ;p(e)=" + entsDocFreqsInCorpus.get(url) + " ;p(n)=" + namesDocFreqsInCorpus.get(name) );
                        }
                    }
                    
                    if (numEnts > 0) {
                        avgScore /= numEnts;
                    }
                    
                    if (totalSumWeightsByDict > 0) {
                        weightedByDictScore /= totalSumWeightsByDict;
                    }
                    
                    if (totalSumWeightsByNumEnts > 0) {
                        weightedByNumEntsScore /= totalSumWeightsByNumEnts;
                    }
                    
                    System.out.println(name + "\t" + "numEnts " + numEnts + " ;avg " + avgScore + " ;weightedByDictScore " + weightedByDictScore + " ;weightedByNumEntsScore " + weightedByNumEntsScore + " ;maxScore " + maxScore + " ;minScore " + minScore);
                    
                    if (numEnts == 1) num1++;
                    if (numEnts == 2) num2++;
                    if (numEnts == 3) num3++;
                    if (numEnts >3 && numEnts <= 10) num410++;
                    if (numEnts > 10) num11++;
                } 
                invdictMap = loadNextChunckOfInvdictForOneName();
                dictMap = loadNextChunckOfDictForOneName();
            }
        }
        
        System.out.println("Num 1 =" + num1);
        System.out.println("Num 2 =" + num2);
        System.out.println("Num 3 =" + num3);
        System.out.println("Num 4 - 10 =" + num410);
        System.out.println("Num > 10 =" + num11);
        num1 += num2 + num3 + num410 + num11;
        System.out.println("Total names =" + num1);
    }
    
}
