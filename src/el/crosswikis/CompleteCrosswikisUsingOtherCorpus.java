package el.crosswikis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import el.input_data_pipeline.TruthMention;
import el.input_data_pipeline.wikilinks.WikilinksShardParser;
import el.input_data_pipeline.wikilinks.WikilinksSinglePage;
import el.utils.Utils;
import el.wikipedia_redirects.WikiRedirects;

// Class used to compute the probabilities p(e|n) and p(n|e) for entities that are 
// not in the Crosswikis data. These will be approximated based on the corpus.
// For each entity in AllEntsFreqs file and not in Inv.dict, compute: #anc(e) , #anc(e,n), p(n|e) 
public class CompleteCrosswikisUsingOtherCorpus {
    
    static class NameEntry implements Comparable<NameEntry> {
        public String name;
        public int freq;  // #anc(e,n)
        
        public NameEntry(String name, int freq) {
            this.name = name;
            this.freq = freq;
        }
        
        @Override
        public int compareTo(NameEntry o) {
            if (this.freq != o.freq) {
                return - this.freq + o.freq;
            }
            return this.name.compareTo(o.name);
        }
    }
    
    static class EntityStatsFromWikilinks {
        String url; // entity
        int numAnchors; // number of anchors of this entity: #anc(e)
        HashMap<String, Integer> nameFreqs; // number of anchors of this entity having a specific name: #anc(e,n)
        
        public EntityStatsFromWikilinks(String url) {
            this.url = url;
            numAnchors = 0;
            nameFreqs = new HashMap<String, Integer>();
        }
    }
    
    private static HashSet<String> LoadAllEntitiesFromWikilinks(String allEntsFilename) throws IOException {
        System.err.println("[INFO] Loading all entities from Wikilinks ...");

        int totalNumDocs = 0;
        HashSet<String> allEntsFromWikilinks = new HashSet<String>();
        BufferedReader in = new BufferedReader(new FileReader(allEntsFilename));
        String line = in.readLine();
        while (!line.startsWith("NR DOCS:")) {
            StringTokenizer st = new StringTokenizer(line, "\t");
            String url = st.nextToken();
            if (!st.hasMoreTokens()) {
                System.err.println("[ERROR] wrong line format in all entities file :" + line + "::::::");
                line = in.readLine();
                continue;
            }
            allEntsFromWikilinks.add(url);
            line = in.readLine();
        }
        totalNumDocs = Integer.parseInt(in.readLine());

        System.err.println("[INFO] All ents size : " + allEntsFromWikilinks.size() + " ; total num docs = " + totalNumDocs);
        return allEntsFromWikilinks;
    }
    
    private static HashSet<String> FindAllEntitiesFromWikilinksButNotInvDict(String invDictFilename, String allEntsFilename) throws IOException{
        System.err.println("[INFO] Loading all ents from inv.dict P(n|e) and Wikilinks ...");
        
        HashSet<String> allEntsFromWikilinksButNotInvDict = LoadAllEntitiesFromWikilinks(allEntsFilename);
        
        BufferedReader in = new BufferedReader(new FileReader(invDictFilename));
        String line = in.readLine();
        int nr_line = 0;
        while (line != null && line.length() > 3) {
            nr_line ++;
            if (nr_line % 20000000 == 0) {
                System.err.println("loaded " + nr_line);
            }
            StringTokenizer st = new StringTokenizer(line, "\t");

            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }
            String url = WikiRedirects.pruneURL(st.nextToken());

            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            allEntsFromWikilinksButNotInvDict.remove(url);

            line = in.readLine();
        }
        in.close();             
        return allEntsFromWikilinksButNotInvDict;
    }   

    // Computes p(n|e) from a single Wikilinks file
    public static void fromFile(
            String filename,
            HashSet<String> allEntsToCorrect,
            TreeMap<String, EntityStatsFromWikilinks> entStats,
            int nr_file) throws IOException {
        
        System.err.println("Processing file " + filename + " ; number = " + nr_file);
        WikilinksShardParser p = new WikilinksShardParser(filename);

        int doc_index = 0;
        while (p.hasNext()) {
            doc_index++;
            WikilinksSinglePage doc = p.next();
            for (TruthMention mention : doc.truthMentions) {
                if (!allEntsToCorrect.contains(mention.wikiUrl)) {
                    continue;
                }
                if (!entStats.containsKey(mention.wikiUrl)) {
                    entStats.put(mention.wikiUrl, new EntityStatsFromWikilinks(mention.wikiUrl));
                }
                EntityStatsFromWikilinks stats = entStats.get(mention.wikiUrl);
                stats.numAnchors++;
                
                if (!stats.nameFreqs.containsKey(mention.anchorText)) {
                    stats.nameFreqs.put(mention.anchorText, 0);
                }
                stats.nameFreqs.put(mention.anchorText, stats.nameFreqs.get(mention.anchorText) + 1);
                entStats.put(mention.wikiUrl, stats);
            }
        }
    }   
    

    public static void fromDir(String dir_file, String invDictFilename, String allEntsFilename, String out_file) throws IOException {        
        System.err.println("[INFO] Running Invdict completion based on Wikilinks ... ");
        if (!dir_file.endsWith("/")) {
            dir_file += "/";
        }

        HashSet<String> allEntsToCorrect = FindAllEntitiesFromWikilinksButNotInvDict(invDictFilename, allEntsFilename);
        System.err.println("[INFO] Number ents to be corrected = " + allEntsToCorrect.size());


        TreeMap<String, EntityStatsFromWikilinks> entStats = new TreeMap<String, EntityStatsFromWikilinks>();

        File dir = new File(dir_file);
        if(dir.isDirectory()==false) {
            System.out.println("Directory does not exists : " + dir_file);
            return;
        }
        String[] list = dir.list();
        int nr_file = 0;
        for (String filename : list) {
            if (!filename.endsWith(".data")) {
                continue;       
            }
            nr_file++;
            fromFile(dir_file + filename, allEntsToCorrect, entStats, nr_file);
        }
        
        PrintWriter writer = new PrintWriter(out_file, "UTF-8");
        for (String url : entStats.keySet()) {
            if (entStats.get(url).numAnchors < 5) { // Keep just entities for which at least 5 anchors exist
                continue;
            }
            
            Vector<NameEntry> v = new Vector<NameEntry>();
            for (Entry<String,Integer> e: entStats.get(url).nameFreqs.entrySet()) {
                v.add(new NameEntry(e.getKey(), e.getValue()));
            }
            Collections.sort(v);
            
            for (NameEntry ne : v) {
                double cprob = ((double)ne.freq) / entStats.get(url).numAnchors;
                DecimalFormat df = new DecimalFormat("#.######");

                // <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]
                writer.println(url + "\t" + df.format(cprob) + " " + ne.name + "\tw:" + ne.freq + "/" + entStats.get(url).numAnchors);                
            }
            
        }
        writer.flush();
        writer.close();     
    }
}
