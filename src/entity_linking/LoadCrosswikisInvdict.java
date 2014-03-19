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

// For printing Invdict in descending order by score
class InvdictEntry implements Comparable {
    String name;
    Double score;
    CrosswikisProbabilityForInvdict cpi;

    public InvdictEntry(String name, Double score, CrosswikisProbabilityForInvdict cpi) {
        this.name = name;
        this.score = score;
        this.cpi = cpi;
    }

    @Override
    public int compareTo(Object o) {
        InvdictEntry ie = (InvdictEntry) o;
        if (score < ie.score) return 1;
        if (score > ie.score) return -1;
        return name.compareTo(ie.name);
    }
}

// TODO: 2 TODOs down here if you want to do a merging on inv.dict from the beginning

//Class for loading a (possibily prunned) Crosswiki dictionary p(n|e) from the inv.dict file
public class LoadCrosswikisInvdict {

    public static HashMap<String, TreeMap<String, Double>> load(
            String filename,
            HashSet<String> allEntitiesFromAllPages, 
            HashMap<String, Integer> entsDocFreqsInCorpus) throws IOException{        

        // <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]
        System.err.println("[INFO] Loading and prunning the inv.dict P(n|e) ...");

        HashMap<String, TreeMap<String, CrosswikisProbabilityForInvdict>> tmpInvdict =
            new HashMap<String, TreeMap<String, CrosswikisProbabilityForInvdict>>();

        String currentURL = "";
        String currentRawURL = "";
        HashMap<String, Integer> denominators = null; // Denominators for all entries of a single URL in inv.dict
        TreeMap<String, CrosswikisProbabilityForInvdict> tmpInvdictEntriesForOneURL = null;  // Partial index for one URL


        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        int nr_line = 0;

        while (line != null && line.length() > 0) {
            nr_line ++;
            if (nr_line % 20000000 == 0) {
                System.err.println("loaded " + nr_line);
            }


            StringTokenizer st = new StringTokenizer(line, "\t");

            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            String rawURL = st.nextToken();
            String url = Utils.pruneURL(rawURL);

            if (url.length() == 0) {
                line = in.readLine();
                continue;
            }

            /*
            if (((int)url.charAt(0)) % nrShards != i) {
                line = in.readLine();
                continue;
            }
             */

            // TODO : delete this if you want to do a merging on inv.dict from the beginning
            // It doesn't make sense to have a candidate entity for which allEntsFreq is 0, because this will mean p(e \in E) = 0
            if (!st.hasMoreTokens() || !allEntitiesFromAllPages.contains(url) || !entsDocFreqsInCorpus.containsKey(url)) {
                line = in.readLine();
                continue;
            }
            /**/

            // When URL changes.
            if (currentRawURL.compareTo(rawURL) != 0) {
                if (currentURL.length() > 0 && tmpInvdictEntriesForOneURL != null && denominators != null) {
                    if (!tmpInvdict.containsKey(currentURL)) {
                        tmpInvdict.put(currentURL, new TreeMap<String, CrosswikisProbabilityForInvdict>());
                    }

                    HashMap<String, Integer> currentDenominators = CrosswikisProbability.initMapOfWs();;
                    if (tmpInvdict.get(currentURL).size() > 0) {
                        currentDenominators = tmpInvdict.get(currentURL).firstEntry().getValue().getDenominator();
                    }

                    for (String name : tmpInvdictEntriesForOneURL.keySet()) {
                        CrosswikisProbabilityForInvdict cp = tmpInvdictEntriesForOneURL.get(name);
                        cp.setDenominator(denominators);

                        if (!tmpInvdict.get(currentURL).containsKey(name)) {
                            tmpInvdict.get(currentURL).put(name, new CrosswikisProbabilityForInvdict());
                            tmpInvdict.get(currentURL).get(name).addDenominator(currentDenominators);
                        }
                        tmpInvdict.get(currentURL).get(name).addNumerator(cp);
                    }

                    for (String name : tmpInvdict.get(currentURL).keySet()) {
                        tmpInvdict.get(currentURL).get(name).addDenominator(denominators);
                    }
                }

                denominators = CrosswikisProbability.initMapOfWs();
                tmpInvdictEntriesForOneURL =  new TreeMap<String, CrosswikisProbabilityForInvdict>();
                currentURL = url;
                currentRawURL = rawURL;
            }

            String left = st.nextToken();
            double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));  

