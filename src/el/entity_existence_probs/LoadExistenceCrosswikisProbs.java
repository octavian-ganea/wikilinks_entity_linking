package el.entity_existence_probs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;



public class LoadExistenceCrosswikisProbs {
    
    public static HashMap<String, DummyIntPair> load(String existenceProbsFilename, String valueToKeep, double multiplyConst) throws IOException {
        System.err.println("[INFO] Loading existence (dummy) probs P(M.ent != dummy| M.ent = n) index...");

        HashMap<String, DummyIntPair> map = new HashMap<String, DummyIntPair>();
        BufferedReader in = new BufferedReader(new FileReader(existenceProbsFilename));
        String line = in.readLine();
        while (line != null && line.length() > 0 && !line.startsWith("Num 1 =")) {
            StringTokenizer st = new StringTokenizer(line, "\t");
            String name = st.nextToken();
            
            StringTokenizer scoresSt = new StringTokenizer(st.nextToken(), " ");
            while (scoresSt.hasMoreTokens() && scoresSt.nextToken().compareTo(";" + valueToKeep) != 0) {}
            
            double score = Double.parseDouble(scoresSt.nextToken()) * multiplyConst;
            if (score > 1.0) score = 1.0;
            
            int numDocsWithName = 1000000;
            int numDocsWithAnchorName = (int) (((double)numDocsWithName) * score);

            // Set a treshold of trust for these dummy probabilities.
            map.put(name, new DummyIntPair(numDocsWithAnchorName, numDocsWithName));
            line = in.readLine();
        }       

        System.err.println("[INFO] Done. Size = " + map.size());
        return map;
    }
}
