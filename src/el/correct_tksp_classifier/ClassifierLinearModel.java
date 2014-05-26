package el.correct_tksp_classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.StringTokenizer;
import java.util.TreeMap;


public class ClassifierLinearModel implements ClassifierModel {
    private TreeMap<Integer, Double> w;
    private double b = 0; // bias term
    
    public String name() {
        return "LIBLINEAR";
    }
    
    public void loadModel(String modelFilename) throws IOException {
        w = new TreeMap<Integer, Double>();
        
        BufferedReader in = new BufferedReader(new FileReader(modelFilename));
        String line = in.readLine();
        
        int lineWithWeights = -1;
        
        while (line != null && line.length() > 0) {
            if (lineWithWeights > 0) {
                // Two formats of lines in an input file: "num -num" or "num" 
                if (!line.contains(" ")) {
                    w.put(lineWithWeights, Double.parseDouble(line));
                } else {
                    StringTokenizer st = new StringTokenizer(line, " ");
                    w.put(lineWithWeights, Double.parseDouble(st.nextToken()));                    
                }
                lineWithWeights++;
            }
            
            if (line.equals("w")) {
                lineWithWeights = 1;
            }
            line = in.readLine();
        }

        b = w.get(w.lastKey());
        w.remove(w.lastKey());
        
        // Output model:
        /*
        System.err.println("Linear Model:");
        for (int i = 1; i < w.size(); ++i) {
            System.err.printf("%d:%.2f ", i , w.get(i));
            if (i%20 == 0) {
                System.err.println();
            }
        }
        System.err.println();
        */
    }
    
    public double decisionFunction(TreeMap<Integer, Double> point) {
        BigDecimal rez = new BigDecimal(0.0);
        for (Integer index : point.keySet()) {
            if (w.containsKey(index)) {
                rez = rez.add(new BigDecimal(w.get(index)).multiply(new BigDecimal(point.get(index))));
            }
        }
        return rez.doubleValue() + b;
    }
}
