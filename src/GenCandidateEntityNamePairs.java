import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

class Candidate {
    String entityURL;
    String freebaseID;
    String name;
    int textIndex;
    double prob_n_cond_e; // probability P(n|e)

    // numerator(e,n,E) = p(e|n)/(p(e \in E)
    // posteriorProb(candidate,E) = score(e,n,E) = numerator(n,e,E) / (denominator(n,e,E) + p(dummy|n)/(1 - p(dummy|n))))
    // Compute also debug values for candidate: 
    //    - dummyPosteriorProb = score(dummy,n,E) = p(dummy|n) / ((1-p(dummy|n)) * (numerator(n,e,E)+denominator(n,e,E)))
    double posteriorProb;
    Debug debug;

    class Debug {
        // Debug values
        double dummyPosteriorProb; // the posterior prob for the dummy entity
        double prob_e_cond_n;
        int prob_e_in_E_times_nr_docs;
        double denominator;
        double dummy_contribution;
    }

    public Candidate(String entityURL, String freebaseID, String name, int textIndex, double invdictProb) {
        this.entityURL = entityURL;
        this.freebaseID= freebaseID ;
        this.name = name;
        this.textIndex= textIndex ;
        this.debug = new Debug();
        this.prob_n_cond_e = invdictProb;
        this.debug.dummyPosteriorProb = -1;
    }
}

public class GenCandidateEntityNamePairs {
    /////////////////////////////// Fields ////////////////////////////////////////////////////////////

    // invdict: P(n|e)
    // invdict[url] = treemap<name, cprob>
    private static HashMap<String, TreeMap<String, Double>> invdict = null;

    // invdict: P(n|e)
    // dict [name] = treemap<url, cprob>
    private static HashMap<String, TreeMap<String, Double>> dict = null;

    // Insert a dummy entity to represent all names that do not refer to a real Wikipedia entity.
    // Compute the p(M.ent = dummy | M.name = n) from doc frequencies (see the generation file).
    // This will augment the actual P(e|n) probabilities that we have from Crosswiki corpus which
    // actually represent p(M.ent = e | M.name = n, M.ent != dummy) instead of p(M.ent = e | M.name = n)
    private static HashMap<String, DummyIntPair> dummyProbabilities = null;

    // all Wikipedia entities (excluding redirects)
    // allEntsFreqs[name] = doc_frequency
    private static HashMap<String, Integer> allEntsFreqs = null;

    // Total number of docs from the Wikilinks corpus.
    private static int totalNumDocs = 0;

    ///////////////////////////////////// Classes ///////////////////////////////////////////////
    // Class used to store the counters for dummy probabilities P(dummy | name)
    private static class DummyIntPair {
        int numDocsWithAnchorName = 0;
        int numDocsWithName = 0;
        public DummyIntPair(int x, int y) {
            numDocsWithAnchorName = x;		
            numDocsWithName = y;
        }
    }

    //////////////// Methods ////////////////////////////////////////////////////////////////////////////