            // TODO : delete this if you want to do a merging on inv.dict from the beginning

            if (cprob < 0.00005) {
                line = in.readLine();
                continue;             
            }
            /**/

            // TODO: uncomment this if you want to do a merging on inv.dict from the beginning
            /*
            if (cprob == 0) {
                line = in.readLine();
                continue;             
            }
             */

            String mention = left.substring(left.indexOf(" ") + 1);         

            StringTokenizer stScores = new StringTokenizer(st.nextToken(), " ");

            if (!tmpInvdictEntriesForOneURL.containsKey(mention)) {
                tmpInvdictEntriesForOneURL.put(mention, new CrosswikisProbabilityForInvdict());
            }
            while (stScores.hasMoreTokens()) {
                String token = stScores.nextToken();
                tmpInvdictEntriesForOneURL.get(mention).addNumeratorsAndUpdateDenominators(token, denominators);
            }

            line = in.readLine();
        }
        in.close();     

        if (currentURL.length() > 0 && tmpInvdictEntriesForOneURL != null && denominators != null) {
            if (!tmpInvdict.containsKey(currentURL)) {
                tmpInvdict.put(currentURL, new TreeMap<String, CrosswikisProbabilityForInvdict>());
            }

            HashMap<String, Integer> currentDenominators = CrosswikisProbability.initMapOfWs();;
            if (tmpInvdict.get(currentURL).size() > 0) {
                currentDenominators = tmpInvdict.get(currentURL).firstEntry().getValue().getDenominator();
            }

            for (String name : tmpInvdictEntriesForOneURL.keySet()) {
                CrosswikisProbabilityForInvdict cp = tmpInvdictEntriesForOneURL.get(name);
                cp.setDenominator(denominators);

                if (!tmpInvdict.get(currentURL).containsKey(name)) {
                    tmpInvdict.get(currentURL).put(name, new CrosswikisProbabilityForInvdict());
                    tmpInvdict.get(currentURL).get(name).addDenominator(currentDenominators);
                }
                tmpInvdict.get(currentURL).get(name).addNumerator(cp);
            }

            for (String name : tmpInvdict.get(currentURL).keySet()) {
                tmpInvdict.get(currentURL).get(name).addDenominator(denominators);
            }
        }


        // invdict: P(n|e)
        // invdict[url] = treemap<name, cprob>
        HashMap<String, TreeMap<String, Double>> invdict = 
            new HashMap<String, TreeMap<String, Double>>();

        // TODO : delete this if you want to do a merging on inv.dict from the beginning
        for (String url : tmpInvdict.keySet()) {
            invdict.put(url, new TreeMap<String, Double>());
            for (String name : tmpInvdict.get(url).keySet()) {
                invdict.get(url).put(name, tmpInvdict.get(url).get(name).getScore());
            }
        }
        /**/

        // TODO: uncomment this if you want to do a merging on inv.dict from the beginning
        // Printing output index.
        /*
        DecimalFormat df = new DecimalFormat("#.######");
        for (String url : tmpInvdict.keySet()) {
            Vector<InvdictEntry> v = new Vector<InvdictEntry>();
            for (String name : tmpInvdict.get(url).keySet()) {
                v.add(new InvdictEntry(name, tmpInvdict.get(url).get(name).getScore(), tmpInvdict.get(url).get(name)));
            }            
            Collections.sort(v);
            for (InvdictEntry ie : v) {
                System.out.println(url + "\t" + df.format(ie.score) + " " + ie.name + "\t" + ie.cpi);
            }
        }
        /**/

        System.err.println("[INFO] Done loading index. Size = " + invdict.size());

        return invdict;
    }       

}
