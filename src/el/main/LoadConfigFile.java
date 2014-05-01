package el.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class LoadConfigFile {
    public static String configFile = "run_config/config";
    
    public static HashMap<String,String> load(String runType) throws IOException {
        System.err.println("[INFO] Config params for run type = " + runType + ":");
        HashMap<String,String> rez = new HashMap<String,String>();
        
        BufferedReader in = new BufferedReader(new FileReader(configFile));
        String line = in.readLine();

        boolean insideConfig = false;
        while (line != null) {
            if (line.startsWith("#")) {
                line = in.readLine();
                continue;
            }
            if (line.equals(runType)) {
                insideConfig = true;
                line = in.readLine();
                continue;                
            }
            if (line.equals("")) {
                insideConfig = false;
                line = in.readLine();
                continue;                
            }
            if (!insideConfig) {
                line = in.readLine();
                continue;                                
            }
            
            if (!line.contains(":")) {
                System.err.println("Invalid params line in the config file: " + line);
                System.exit(1);
            }
            
            StringTokenizer st = new StringTokenizer(line, " :");
            
            String key = st.nextToken();
            String val = st.nextToken();
            while (val.equals("")) {
                val = st.nextToken();
            }
            
            rez.put(key, val);
            System.err.println("[INFO]   " + key + " --> " + val);
            line = in.readLine();
        }
        return rez;
    }
}
