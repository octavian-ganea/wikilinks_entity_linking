package el.context_probs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class LoadContextProbs {
    public static HashMap<String,Double> load(String file) throws IOException {
        System.err.println("[INFO] Loading all context probs P(context | n,e)...");

        HashMap<String,Double> rez = new HashMap<String,Double>();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line = in.readLine();
        while (line != null) {
            StringTokenizer st = new StringTokenizer(line, "\t");

            String e = st.nextToken();
            String n = st.nextToken();
            String context = st.nextToken();
            st.nextToken();
            st.nextToken();
            
            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }
            double prob = Double.parseDouble(st.nextToken());

            rez.put(e + "\t" + n + "\t" + context, prob);
            line = in.readLine();
        }

        System.err.println("[INFO] Done. size : " + rez.size());

        return rez;
    }
}
