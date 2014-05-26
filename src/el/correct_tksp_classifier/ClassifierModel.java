package el.correct_tksp_classifier;

import java.io.IOException;
import java.util.TreeMap;
import java.util.Vector;

public interface ClassifierModel {

    public String name();
    
    public void loadModel(String modelFilename) throws IOException;
    
    public double decisionFunction(TreeMap<Integer, Double> point);

}
