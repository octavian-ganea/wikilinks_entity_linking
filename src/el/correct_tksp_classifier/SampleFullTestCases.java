package el.correct_tksp_classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

// Randomly select 2% of an input Wikilinks feature points file
public class SampleFullTestCases {

    public static void sample(String inputFile, String outputSampleFile, String outputRestFile) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        PrintWriter writerSampleFile = new PrintWriter(outputSampleFile, "UTF-8");
        PrintWriter writerRestFile = new PrintWriter(outputRestFile, "UTF-8");
        
        Vector<String> fullCase = new Vector<String>();
        String line = in.readLine();
        
        while (line != null && line.length() > 0) {
            if (!line.startsWith("1 ")) {
                System.err.println("[FATAL] Test file is ill formed!");
                System.exit(1);
            }

            fullCase.clear();
            fullCase.add(line);

            line = in.readLine();
            while (line != null && line.length() > 0 && line.startsWith("0 ")) {
                fullCase.add(line);
                line = in.readLine();
            }
            
            // 30% fir test, 70% for train
            if (Math.random() <= 0.3) {
                for (String s : fullCase) {
                    writerSampleFile.println(s);
                }
            } else {
                for (String s : fullCase) {
                    writerRestFile.println(s);
                }                
            }
        }
        
        writerSampleFile.flush();
        writerRestFile.flush();
        writerSampleFile.close();
        writerRestFile.close();
    }
}
