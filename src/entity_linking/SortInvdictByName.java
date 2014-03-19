package entity_linking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class SortInvdictByName {

    public static void run(String invdictfilename) throws IOException{        
        // <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]
        System.err.println("[INFO] Sorting inv.dict P(n|e) by their name ...");

        TreeMap<String, Vector<String>> map = new TreeMap<String,Vector<String>>();
        
        BufferedReader in = new BufferedReader(new FileReader(invdictfilename));
        String line = in.readLine();
        int nr_line = 0;

        while (line != null && line.length() > 0) {
            nr_line ++;
            if (nr_line % 5000000 == 0) {
                System.err.println("loaded " + nr_line);
            }

            StringTokenizer st = new StringTokenizer(line, "\t");

            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            String rawURL = st.nextToken();

            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }
 
            String left = st.nextToken();
            double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));  

            if (cprob < 0.00005) {
                line = in.readLine();
                continue;             
            }
            
            String mention = left.substring(left.indexOf(" ") + 1);         

            if (mention.length() > 0 && mention.charAt(0) <= 'Z') {
                line = in.readLine();
                continue;
            }
            
            if (!map.containsKey(mention)) {
                map.put(mention, new Vector<String>());
            }
            map.get(mention).add(rawURL+ "\t" + cprob);
            
            line = in.readLine();
        }
        in.close();
        
        for (String name : map.keySet()) {
            for (String l : map.get(name)) {
                System.out.println(l + " " + name);
            }
        }
    }
}
