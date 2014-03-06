package entity_linking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TreeMap;

// Class for loading a (possibily prunned) Crosswiki dictionary p(e|n) from the dict file
public class LoadCrosswikisDict {

    public static HashMap<String, TreeMap<String, Double>> LoadAndPruneDict(String filename, HashSet<String> allCandidateNames) throws IOException{
        // <string><tab><cprob><space><url>[<space><score>]*
        
        System.err.println("[INFO] Loading and prunning dict P(e|n) ...");

        // dict: P(e|n)
        // dict [name] = treemap<url, cprob>
        HashMap<String, TreeMap<String, Double>> dict =  new HashMap<String, TreeMap<String, Double>>();

        HashMap<String, TreeMap<String, CrosswikisProbability>> tmpDictEntriesForOneName =  new HashMap<String, TreeMap<String, CrosswikisProbability>>();
        
        String currentName = "";
        HashMap<String, Integer> denominators = CrosswikisProbability.initMapOfWs(); // Denominators for all entries of a single name in dict

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

            if (name.compareTo(currentName) != 0) {
                // TODO
                
                currentName = name;
            }
            
            if (!allCandidateNames.contains(name) || !st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            String left = st.nextToken();
            StringTokenizer stLeft = new StringTokenizer(left, " ");
            
            double cprob = Double.parseDouble(stLeft.nextToken());             
            String url = Utils.pruneURL(stLeft.nextToken());           


            CrosswikisProbability crosswProb = new CrosswikisProbability();
            
            
            
            line = in.readLine();
        }
        in.close();     

        System.err.println("[INFO] Done. Size = " + dict.size());
        
        
        return dict;
    }   
    
}
