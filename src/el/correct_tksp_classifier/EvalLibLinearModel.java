package el.correct_tksp_classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.StringTokenizer;
import java.util.Vector;


public class EvalLibLinearModel {
    private Vector<Double> w;
    
    public EvalLibLinearModel(String modelFilename) throws IOException {
        loadWeights(modelFilename);
        
        for (int i = 1; i < w.size(); ++i) {
            System.err.printf("%d:%.2f ", i , w.get(i));
            if (i%20 == 0) {
                System.err.println();
            }
        }
        System.err.println();
        
    }
    
    private void loadWeights(String modelFilename) throws IOException {
        w = new Vector<Double>();
        w.add(0.0); // Because w is 1-indexed.
        
        BufferedReader in = new BufferedReader(new FileReader(modelFilename));
        String line = in.readLine();
        
        boolean lineWithWeights = false;
        
        while (line != null && line.length() > 0) {
            if (lineWithWeights) {
                // Two formats of lines in an input file: "num -num" or "num" 
                if (!line.contains(" ")) {
                    w.add(Double.parseDouble(line));
                } else {
                    StringTokenizer st = new StringTokenizer(line, " ");
                    w.add(Double.parseDouble(st.nextToken()));                    
                }
            }
            
            if (line.equals("w")) {
                lineWithWeights = true;
            }
            line = in.readLine();
        }
    }
    
    private Vector<Double> loadInstancePoint(String line) {
        StringTokenizer st = new StringTokenizer(line, " ");
        st.nextToken(); // class of this point
        
        Vector<Double> point = new Vector<Double>();
        point.add(0.0);
        while (st.hasMoreTokens()) {
            String featureString = st.nextToken();
            point.add(Double.parseDouble(featureString.substring(featureString.indexOf(':') + 1)));
        }
        point.add(1.0); // bias term
        return point;
    }
    
    private double decisionFunction(Vector<Double> point, ScoreType scoreType) {
        if (scoreType == ScoreType.LIBLINEAR_SCORE) {
            BigDecimal rez = new BigDecimal(0.0);
            for (int i = 1; i < w.size(); ++i) {
                rez = rez.add(new BigDecimal(w.get(i)).multiply(new BigDecimal(point.get(i))));
            }
            return rez.doubleValue();
        }
        
        if (scoreType == ScoreType.CROSSWIKIS_SCORE_ONLY) {
            return point.get(1);
        }
        
        if (scoreType == ScoreType.LONGEST_TKSP_SCORE) {
            int nrTokens = 0;
            for (int i = 1; i <= 10; ++i) {
                nrTokens += i * point.get(i+2);
            }
            return nrTokens;
        }
        
        System.err.println("[FATAL] Invalid score type.");
        System.exit(1);
        return 0;
    }

    // Eval of the category of each point exactly like a classifier evaluation will do.
    // LIBLINEAR_SCORE is used
    public void evalEachPoint(String testFilename) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(testFilename));
        String line = in.readLine();

        int numCases = 0;
        int numCorrectlyClassifiedCases = 0;
        
        while (line != null && line.length() > 0) {
            boolean positive = true;
            if (!line.startsWith("1 ")) {
                positive = false;
            }
            numCases++;
            Vector<Double> point = loadInstancePoint(line);
            Double decisionScore = decisionFunction(point, ScoreType.LIBLINEAR_SCORE);

            if (positive == (decisionScore > 0)) {
                numCorrectlyClassifiedCases++;
            }
            line = in.readLine();
        }
        
        double accuracy = (100 * numCorrectlyClassifiedCases + 0.0) / numCases;
        System.err.printf("[FINAL RESULTS OF EACH POINT] accuracy = %.4f\n", accuracy);
    }
    
    
    // Eval of each test case to see if the positive token span ranks the highest or not
    // (higher than the rest of negative candidates corresponding to this test case).
    public void evalEachTestCase(String testFilename, String testVerboseFilename, ScoreType scoreType, boolean debugInfo) throws IOException {
        BufferedReader verboseIn = null; 
        String lineVerbose = null;
        if (debugInfo) {
            verboseIn = new BufferedReader(new FileReader(testVerboseFilename));
            lineVerbose = verboseIn.readLine();
        }
        
        BufferedReader in = new BufferedReader(new FileReader(testFilename));
        String line = in.readLine();
        int nrLine = 0;
        nrLine++;
        
        int numCases = 0;
        int numCorrectlyClassifiedCases = 0;
        
        while (line != null && line.length() > 0) {
            if (!line.startsWith("1 ")) {
                System.err.println("[FATAL] Test file is ill formed!");
                System.exit(1);
            }
            numCases++;
            Vector<Double> positivePoint = loadInstancePoint(line);
            double positiveDecisionScore = decisionFunction(positivePoint, scoreType);
            double bestNegativeDecisionScore = -1;
            
            int positiveVerboseLineNr = nrLine;
            int bestNegativeVerboseLineNr = -1;
            
            line = in.readLine();
            nrLine++;

            while (line != null && line.length() > 0 && line.startsWith("0 ")) {
                Vector<Double> negativePoint = loadInstancePoint(line);
                double negativeDecisionScore = decisionFunction(negativePoint, scoreType);
                if (negativeDecisionScore >= positiveDecisionScore) {
                    bestNegativeVerboseLineNr = nrLine;
                    bestNegativeDecisionScore = negativeDecisionScore;
                }
                line = in.readLine();
                nrLine++;
            }
            
            if (bestNegativeVerboseLineNr < 0) {
                numCorrectlyClassifiedCases++;
            } else if (debugInfo){
                System.err.println("positive = " + positiveDecisionScore);
                System.err.println("negative best = " + bestNegativeDecisionScore);
                System.err.println();
                
                while (lineVerbose != null && !lineVerbose.endsWith(": " + positiveVerboseLineNr)) {
                    lineVerbose = verboseIn.readLine();
                }
                
                while (lineVerbose != null && !lineVerbose.endsWith(": " + (bestNegativeVerboseLineNr + 1))) {
                    System.err.println(lineVerbose);
                    lineVerbose = verboseIn.readLine();
                }
                System.err.println("------------------------------");
            }
        }
        
        double accuracy = (100 * numCorrectlyClassifiedCases + 0.0) / numCases;
        System.err.printf("[FINAL RESULTS OF EACH CASE] scoretype = %s ; accuracy = %.4f\n", scoreType, accuracy);
    }
}
