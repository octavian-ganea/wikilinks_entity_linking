package el.correct_tksp_classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.TreeMap;


public class EvalLinearOrSVMModel {
    public static enum ScoreType {
        // If the positive token span ranks the highest or not using the distance to the separation hyperplane 
        HYPERPLANE_DISTANCE,
        // If the correct token span is labeled positive and the rest are labeled negative using the classifier predicted labels
        CLASSIFIER,
        // If the positive token span ranks the highest or not using the decision function p(n|e)
        CROSSWIKIS_PROB_ONLY,
        // If the positive token span ranks the highest or not using the decision function #tokens
        LONGEST_TKSP
    }
    
    public static enum ModelType {
        LIBSVM,
        LIBLINEAR
    }
    
    private ClassifierModel model;
    
    public EvalLinearOrSVMModel(String modelFilename, ModelType modelType) throws IOException {
        if (modelType == ModelType.LIBLINEAR) {
            model = new ClassifierLinearModel();
        } else {
            model = new ClassifierSVMModel();            
        }
        
        model.loadModel(modelFilename);
    }
        
    public void eval(String testFile, String testFileVerbose, boolean verbose) throws IOException {
        System.err.println();
        System.err.println("Eval " + model.name() + " for file : " + testFile);
        evalEachPoint(testFile);
        evalEachTestCase(testFile, testFileVerbose, EvalLinearOrSVMModel.ScoreType.CLASSIFIER, verbose);
        evalEachTestCase(testFile, testFileVerbose, EvalLinearOrSVMModel.ScoreType.HYPERPLANE_DISTANCE, verbose);
        evalEachTestCase(testFile, testFileVerbose, EvalLinearOrSVMModel.ScoreType.CROSSWIKIS_PROB_ONLY, verbose);
        evalEachTestCase(testFile, testFileVerbose, EvalLinearOrSVMModel.ScoreType.LONGEST_TKSP, verbose);
        
    }
    
    private double decisionFunction(TreeMap<Integer, Double> point, ScoreType scoreType) {
        if (scoreType == ScoreType.HYPERPLANE_DISTANCE) {
            return model.decisionFunction(point);
        }
        
        if (scoreType == ScoreType.CLASSIFIER) {
            return (model.decisionFunction(point) >= 0 ?  1.0 : -1.0);
        }
        
        if (scoreType == ScoreType.CROSSWIKIS_PROB_ONLY) {
            return point.get(1);
        }
        
        if (scoreType == ScoreType.LONGEST_TKSP) {
            int nrTokens = 0;
            for (int i = 1; i <= 10; ++i) {
                if (point.containsKey(i+2)) {
                    nrTokens += i * point.get(i+2);
                }
            }
            return nrTokens;
        }
        
        System.err.println("[FATAL] Invalid score type.");
        System.exit(1);
        return 0;
    }

    // Parses a string line representing a vector (an N-dim point) in the form "1:val_1 2:val_2 ..."
    private TreeMap<Integer, Double> loadInstancePoint(String line) {
        StringTokenizer st = new StringTokenizer(line, " ");
        st.nextToken(); // class of this point
        
        TreeMap<Integer, Double> point = new TreeMap<Integer, Double>();
        while (st.hasMoreTokens()) {
            String featureString = st.nextToken();
            int index = Integer.parseInt(featureString.substring(0, featureString.indexOf(':')));
            double val = Double.parseDouble(featureString.substring(featureString.indexOf(':') + 1));
            if (val != 0) {
                point.put(index, val);
            }
        }
        return point;
    }
    
    // Eval of the category of each point exactly like a classifier evaluation will do.
    // This is mostly used as a DEBUG tool to check if this code returns the same results as svm-predict . 
    public void evalEachPoint(String testFilename) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(testFilename));
        String line = in.readLine();
        int nrLine = 0;
        
        int numCases = 0;
        int numCorrectlyClassifiedCases = 0;
        
        while (line != null && line.length() > 0) {
            boolean positive = true;
            if (!line.startsWith("1 ")) {
                positive = false;
            }
            numCases++;
            TreeMap<Integer, Double> point = loadInstancePoint(line);
            Double decisionScore = decisionFunction(point, ScoreType.CLASSIFIER);

            if (positive == (decisionScore > 0)) {
                numCorrectlyClassifiedCases++;
            }
            line = in.readLine();
            
            nrLine ++;
            if (nrLine % 200 <= 1) {
                //System.err.println(nrLine);
            }
        }
        
        double accuracy = (100 * numCorrectlyClassifiedCases + 0.0) / numCases;
        System.err.printf("[FINAL RESULTS OF EACH POINT] accuracy = %.4f\n", accuracy);
    }
    
    
    // Eval of each test case.
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
            TreeMap<Integer, Double> positivePoint = loadInstancePoint(line);

            double positiveDecisionScore = decisionFunction(positivePoint, scoreType);

            int positiveVerboseLineNr = nrLine;

            double bestNegativeDecisionScore = -1;
            int bestNegativeVerboseLineNr = -1;
            
            line = in.readLine();
            nrLine++;

            while (line != null && line.length() > 0 && line.startsWith("0 ")) {
                TreeMap<Integer, Double> negativePoint = loadInstancePoint(line);
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
