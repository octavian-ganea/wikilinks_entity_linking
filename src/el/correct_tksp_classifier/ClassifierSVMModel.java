package el.correct_tksp_classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

public class ClassifierSVMModel implements ClassifierModel {

    private double gamma;
    private double rho;
    private Vector<Double> alpha; // this is actually alpha_i * y_i as described in LibSVM doc for the format of the model file
    private Vector<TreeMap<Integer, Double> > SV;
    
    public String name() {
        return "LIBSVM";
    }

    public void loadModel(String modelFilename) throws IOException {
        alpha = new Vector<Double>();
        SV = new Vector<TreeMap<Integer, Double> >();
        
        BufferedReader in = new BufferedReader(new FileReader(modelFilename));
        String line = in.readLine();
        
        boolean lineWithSV = false;
        
        while (line != null && line.length() > 0) {
            StringTokenizer st = new StringTokenizer(line, " ");

            if (lineWithSV) {
                alpha.add(Double.parseDouble(st.nextToken()));
                
                TreeMap<Integer, Double> point = new TreeMap<Integer, Double>();

                while (st.hasMoreTokens()) {
                    String featureString = st.nextToken();
                    int index = Integer.parseInt(featureString.substring(0, featureString.indexOf(':')));
                    double val = Double.parseDouble(featureString.substring(featureString.indexOf(':') + 1));
                    if (val != 0) {
                        point.put(index, val);
                    }
                }
                SV.add(point);
            }
            
            if (line.startsWith("SV")) {
                lineWithSV = true;
            }
            
            if(line.startsWith("gamma")) {
                st.nextToken();
                gamma = Double.parseDouble(st.nextToken());
            }
            
            if(line.startsWith("rho")) {
                st.nextToken();
                rho = Double.parseDouble(st.nextToken());
            }
            
            line = in.readLine();
        }
    }
    
    // exp(-gamma * |xi - xj|^2)
    private double RBFKernel(TreeMap<Integer, Double>  x, TreeMap<Integer, Double> y) {        
        Iterator<Integer> xIterator = x.keySet().iterator();
        Iterator<Integer> yIterator = y.keySet().iterator();

        double sum = 0;

        int xCurrent = -1, yCurrent = - 1;
        if (xIterator.hasNext()) xCurrent = xIterator.next();
        if (yIterator.hasNext()) yCurrent = yIterator.next();

        while (xCurrent != -1 && yCurrent != -1) {
            if (xCurrent == yCurrent) {
                double d = x.get(xCurrent) - y.get(yCurrent);
                sum += d*d;
                if (xIterator.hasNext()) xCurrent = xIterator.next();
                else xCurrent = -1;
                if (yIterator.hasNext()) yCurrent = yIterator.next();
                else yCurrent = -1;
            } else {
                if (xCurrent > yCurrent) {   
                    sum += y.get(yCurrent) * y.get(yCurrent);
                    if (yIterator.hasNext()) yCurrent = yIterator.next();
                    else yCurrent = -1;
                } else {
                    sum += x.get(xCurrent) * x.get(xCurrent);
                    if (xIterator.hasNext()) xCurrent = xIterator.next();
                    else xCurrent = -1;
                }
            }
        }

        while (xCurrent != -1) {
            sum += x.get(xCurrent) * x.get(xCurrent);
            if (xIterator.hasNext()) xCurrent = xIterator.next();
            else xCurrent = -1;
        }

        while (yCurrent != -1) {
            sum += y.get(yCurrent) * y.get(yCurrent);
            if (yIterator.hasNext()) yCurrent = yIterator.next();
            else yCurrent = -1;
        }

        return Math.exp(sum * (-gamma));
    }
    
    @Override
    public double decisionFunction(TreeMap<Integer, Double> point) {
        double rez = 0.0;
        for (int i = 0; i < alpha.size(); ++i) {
            rez += alpha.get(i) * RBFKernel(SV.get(i), point);
        }
        return rez - rho;
    }
 
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
