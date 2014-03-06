package entity_linking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

import entity_linking.input_data_pipeline.*;

public class GenCandidateEntityNamePairs {
    /////////////////////////////// Fields ////////////////////////////////////////////////////////////

    // invdict: P(n|e)
    // invdict[url] = treemap<name, cprob>
    private static HashMap<String, TreeMap<String, Double>> invdict = null;

    // dict: P(e|n)
    // dict [name] = treemap<url, cprob>
    private static HashMap<String, TreeMap<String, Double>> dict = null;

    // Insert a dummy entity to represent all names that do not refer to a real Wikipedia entity.
    // Compute the p(M.ent = dummy | M.name = n) from doc frequencies (see the generation file).
    // This will augment the actual P(e|n) probabilities that we have from Crosswiki corpus which
    // actually represent p(M.ent = e | M.name = n, M.ent != dummy) instead of p(M.ent = e | M.name = n)
    private static HashMap<String, DummyIntPair> dummyProbabilities = null;

    // all Wikipedia entities (excluding redirects)
    // entsDocFreqsInCorpus[url] = doc_frequency
    private static HashMap<String, Integer> entsDocFreqsInCorpus = null;

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
    private static void LoadAllEntities(String filename) throws IOException {
        System.err.println("[INFO] Loading all entities...");

        entsDocFreqsInCorpus = new HashMap<String, Integer>();
        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        while (!line.startsWith("NR DOCS:")) {
            StringTokenizer st = new StringTokenizer(line, "\t");
            String url = st.nextToken();
            if (!st.hasMoreTokens()) {
                System.err.println("[ERROR] wrong line format in all entities file :" + line + "::::::");
                line = in.readLine();
                continue;
            }
            int freq = Integer.parseInt(st.nextToken());
            entsDocFreqsInCorpus.put(url, freq);
            line = in.readLine();
        }
        totalNumDocs = Integer.parseInt(in.readLine());

        System.err.println("[INFO] All ents size : " + entsDocFreqsInCorpus.size() + " ; total num docs = " + totalNumDocs);
    }
    
    private static HashSet<String> GetAllEntitiesFromAllPages(GenericPagesIterator inputPagesIterator) {
        GenericPagesIterator pagesIterator = inputPagesIterator.hardCopy();

        HashSet<String> allEntitiesFromAllPages = new HashSet<String>();
       
        while (pagesIterator.hasNext()) {
            GenericSinglePage i = pagesIterator.next();
            for (TruthMention m : i.truthMentions) {
                allEntitiesFromAllPages.add(m.wikiUrl);
            }
        }
        
        System.err.println("[INFO] Number of all entities from all input docs = " + allEntitiesFromAllPages.size());
        
        int nrEntsNotInentsDocFreqsInCorpus = 0;
        for (String url : allEntitiesFromAllPages) {
            if (!entsDocFreqsInCorpus.containsKey(url)) {
                nrEntsNotInentsDocFreqsInCorpus++;
            }
        }
        System.err.println("[INFO] Number of all entities from all input docs that are not in the file with Entities Frequencies = " + nrEntsNotInentsDocFreqsInCorpus);

        return allEntitiesFromAllPages;
    }
 
