package el.entity_existence_probs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class LoadKeyphrasenessDummyProbs {
    public static HashMap<String, DummyIntPair> load(String existenceProbsFilename) throws IOException {
        System.err.println("[INFO] Loading dummy probs P(M.ent != dummy| M.ent = n) index...");

        HashMap<String, DummyIntPair> map = new HashMap<String, DummyIntPair>();
        BufferedReader in = new BufferedReader(new FileReader(existenceProbsFilename));
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
                map.put(name, new DummyIntPair(numDocsWithAnchorName, numDocsWithName));
            }
            line = in.readLine();
        }       

        System.err.println("[INFO] Done. Size = " + map.size());
        return map;
    }
}