    private static void LoadInvdict(String filename) throws IOException{
        // <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]

        invdict =  new HashMap<String, TreeMap<String, Double>>();
        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        int nr_line = 0;
        while (line != null && line.length() > 3) {
            nr_line ++;
            StringTokenizer st = new StringTokenizer(line, "\t");

            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }
            String url = st.nextToken();
            url = Utils.pruneURL(url);

            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            String left = st.nextToken();
            double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));				
            String mention = left.substring(left.indexOf(" ") + 1);			

            if (!invdict.containsKey(url)) {
                invdict.put(url, new TreeMap<String, Double>());
            }
            invdict.get(url).put(mention, cprob);

            line = in.readLine();
        }
        in.close();		
    }	

    private static void LoadDict(String filename, HashSet<String> allCandidateNames) throws IOException{
        // <string><tab><cprob><space><url>
        dict =  new HashMap<String, TreeMap<String, Double>>();
        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        int nr_line = 0;
        while (line != null && line.length() > 3) {
            if (nr_line % 10000000 == 0) {
                System.out.println("loaded " + nr_line);
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

            String left = st.nextToken();
            double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));				
            String url = new StringTokenizer(left.substring(left.indexOf(" ") + 1), " ").nextToken();			
            url = Utils.pruneURL(url);

            if (!dict.containsKey(name)) {
                dict.put(name, new TreeMap<String, Double>());
            }
            dict.get(name).put(url, cprob);

            line = in.readLine();
        }
        in.close();		
    }	

    private static void LoadAllEntities(String filename) throws IOException {
        allEntsFreqs = new HashMap<String, Integer>();
        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        int nr_line = 0;
        while (!line.startsWith("NR DOCS:")) {
            StringTokenizer st = new StringTokenizer(line, "\t");
            String url = st.nextToken();
            if (!st.hasMoreTokens()) {
                System.err.println("[ERROR] wrong line format in all entities file :" + line + "::::::");
                line = in.readLine();
                continue;
            }
            int freq = Integer.parseInt(st.nextToken());
            allEntsFreqs.put(url, freq);
            line = in.readLine();
        }
        totalNumDocs = Integer.parseInt(in.readLine());
    }

    private static void LoadDummyProbs(String dummyProbsFilename) throws IOException {
        dummyProbabilities = new HashMap<String, DummyIntPair>();
        BufferedReader in = new BufferedReader(new FileReader(dummyProbsFilename));
        String line = in.readLine();
        while (line != null && line.length() > 0) {
            StringTokenizer st = new StringTokenizer(line, "\t");
            String name = st.nextToken();
            int numDocsWithAnchorName = Integer.parseInt(st.nextToken());
            int numDocsWithName = Integer.parseInt(st.nextToken());
            if (numDocsWithAnchorName > numDocsWithName) {
                numDocsWithAnchorName = numDocsWithName;
            }

            // Set a treshold of trust for these dummy probabilities.
            if (numDocsWithName >= 10) {
                dummyProbabilities.put(name, new DummyIntPair(numDocsWithAnchorName, numDocsWithName));
            }
            line = in.readLine();
        }		
    }


    // For a given Webpage, select all candidate pairs (n,e) such that P(n|e) >= theta
    private static Vector<Candidate> GenAllCandidates(WikiLinkItem i, double theta) {
        Vector<Candidate> currentPageCandidates = new Vector<Candidate>();
        HashSet<String> hs = new HashSet<String>();
        for (Mention m : i.mentions) {
            if (m.wiki_url.length() > 0) {
                hs.add(m.wiki_url);
            }
        }

        Iterator<String> it = hs.iterator();		
        while(it.hasNext()) {
            String url = it.next();

            if (!invdict.containsKey(url)) {
                continue;
            }

            Set<Entry<String,Double>> set = invdict.get(url).entrySet();
            for (Entry<String, Double> entry : set) {
                if (entry.getValue() >= theta) {
                    String name = entry.getKey();
                    if (name.length() == 0) {
                        continue;
                    }

                    int index = i.all_text.indexOf(name);

                    while (index != -1) {
                        // Keep just candidates that are separate words.
                        if (index > 0 && !Utils.isWordSeparator(i.all_text.charAt(index-1))) {
                            index = i.all_text.indexOf(name, index + 1);
                            continue;
                        }
                        if (index + name.length() < i.all_text.length() &&
                                !Utils.isWordSeparator(i.all_text.charAt(index + name.length()))) {
                            index = i.all_text.indexOf(name, index + 1);
                            continue;
                        }

                        currentPageCandidates.add(
                                new Candidate(url, null, name, index, entry.getValue()));

                       // System.out.println("# CANDIDATE # -- url="+url+" name="+name+"::: index=" + index);

                        index = i.all_text.indexOf(name, index + 1);							
                    }
                }
            }
        }
        return currentPageCandidates;
    }

    // Compute denominator(n,e,E) = \sum_{e' \neq e} p(e'|n) \frac{1}{p(e' \not\in E)}
    // This does not use the dummy entity.
    private static double ComputeDenominator(String name, String entity, HashSet<String> docEntities) {
        double denominator = 0.0;
        Set<Entry<String,Double>> set = dict.get(name).entrySet();
        for (Entry<String, Double> entry : set) {
            String url = entry.getKey();
            double cprob = entry.getValue();
            if (entity.compareTo(url) == 0) {
                continue;
            }

            // if e \in E
            if (docEntities.contains(url)) {
                denominator += cprob * totalNumDocs / allEntsFreqs.get(url);
            }
            // if e \notin E
            else if (allEntsFreqs.containsKey(url)) {
                denominator += cprob * totalNumDocs / (totalNumDocs - allEntsFreqs.get(url));
            } else {
                denominator += cprob;						
            }
        }	
        return denominator;
    }

    // numerator(e,n,E) = p(e|n)/(p(e \in E)
    // score(candidate,E) = score(e,n,E) =  numerator(n,e,E) / (denominator(n,e,E) + p(dummy|n)/(1 - p(dummy|n))))
    // Compute also debug values for candidate: 
    //    - dummyPosteriorProb = score(dummy,n,E) = p(dummy|n) / ((1-p(dummy|n)) * (numerator(n,e,E)+denominator(n,e,E)))
    private static void ComputeSimpleScoreForOneCandidate(Candidate cand, HashSet<String> docEntities) {		
        double numerator = dict.get(cand.name).get(cand.entityURL);
        numerator *= totalNumDocs / allEntsFreqs.get(cand.entityURL);

        // Compute denominator = \sum_{e' \neq e} p(e'|n) \frac{1}{p(e' \not\in E)}
        double denominator = ComputeDenominator(cand.name, cand.entityURL, docEntities);

        // Insert dummy probabilities: p(dummy | n) / (1 - p(dummy|n))
        double dummyContributionProb = 0.0;
        if (dummyProbabilities.containsKey(cand.name)) {
            DummyIntPair dp = dummyProbabilities.get(cand.name);
            if (dp.numDocsWithAnchorName == 0) {
                dummyContributionProb = Double.POSITIVE_INFINITY;
            } else {
                dummyContributionProb = 
                    (dp.numDocsWithName - dp.numDocsWithAnchorName + 0.0) / dp.numDocsWithAnchorName;
            }
        }

        if (denominator + dummyContributionProb == 0) {
            cand.posteriorProb = Double.POSITIVE_INFINITY;
        } else cand.posteriorProb = numerator / (denominator + dummyContributionProb);

        // DEBUG VALUES:
        cand.debug.dummy_contribution = dummyContributionProb;
        cand.debug.dummyPosteriorProb = dummyContributionProb / (denominator + numerator);

        cand.debug.prob_e_cond_n = dict.get(cand.name).get(cand.entityURL);
        cand.debug.prob_e_in_E_times_nr_docs = allEntsFreqs.get(cand.entityURL);
        cand.debug.denominator = denominator;
    }

    // 2 steps: 
    // ** for each candidate - compute posterior probability and keep the highest scored one for each (offset,name)
    // ** for each winner candidate - print it and print debug info
    private static void GenWinningEntities(
            WikiLinkItem i,
            Vector<Candidate> candidates,
            HashSet<String> docEntities) {
        
        HashSet<String> serializedMentions = new HashSet<String>();
        for (Mention m : i.mentions) {
            if (m.text_offset >= 0) {
                serializedMentions.add(m.wiki_url + "\t" + m.anchor_text.trim() + "\t" + m.text_offset);
            }
        }
        
        // Winning candidates grouped by their starting index in all_text
        // Key of the hash map: all_text offset + "\t" + name
        HashMap<String, Candidate> winners = new HashMap<String,Candidate>();

        // For each candidate, compute the score as described in formula (8).
        for (Candidate cand : candidates) {				
            if (!dict.containsKey(cand.name)) {
                continue;
            }
            // if P(cand.wiki_url | cand.name) = 0 , we ignore this candidate.
            if (!dict.get(cand.name).containsKey(cand.entityURL)) {
                continue;
            }

            ComputeSimpleScoreForOneCandidate(cand, docEntities);

            String key = cand.textIndex + "\t" + cand.name;
            if ((!winners.containsKey(key) || winners.get(key).posteriorProb < cand.posteriorProb)) {	
                //if (dummyPosteriorProb < cand.posteriorProb)
                winners.put(key, cand);
            }
        }

        // **************** WRITING WINNER CANDIDATES ******************************
        for (Candidate c : winners.values()) {
            System.out.println();
            System.out.println("entity=" + c.entityURL + "; name=" + c.name + "; page offset=" +
                    c.textIndex);
            System.out.println("l(dummy,n)=" + c.debug.dummyPosteriorProb + "; l(e,n)=" + c.posteriorProb);
            System.out.print("; P(n|e)=" + c.prob_n_cond_e );
            System.out.print("; p(e|n)=" + c.debug.prob_e_cond_n + "; p(e in E)=" + c.debug.prob_e_in_E_times_nr_docs);
            System.out.println("; denominator=" + c.debug.denominator + "; p(d|n)/(1-p(d|n)) = " + c.debug.dummy_contribution);

            int start = c.textIndex - 50;
            if (start < 0) {
                start = 0;
            }
            int end = c.textIndex + 50;
            if (end >= i.all_text.length()) {
                end = i.all_text.length() - 1;
            }

            System.out.println(";context=" + i.all_text.substring(start, end).replace('\n', ' '));

            if (c.debug.dummyPosteriorProb > 0 && c.debug.dummyPosteriorProb < c.posteriorProb) {
                System.out.println("## DUMMY WORSE THAN ME ### ");
            }

            if (c.debug.dummyPosteriorProb > 0 && c.debug.dummyPosteriorProb >= c.posteriorProb) {
                System.out.println("## DUMMY BETTER THAN ALL CANDIDATES ### ");
            } else {
                if (serializedMentions.contains(c.entityURL +"\t" + c.name.trim() + "\t" + c.textIndex)) {
                    System.out.println("## GOOD RESULT ##");
                }
            }
        }		
    }


    /*  Extended token span approach.
     *  Steps: 
     *** retain the set of (name, page_offset) for each page
     *** for each (n, offset): 
     ***   - compute the set (n',e') with p(n'|e') >= theta and n' \in t=n-,n,n+
     ***   - for each such e' compute l(n'\in t, e')
     ***   - retain the highest scored e'
     ***   - compute the name n' \in t with the highest p(n'|e')
     ***   - output winning pair (n',e') - print it and print debug info
     */
    private static void GenWinningEntitiesWithExtendedTokenSpan(
            WikiLinkItem i,
            Vector<Candidate> candidates,
            HashSet<String> docEntities) {

        int nrCorrectMatchingsFromWiklinksMentions = 0;
        HashSet<String> serializedMentions = new HashSet<String>();
        for (Mention m : i.mentions) {
            if (m.text_offset >= 0) {
                serializedMentions.add(m.wiki_url + "\t" + m.anchor_text.trim() + "\t" + m.text_offset);
            }
        }
        
        // Key of the hash map: name + "\t" + all_text offset
        HashSet<String> namesOfCandidates = new HashSet<String>();
        // Key of the hash map: name
        HashMap<String,Vector<Candidate>> indexOfCandidates = new HashMap<String,Vector<Candidate>>();
        
        for (Candidate cand : candidates) {
            String key = cand.name + "\t" + cand.textIndex;
            namesOfCandidates.add(key);
            
            if (!indexOfCandidates.containsKey(cand.name)) {
                indexOfCandidates.put(cand.name, new Vector<Candidate>());
            }
            indexOfCandidates.get(cand.name).add(cand);
        }

        // This is used just to know if a winner was printed or not.
        HashSet<String> winners = new HashSet<String>();
        
        // for each (n, offset):
        for (String key : namesOfCandidates) {
            String name = key.substring(0, key.lastIndexOf('\t')).trim();
            int offset = Integer.parseInt(key.substring(name.length() + 1));

            // Set of all entities e' from all pairs (n',e') with p(n'|e') >= theta and n' \in t=n-,n,n+
            HashSet<String> possibleEntities = new HashSet<String>();
            
            // for each n' \in t=n-,n,n+
            Vector<TokenSpan> surroundingTokenSpans = Utils.getTokenSpans(i.all_text, offset, name.length());
            for (TokenSpan ts : surroundingTokenSpans) {
                // extract n'
                String extendedName = i.all_text.substring(ts.start, ts.end);
                
                // for each (n',e') with p(n'|e') >= theta and n' \in t=n-,n,n+
                if (!indexOfCandidates.containsKey(extendedName)) {
                    continue;
                }
                for (Candidate cand : indexOfCandidates.get(extendedName)) {
                    // retain just e'
                    possibleEntities.add(cand.entityURL);
                }
            }
            
            String winnerURL = "";
            double winnerScore = 0.0; 
            // for each such e' compute l(n'\in t, e') and retain the highest scored e'
            for (String entity : possibleEntities) {
                double numerator = 0.0;
                double denominator = 0.0;
                for (TokenSpan ts : surroundingTokenSpans) {
                    // extract n'
                    String extendedName = i.all_text.substring(ts.start, ts.end);
                    if (!dict.containsKey(extendedName)) {
                        continue;
                    }
                    denominator += ComputeDenominator(extendedName, entity, docEntities);
                    
                    // if P(extendedName | entity) = 0 , we ignore this candidate.
                    if (!dict.get(extendedName).containsKey(entity)) {
                        continue;
                    }
                    numerator += dict.get(extendedName).get(entity) * totalNumDocs / allEntsFreqs.get(entity);
                }
                
                double score;
                if (numerator == 0) {
                    continue;
                }
                if (denominator == 0) {
                    score = Double.POSITIVE_INFINITY;
                }
                else {
                    score = numerator / denominator;
                }
                
                if (score > winnerScore) {
                    winnerScore = score;
                    winnerURL = entity;
                }
            }
            
            String winnerName = "";
            double winnerNameCondProbability = 0.0;
            int winnerOffset = -1;
            for (TokenSpan ts : surroundingTokenSpans) {
                // extract n'
                String extendedName = i.all_text.substring(ts.start, ts.end);
                // for each (n',e') with p(n'|e') >= theta and n' \in t=n-,n,n+
                if (!indexOfCandidates.containsKey(extendedName)) {
                    continue;
                }
                for (Candidate cand : indexOfCandidates.get(extendedName)) {
                    if (cand.entityURL == winnerURL && cand.prob_n_cond_e > winnerNameCondProbability) {
                        winnerName = extendedName;
                        winnerNameCondProbability = cand.prob_n_cond_e;
                        winnerOffset = ts.start;
                    }
                }
            }

            if (winnerOffset < 0) {
                continue;
            }

            String winnerKey = winnerURL + "\t" + winnerName.trim() + "\t" + winnerOffset;
            if (winners.contains(winnerKey)) {
                continue;
            }
            winners.add(winnerKey);
            
            // Print winner and some debug values:
            System.out.println();
            System.out.println("entity=" + winnerURL + "; candidate_name=" + name + "; matched_name=" + winnerName + "; page offset=" + winnerOffset);
            System.out.println("; P(n|e)=" + winnerNameCondProbability + " ; winner entity score=" + winnerScore);

            int start = winnerOffset - 50;
            if (start < 0) {
                start = 0;
            }
            int end = winnerOffset + 50;
            if (end >= i.all_text.length()) {
                end = i.all_text.length() - 1;
            }

            System.out.println(";context=" + i.all_text.substring(start, end).replace('\n', ' '));
            
            if (serializedMentions.contains(winnerKey)) {
                System.out.println("## GOOD RESULT ##");
                nrCorrectMatchingsFromWiklinksMentions++;
            }
        }
        
        if (nrCorrectMatchingsFromWiklinksMentions < i.mentions.size()) {
            System.out.println("## PAGE WITH LESS MATCHINGS ##");
        }
    }

    public static void run(
            String prunnedInvdictFilename, 
            String dictFilename, 
            String allEntitiesFilename, 
            String dummyProbsFilename,
            Double theta, 
            String webpagesFilename,
            boolean extendedTokenSpan) throws IOException, InterruptedException {

        System.out.println("loading inv index P(n|e) ...");
        LoadInvdict(prunnedInvdictFilename);
        System.out.println("Done loading index. Size = " + invdict.size());

        // ***** STAGE 1: Generate all candidate pairs (n,e) such that P(n|e) >= theta
        HashSet<String> allCandidateNames = new HashSet<String>();
        Vector<Vector<Candidate>> allCandidates = new Vector<Vector<Candidate>>();

        System.out.println("Generating all candidates and their names ...");
        WikilinksParser p = new WikilinksParser(webpagesFilename);
        int nr_page = -1;
        while (p.hasMoreItems()) {
            nr_page++;
            if (nr_page % 1000 == 0) {
                System.out.println(nr_page);
            }
            WikiLinkItem i = p.nextItem();

            Vector<Candidate> thisPageCandidates = GenAllCandidates(i, theta);
            allCandidates.add(thisPageCandidates);
            for (Candidate cand : thisPageCandidates) {
                allCandidateNames.add(cand.name);
            }
        }
        System.out.println("Done. Num all candidate names = " + allCandidateNames.size());

        // Free memory:
        invdict = null;

        System.out.println("loading dict P(e|n) index...");
        LoadDict(dictFilename, allCandidateNames);
        System.out.println("Done. Size = " + dict.size());

        System.out.println("loading all entities...");
        LoadAllEntities(allEntitiesFilename);
        System.out.println("All ents size : " + allEntsFreqs.size() + " ; total num docs = " + totalNumDocs);

        System.out.println("loading dummy probs P(M.ent != dummy| M.ent = n) index...");
        LoadDummyProbs(dummyProbsFilename);
        System.out.println("Done. Size = " + dummyProbabilities.size());

        // ***** STAGE 2: Compute the l(n,e) values, group by n and find the winning candidate.
        System.out.println("Winner entities:");
        p = new WikilinksParser(webpagesFilename);
        nr_page = -1;
        while (p.hasMoreItems()) {
            nr_page++;
            System.out.println("------- Page: " + nr_page);

            WikiLinkItem i = p.nextItem();

            // compute M.doc.E
            HashSet<String> docEntities = new HashSet<String>();
            for (Mention m : i.mentions) {
                if (m.wiki_url.length() > 0) {
                    docEntities.add(m.wiki_url);
                }
            }			

            for (Mention m : i.mentions) {
                System.out.println("## MENTION ## " + m);
            }
            if (extendedTokenSpan) {
                GenWinningEntitiesWithExtendedTokenSpan(i, allCandidates.get(nr_page), docEntities);
            } else {
                GenWinningEntities(i, allCandidates.get(nr_page), docEntities);
            }
        }
    }

}