    // invdict: P(n|e)
    private static void LoadAndPruneInvdict(String filename, HashSet<String> allEntitiesFromAllPages) throws IOException{
        // <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]

        System.err.println("[INFO] Loading and prunning inv.dict P(n|e) ...");
        
        invdict =  new HashMap<String, TreeMap<String, Double>>();
        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        int nr_line = 0;
        String lastUrl = "";
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
            String unprocessedUrl = st.nextToken();
            String url = Utils.pruneURL(unprocessedUrl);
            
            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            String left = st.nextToken();
            double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));				
            String mention = left.substring(left.indexOf(" ") + 1);			

            // It doesn't make sense to have a candidate entity for which allEntsFreq is 0, because this will mean p(e \in E) = 0
            if (!allEntitiesFromAllPages.contains(url) || !entsDocFreqsInCorpus.containsKey(url) || cprob < 0.0001) {
                line = in.readLine();
                continue;
            }

            if (url.compareTo(lastUrl) != 0) {
                if (invdict.containsKey(url)) {
                    System.err.println(unprocessedUrl + " :::: " + url);                    
                } else {
                    System.err.println(unprocessedUrl + " ::----->:: " + url);
                }
            }
            lastUrl = url;
            
            if (!invdict.containsKey(url)) {
                invdict.put(url, new TreeMap<String, Double>());
            }
            invdict.get(url).put(mention, cprob);

            line = in.readLine();
        }
        in.close();		

        System.err.println("[INFO] Done loading index. Size = " + invdict.size());
    }		

    private static void LoadDummyProbs(String dummyProbsFilename) throws IOException {
        System.err.println("[INFO] Loading dummy probs P(M.ent != dummy| M.ent = n) index...");

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

        System.err.println("[INFO] Done. Size = " + dummyProbabilities.size());
    }

    // For a given Webpage, select all candidate pairs (n,e) such that P(n|e) >= theta
    // TODO: delete HashMap<String,HashMap<String, Boolean>> debugMentionsInfos
    private static int GenAllCandidatesFromOnePage(
            GenericSinglePage doc, 
            double theta, 
            Vector<Candidate> outputCandidates, 
            HashMap<String,HashMap<String, Boolean>> debugMentionsInfos) {
                
        int nrTruthCorrectCandidates = 0;
        
        HashSet<String> setOfUrls = new HashSet<String>();
        for (TruthMention m : doc.truthMentions) {
            if (m.wikiUrl.length() > 0) {
                setOfUrls.add(m.wikiUrl);
            }
        }

        Iterator<String> it = setOfUrls.iterator();		
        while(it.hasNext()) {
            String url = it.next();

            if (!invdict.containsKey(url)) {
                continue;
            }

            Set<Entry<String,Double>> set = invdict.get(url).entrySet();
            for (Entry<String, Double> entry : set) {
                if (entry.getValue() >= theta) {
                    String name = entry.getKey();
                    
                    // Candidate names that end in a word separator are bad, because they will be matched almost for sure.
                    // Example: "gluatmine," appears in inv.dict for Glutamine
                    // TODO: something must be done here.
                    if (name.length() == 0) { // || Utils.isWordSeparator(name.charAt(0)) || Utils.isWordSeparator(name.charAt(name.length() - 1))) {
                        continue;
                    }

                    int index = doc.getRawText().indexOf(name);

                    while (index != -1) {
                        // Keep just candidates that are separate words.
                        if (index > 0 && !Utils.isWordSeparator(doc.getRawText().charAt(index-1))) {
                            index = doc.getRawText().indexOf(name, index + 1);
                            continue;
                        }
                        if (index + name.length() < doc.getRawText().length() &&
                                !Utils.isWordSeparator(doc.getRawText().charAt(index + name.length()))) {
                            index = doc.getRawText().indexOf(name, index + 1);
                            continue;
                        }
                        outputCandidates.add(
                                new Candidate(url, null, name, index, entry.getValue()));
                        index = doc.getRawText().indexOf(name, index + 1);							
                    }
                }
            }
        }
        
        // TODO FOR DEBUG ONLY:
        for (TruthMention m : doc.truthMentions) {
                
            String key = "URL=" + m.wikiUrl + ";name=" + m.anchorText + ";offset=" + m.mentionOffsetInText + ";doc=" + doc.pageName;
            debugMentionsInfos.put(key, new HashMap<String, Boolean>());
            debugMentionsInfos.get(key).put("isCandidate", true);
            debugMentionsInfos.get(key).put("EntIsInInvdict", true);
            debugMentionsInfos.get(key).put("NameIsInInvdictForEnt", true);
            debugMentionsInfos.get(key).put("EntIsInAllEnts", true);
            
            if (!invdict.containsKey(m.wikiUrl)) {
                debugMentionsInfos.get(key).put("EntIsInInvdict", false);
            }
            if (!entsDocFreqsInCorpus.containsKey(m.wikiUrl))
                debugMentionsInfos.get(key).put("EntIsInAllEnts", false);
            if (invdict.containsKey(m.wikiUrl) && !invdict.get(m.wikiUrl).containsKey(m.anchorText))
                debugMentionsInfos.get(key).put("NameIsInInvdictForEnt", false);
            
            
            boolean isNotACandidate = true;
            for (Candidate c : outputCandidates) {
                if (c.entityURL.compareTo(m.wikiUrl) == 0 && c.name.compareTo(m.anchorText) == 0 && c.textIndex == m.mentionOffsetInText) {
                    isNotACandidate = false;
                    break;
                }
            }

            if (isNotACandidate) {
                debugMentionsInfos.get(key).put("isCandidate", false);
            } else {
                nrTruthCorrectCandidates++;
            }
        }        
        
        return nrTruthCorrectCandidates;
    }
      
    
    // TODO : delete debugMEntionsInfos in the end
    private static void GenAllCandidatesFromAllPages(
            GenericPagesIterator pagesIterator,
            double theta,
            HashSet<String> allCandidateNames,
            Vector<Vector<Candidate>> allCandidates,
            HashMap<String,HashMap<String, Boolean>> debugMentionsInfos) {
        System.err.println("[INFO] Generating all candidates and their names ...");
        int nr_page = -1;
        int nrCandidates = 0;
        int nrMentions = 0;
        int nrTruthCandidates = 0;        
        
        HashSet<String> allMentionsNames = new HashSet<String>();
        while (pagesIterator.hasNext()) {
            nr_page++;
            if (nr_page % 10000 == 0) {
                System.err.println("[INFO] " + nr_page);
            }
            GenericSinglePage doc = pagesIterator.next();

            Vector<Candidate> thisPageCandidates = new Vector<Candidate>();
            
            nrTruthCandidates += GenAllCandidatesFromOnePage(doc, theta, thisPageCandidates, debugMentionsInfos);
            allCandidates.add(thisPageCandidates);
            for (Candidate cand : thisPageCandidates) {
                allCandidateNames.add(cand.name);
            }
            nrCandidates += thisPageCandidates.size();
            
            for (TruthMention m : doc.truthMentions) {
                allMentionsNames.add(m.anchorText);
            }
            nrMentions += doc.truthMentions.size();
        }
        
        System.err.println("[INFO] Done. Num all generated candidates = " + nrCandidates);
        System.err.println("[INFO] Num all candidates' names = " + allCandidateNames.size());
        System.err.println("[INFO] Num all mentions from all input docs = " + nrMentions);
        System.err.println("[INFO] Num all mentions' names = " + allMentionsNames.size());
        System.err.println("[INFO] Num truth candidates (candidates that are ground truth mentions) = " + nrTruthCandidates);
    }
        

    // Compute denominator(n,e,E) = \sum_{e' \neq e} p(e'|n) \frac{1}{p(e' \not\in E)}
    // This does not use the dummy entity.
    private static double ComputeDenominator(String name, String entity, HashSet<String> docEntities) {
        double denominator = 0.0;
        if (!dict.containsKey(name)) {
            return 0;
        }
        Set<Entry<String,Double>> set = dict.get(name).entrySet();
        for (Entry<String, Double> entry : set) {
            String url = entry.getKey();
            double cprob = entry.getValue();
            if (entity.compareTo(url) == 0) {
                continue;
            }

            // if e \in E
            if (docEntities.contains(url) && entsDocFreqsInCorpus.containsKey(url)) {
                denominator += cprob * totalNumDocs / entsDocFreqsInCorpus.get(url);
            }
            // if e \notin E
            else if (entsDocFreqsInCorpus.containsKey(url)) {
                denominator += cprob * totalNumDocs / (totalNumDocs - entsDocFreqsInCorpus.get(url));
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
    private static void ComputeSimpleScoreForOneCandidate(
            Candidate cand, 
            HashSet<String> docEntities,
            boolean withDummyEnt) {
        
        double numerator = dict.get(cand.name).get(cand.entityURL);
        numerator *= totalNumDocs / entsDocFreqsInCorpus.get(cand.entityURL);

        // Compute denominator = \sum_{e' \neq e} p(e'|n) \frac{1}{p(e' \not\in E)}
        double denominator = ComputeDenominator(cand.name, cand.entityURL, docEntities);

        // Insert dummy probabilities: p(dummy | n) / (1 - p(dummy|n))
        double dummyContributionProb = 0.0;
        
        if (withDummyEnt && dummyProbabilities.containsKey(cand.name)) {
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
        cand.debug.prob_e_in_E_times_nr_docs = entsDocFreqsInCorpus.get(cand.entityURL);
        cand.debug.denominator = denominator;
    }

    // 2 steps: 
    // ** for each candidate - compute posterior probability and keep the highest scored one for each (offset,name)
    // ** for each winner candidate - print it and print debug info
    private static void GenWinningEntities(
            GenericSinglePage doc,
            Vector<Candidate> candidates,
            HashSet<String> docEntities,
            boolean includeDummyEnt) {
        
        HashSet<String> serializedMentions = new HashSet<String>();
        for (TruthMention m : doc.truthMentions) {
            if (m.mentionOffsetInText >= 0) {
                serializedMentions.add(m.wikiUrl + "\t" + m.anchorText + "\t" + m.mentionOffsetInText);
            }
        }
        
        // Winning candidates grouped by their starting index in allAnnotatedText
        // Key of the hash map: allAnnotatedText offset + "\t" + name
        HashMap<String, Candidate> winners = new HashMap<String,Candidate>();

        // For each candidate, compute the score as described in formula (8).
        for (Candidate cand : candidates) {				
            if (!dict.containsKey(cand.name)) {
                continue;
            }
            // if P(cand.wikiUrl | cand.name) = 0 , we ignore this candidate.
            if (!dict.get(cand.name).containsKey(cand.entityURL)) {
                continue;
            }

            ComputeSimpleScoreForOneCandidate(cand, docEntities, includeDummyEnt);

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
            if (end >= doc.getRawText().length()) {
                end = doc.getRawText().length() - 1;
            }

            System.out.println(";context=" + doc.getRawText().substring(start, end).replace('\n', ' '));

            if (c.debug.dummyPosteriorProb > 0 && c.debug.dummyPosteriorProb < c.posteriorProb) {
                System.out.println("## DUMMY WORSE THAN ME ### ");
            }

            if (c.debug.dummyPosteriorProb > 0 && c.debug.dummyPosteriorProb >= c.posteriorProb) {
                System.out.println("## DUMMY BETTER THAN ALL CANDIDATES ### ");
            } else {
                String key = c.entityURL +"\t" + c.name + "\t" + c.textIndex;      
                if (serializedMentions.contains(key)) {
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
     ***   - for each such e' compute l(n' \in t, e')
     ***   - retain the highest scored e'
     ***   - compute the name n' \in t with the highest p(n'|e')
     ***   - output winning pair (n',e') - print it and print debug info
     *
     *  Returns a set of winning candidates.   
     */
    private static Vector<Candidate> GenWinningEntitiesWithExtendedTokenSpan(
            GenericSinglePage doc,
            Vector<Candidate> candidates,
            HashSet<String> docEntities,
            boolean includeDummyEnt,
            // TODO: delete this in the end
            HashMap<String,HashMap<String, Boolean>> debugMentionsInfos) {

        Vector<Candidate> winnerMatchings = new Vector<Candidate>();
        
        HashSet<String> serializedMentions = new HashSet<String>();
        for (TruthMention m : doc.truthMentions) {
            serializedMentions.add(m.wikiUrl + "\t" + m.anchorText + "\t" + m.mentionOffsetInText);
        }
        
        // Key of the hash map: name + "\t" + allAnnotatedText offset
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
        // The same (e,n,offset) might be discovered from multiple candidates in this token span approach.
        HashSet<String> winners = new HashSet<String>();
        
        // for each (n, offset):
        for (String key : namesOfCandidates) {            
            System.out.println("NOW ANALYZE CAND: " + key);
            
            String name = key.substring(0, key.lastIndexOf('\t'));
            int offset = Integer.parseInt(key.substring(name.length() + 1));

            // Set of all entities e' from all pairs (n',e') with p(n'|e') >= theta and n' \in t=n-,n,n+
            HashSet<String> possibleEntities = new HashSet<String>();
            
            // for each n' \in t=n-,n,n+
            Vector<TokenSpan> surroundingTokenSpans = Utils.getTokenSpans(doc.getRawText(), offset, name.length());
            for (TokenSpan ts : surroundingTokenSpans) {
                // extract n'
                String extendedName = doc.getRawText().substring(ts.start, ts.end);
                
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
                    String extendedName = doc.getRawText().substring(ts.start, ts.end);
                    
                    // If p(entity | extendedName) == 0 or does not exist
                    if (!dict.containsKey(extendedName)) {
                        continue;
                    }
                    
                    // p(dummy | name)
                    double dummyProb = 0;
                    if (includeDummyEnt && dummyProbabilities.containsKey(extendedName)) {
                        DummyIntPair dp = dummyProbabilities.get(extendedName);
                        dummyProb = 1 - (dp.numDocsWithAnchorName + 0.0) / dp.numDocsWithName;
                    }
                    
                    denominator += ComputeDenominator(extendedName, entity, docEntities) * (1 - dummyProb) + dummyProb;
                    
                    // if P(extendedName | entity) = 0 , we ignore this candidate.
                    if (!dict.get(extendedName).containsKey(entity)) {
                        continue;
                    }
                    
                    if (!entsDocFreqsInCorpus.containsKey(entity)) {
                        System.err.println("[FATAL] entity frequency is 0, but still it is a candidate !");
                        System.exit(1);
                    }
                    
                    numerator += dict.get(extendedName).get(entity) * totalNumDocs / entsDocFreqsInCorpus.get(entity) * (1-dummyProb);
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

                System.out.println("  Candidate entity=" + entity + "; l(e, n \\in t)=" + score + " ;numerator=" + numerator + " ;denominator = " + denominator);
                if (score > winnerScore) {
                    winnerScore = score;
                    winnerURL = entity;
                }
            }
            
            
            // Compute dummy entity score l(n \in t, dummy)
            double dummyScore = 0.0;            
            if (includeDummyEnt) {
                double dummyNumerator = 0.0;
                double dummyDenominator = 0.0;

                for (TokenSpan ts : surroundingTokenSpans) {
                    String extendedName = doc.getRawText().substring(ts.start, ts.end);
                    
                    double dummyProb = 0.0;
                    if (dummyProbabilities.containsKey(extendedName)) {
                        DummyIntPair dp = dummyProbabilities.get(extendedName);
                        dummyProb = 1 - (dp.numDocsWithAnchorName + 0.0) / dp.numDocsWithName;
                    }
                    dummyNumerator += dummyProb;
                    dummyDenominator += (1 - dummyProb) * ComputeDenominator(extendedName, "", docEntities);
                }
                                
                if (dummyNumerator > 0) {
                    if (dummyDenominator == 0) {
                        dummyScore = Double.POSITIVE_INFINITY;
                    }
                    else {
                        dummyScore = dummyNumerator / dummyDenominator;
                    }
                }
            } 

            if (includeDummyEnt && dummyScore > 0 && dummyScore > winnerScore) {
                System.out.println("  WINNER URL FOR CAND: ## DUMMY ##" );
                continue;
            }
            if (includeDummyEnt && dummyScore > 0 && dummyScore <= winnerScore) {
                System.out.println("## DUMMY WORSE THAN ME ### dummyscore=" + dummyScore);
            }
            System.out.println("  WINNER URL FOR CAND: " + winnerURL);

            
            String winnerName = "";
            double winnerNameCondProbability = 0.0;
            int winnerOffset = -1;
            for (TokenSpan ts : surroundingTokenSpans) {
                // extract n'
                String extendedName = doc.getRawText().substring(ts.start, ts.end);
                System.out.println("  TOKEN SPAN: " + extendedName);

                // for each (n',e') with p(n'|e') >= theta and n' \in t=n-,n,n+
                if (!indexOfCandidates.containsKey(extendedName)) {
                    continue;
                }
                
                // This might be improved to a O(1) by using a HashSet
                for (Candidate cand : indexOfCandidates.get(extendedName)) {
                    if (cand.entityURL.compareTo(winnerURL) == 0) {
                        System.out.println("  CAND APPEARING IN REALITY :::" + extendedName + " ;url=" + cand.entityURL + 
                                " ;p(n|e)=" + cand.prob_n_cond_e + "; p(e|n,∃e)=" + dict.get(extendedName).get(winnerURL));
                    }
                        
                    if (cand.entityURL.compareTo(winnerURL) == 0 && cand.prob_n_cond_e > winnerNameCondProbability) {
                        System.out.println("  BETTER CAND :::" + extendedName + " ;url=" + winnerURL + " ;p(n|e)=" + cand.prob_n_cond_e +
                                "; p(e|n,∃e)=" + dict.get(extendedName).get(winnerURL));
                        winnerName = extendedName;
                        winnerNameCondProbability = cand.prob_n_cond_e;
                        winnerOffset = ts.start;
                    }
                }
            }

            System.out.println("  WINNER NAME FOR CAND: " + winnerName + "::: winner offset " + winnerOffset);

            if (winnerOffset < 0) {
                continue;
            }
            
            String winnerKey = winnerURL + "\t" + winnerName + "\t" + winnerOffset;

            if (winners.contains(winnerKey)) {
                continue;
            }
            winners.add(winnerKey);

            if (serializedMentions.contains(winnerKey)) {
                System.out.println("## GOOD RESULT ##");
            }
            System.out.println("## ANNOTATION ##");
            
            if (dummyScore <= 0 || dummyScore <= winnerScore) {
                Candidate c = new Candidate(winnerURL, null, winnerName, winnerOffset, -1);
                c.posteriorProb = winnerScore;
                c.prob_n_cond_e = winnerNameCondProbability;
                
                int start = winnerOffset - 20;
                if (start < 0) {
                    start = 0;
                }
                int end = winnerOffset + 20;
                if (end >= doc.getRawText().length()) {
                    end = doc.getRawText().length() - 1;
                }

                c.debug.context = doc.getRawText().substring(start, end).replace('\n', ' ');
                c.debug.initialName = name;
                
                winnerMatchings.add(c);
            }
        }
        

        
        // TODO: delete this, it might take a long time to finish
        for (TruthMention m : doc.truthMentions) {
            boolean wasFound = false;
            for (Candidate c : winnerMatchings) {
                if (c.textIndex == m.mentionOffsetInText && c.entityURL.compareTo(m.wikiUrl) == 0 && c.name.compareTo(m.anchorText) == 0) {
                    wasFound = true;
                    break;
                }
            }  
            
            if (!wasFound) {
                String key = "URL=" + m.wikiUrl + ";name=" + m.anchorText + ";offset=" + m.mentionOffsetInText + ";doc=" + doc.pageName;
                System.out.println("## UNFOUND MENTION ##: " + key);
                
                if (!debugMentionsInfos.get(key).get("isCandidate")) System.out.println("  NOT A CANDIDATE");
                if (!debugMentionsInfos.get(key).get("EntIsInInvdict")) System.out.println("  ENT NOT IN INVDICT");
                if (!debugMentionsInfos.get(key).get("NameIsInInvdictForEnt")) System.out.println("  NAME NOT IN INVDICT FOR ENT");
                if (!debugMentionsInfos.get(key).get("EntIsInAllEnts")) System.out.println("  ENT NOT IN ALL ENTS");

                for (Candidate c : candidates) {
                    if (c.textIndex == m.mentionOffsetInText) {
                        System.out.println("  [NORMAL CAND] url=" + c.entityURL + "; name=" + c.name + "; p(n|e)=" + c.prob_n_cond_e + "; p(e|n,∃e)=" + dict.get(c.name).get(c.entityURL) + "; offset=" + c.textIndex);
                       
                    }
                }
                for (Candidate c : winnerMatchings) {
                    if (c.textIndex == m.mentionOffsetInText) {
                        System.out.println("  [WINNER CAND] url=" + c.entityURL + "; initial name:" + c.debug.initialName + "; final name=" + c.name + "; p(n|e)=" + c.prob_n_cond_e + "; p(e|n,∃e)=" + dict.get(c.name).get(c.entityURL) + " ; winner entity score l(n \\in t, e)=" + c.posteriorProb + 
                                "; offset=" + c.textIndex + " ;context :::" + c.debug.context );
                       
                    }
                }  

                System.out.println();
            }
        }

        
        return winnerMatchings;
    }

    
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
    // Main function that generates the candidates and selects the highest scored ones.
    // Supports both simple and extended token span implementations with dummy entity or not.
    public static void run(
            String invdictFilename, 
            String dictFilename, 
            String allEntitiesFilename, 
            String dummyProbsFilename,
            Double theta, 
            GenericPagesIterator inputPagesIterator,
            boolean extendedTokenSpan,
            boolean includeDummyEnt) throws IOException, InterruptedException {

        LoadAllEntities(allEntitiesFilename);

        // ***** STAGE 1: Generate all candidate pairs (n,e) such that P(n|e) >= theta
        HashSet<String> allEntitiesFromAllPages = GetAllEntitiesFromAllPages(inputPagesIterator);        
        LoadAndPruneInvdict(invdictFilename, allEntitiesFromAllPages);

        HashSet<String> allCandidateNames = new HashSet<String>();
        Vector<Vector<Candidate>> allCandidates = new Vector<Vector<Candidate>>();
        GenericPagesIterator p = inputPagesIterator.hardCopy();

        // TODO : delete this in the end !!!!!!!
        HashMap<String,HashMap<String, Boolean>> debugMentionsInfos = new HashMap<String, HashMap<String,Boolean>>();

        GenAllCandidatesFromAllPages(p, theta, allCandidateNames, allCandidates, debugMentionsInfos);

        System.gc(); // Clean inv.dict index
        // ***** STAGE 2: Compute the l(n,e) values, group by n and find the winning candidate.        
        dict = LoadCrosswikisDict.LoadAndPruneDict(dictFilename, allCandidateNames);

        if (includeDummyEnt) {
            LoadDummyProbs(dummyProbsFilename);
        }
        
        System.err.println("[INFO] Writing winner entities...");
        p = inputPagesIterator.hardCopy();
        int nr_page = -1;
        while (p.hasNext()) {
            nr_page++;

            GenericSinglePage doc = p.next();
            System.out.println("[INFO] ------- Page: " + nr_page + " ---- Page name: " + doc.pageName);

            // compute M.doc.E
            HashSet<String> docEntities = new HashSet<String>();
            for (TruthMention m : doc.truthMentions) {
                if (m.wikiUrl.length() > 0) {
                    docEntities.add(m.wikiUrl);
                }
            }			

            HashMap<String,String> wikiFreebaseMap = new HashMap<String,String>();
            
            for (TruthMention m : doc.truthMentions) {
                wikiFreebaseMap.put(m.wikiUrl, m.freebaseId);
            }
            
            if (extendedTokenSpan) {
                Vector<Candidate> matchings = 
                    GenWinningEntitiesWithExtendedTokenSpan(doc, allCandidates.get(nr_page), docEntities, includeDummyEnt, debugMentionsInfos);
                
                /*
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
                if (finalMatchings.size() == 0) continue;
                Candidate currentCandidate = finalMatchings.get(finalMatchingsIndex);
                
                StringBuilder allTextBuilder = new StringBuilder();
                for (int off = 0; off < i.getRawText().length(); off++) {
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
                        allTextBuilder.append(i.getRawText().charAt(off));
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
                */
            } else {
                GenWinningEntities(doc, allCandidates.get(nr_page), docEntities, includeDummyEnt);
            }
        }
        System.err.println("[INFO] --------------- Done ---------------");
    }

}
