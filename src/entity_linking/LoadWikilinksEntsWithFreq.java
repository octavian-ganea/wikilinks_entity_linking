package entity_linking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class LoadWikilinksEntsWithFreq {
    public static HashMap<String, Integer> load(String filename) throws IOException {
        System.err.println("[INFO] Loading all entities...");

        HashMap<String, Integer> entsDocFreqsInCorpus = new HashMap<String, Integer>();
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
        int totalNumDocs = Integer.parseInt(in.readLine());

        System.err.println("[INFO] All ents size : " + entsDocFreqsInCorpus.size() + " ; total num docs = " + totalNumDocs);
        return entsDocFreqsInCorpus;
    }
}
