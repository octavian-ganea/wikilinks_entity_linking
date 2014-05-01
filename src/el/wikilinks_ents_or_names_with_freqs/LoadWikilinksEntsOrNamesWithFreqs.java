package el.wikilinks_ents_or_names_with_freqs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class LoadWikilinksEntsOrNamesWithFreqs {
    public static HashMap<String, Integer> load(String filename, String namesOrEnts) throws IOException {
        System.err.println("[INFO] Loading all " + namesOrEnts + "...");

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        while (line != null && line.length() > 0 && !line.startsWith("NR DOCS:")) {
            StringTokenizer st = new StringTokenizer(line, "\t");
            String url = st.nextToken();
            if (!st.hasMoreTokens()) {
                System.err.println("[ERROR] wrong line format in all entities file :" + line + "::::::");
                line = in.readLine();
                continue;
            }
            int freq = Integer.parseInt(st.nextToken());
            map.put(url, freq);
            line = in.readLine();
        }
        
        System.err.println("[INFO] All " + namesOrEnts + " size : " + map.size());
        return map;
    }
}


