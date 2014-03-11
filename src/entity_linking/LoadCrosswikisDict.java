package entity_linking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

//For printing Dict in descending order by score
class DictEntry implements Comparable {
    String url;
    Double score;
    CrosswikisProbabilityForDict cpd;
    
    public DictEntry(String url, Double score, CrosswikisProbabilityForDict cpd) {
        this.url = url;
        this.score = score;
        this.cpd = cpd;
    }
    
    @Override
    public int compareTo(Object o) {
        DictEntry ie = (DictEntry) o;
        if (score < ie.score) return 1;
        if (score > ie.score) return -1;
        return url.compareTo(ie.url);
    }
}

//TODO: 2 TODOs down here if you want to do a merging on inv.dict from the beginning

// Class for loading a (possibily prunned) Crosswiki dictionary p(e|n) from the dict file
public class LoadCrosswikisDict {

    public static HashMap<String, TreeMap<String, Double>> load(String filename, HashSet<String> allCandidateNames) throws IOException{
        // <string><tab><cprob><space><url>[<space><score>]*
        
        System.err.println("[INFO] Loading and prunning dict P(e|n) ...");

        // dict: P(e|n)
        // dict [name] = treemap<url, cprob>
        HashMap<String, TreeMap<String, Double>> dict =  new HashMap<String, TreeMap<String, Double>>();
        
        String currentName = "";
        HashMap<String, Integer> denominators = null; // Denominators for all entries of a single name in dict
        TreeMap<String, CrosswikisProbabilityForDict> tmpDictEntriesForOneName = null;  // Partial index for one name

        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        int nr_line = 0;
        
        while (line != null && line.length() > 0) {
            if (nr_line % 20000000 == 0) {
                System.err.println("loaded " + nr_line);
            }
            nr_line ++;
            StringTokenizer st = new StringTokenizer(line, "\t");

            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }
            
            String name = st.nextToken();
            if (!allCandidateNames.contains(name) || !st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }
            
            // When name changes.
            if (name.compareTo(currentName) != 0) {
                if (currentName.length() > 0 && tmpDictEntriesForOneName != null && denominators != null) {
                    if (dict.containsKey(currentName)) {
                        System.err.println("[FATAL] Error in processing Crosswikis dict because of name: " + currentName);
                        System.exit(1);
                    }
                    dict.put(currentName, new TreeMap<String, Double>());

                    Vector<DictEntry> v = new Vector<DictEntry>();
                    
                    for (String url : tmpDictEntriesForOneName.keySet()) {
                        CrosswikisProbabilityForDict cp = tmpDictEntriesForOneName.get(url);
                        cp.setDenominator(denominators);
                        dict.get(currentName).put(url, cp.getScore());
                        
                        v.add(new DictEntry(url, dict.get(currentName).get(url), cp));
                    }
                    
                    // TODO: uncomment this if you want to do a merging on inv.dict from the beginning
                    /*
                    DecimalFormat df = new DecimalFormat("#.######");
                    Collections.sort(v);
                    for (DictEntry ie : v) {
                        System.out.println(currentName + "\t" + df.format(ie.score) + " " + ie.url + " " + ie.cpd);
                    }
                    */
                }
                
                denominators = CrosswikisProbability.initMapOfWs();
                tmpDictEntriesForOneName =  new TreeMap<String, CrosswikisProbabilityForDict>();
                currentName = name;
            }
            
            String left = st.nextToken();
            StringTokenizer stLeft = new StringTokenizer(left, " ");
            
            double cprob = Double.parseDouble(stLeft.nextToken());             
            String url = Utils.pruneURL(stLeft.nextToken());           

            if (!tmpDictEntriesForOneName.containsKey(url)) {
                tmpDictEntriesForOneName.put(url, new CrosswikisProbabilityForDict());
            }
            while (stLeft.hasMoreTokens()) {
                tmpDictEntriesForOneName.get(url).addNumeratorsAndUpdateDenominators(stLeft.nextToken(), denominators);
            }
            
            line = in.readLine();
        }
        
        // End of dict. Add the remaining entries.
        if (currentName.length() > 0 && tmpDictEntriesForOneName != null && denominators != null) {
            if (dict.containsKey(currentName)) {
                System.err.println("[FATAL] Error in processing Crosswikis dict because of name: " + currentName);
                System.exit(1);
            }
            dict.put(currentName, new TreeMap<String, Double>());

            Vector<DictEntry> v = new Vector<DictEntry>();
            
            for (String url : tmpDictEntriesForOneName.keySet()) {
                CrosswikisProbabilityForDict cp = tmpDictEntriesForOneName.get(url);
                cp.setDenominator(denominators);
                dict.get(currentName).put(url, cp.getScore());
                
                v.add(new DictEntry(url, dict.get(currentName).get(url), cp));
            }
            
            // TODO: uncomment this if you want to do a merging on inv.dict from the beginning
            /*
            DecimalFormat df = new DecimalFormat("#.######");
            Collections.sort(v);
            for (DictEntry ie : v) {
                System.out.println(currentName + "\t" + df.format(ie.score) + " " + ie.url + " " + ie.cpd);
            }
            */
        }
        in.close();     
        System.err.println("[INFO] Done. Size = " + dict.size());       
    
        return dict;
    }   
    
}
