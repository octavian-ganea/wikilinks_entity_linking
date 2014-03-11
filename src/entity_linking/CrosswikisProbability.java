package entity_linking;

import java.util.HashMap;
import java.util.StringTokenizer;

public class CrosswikisProbability {

    protected HashMap<String, Integer> numerator;
    protected HashMap<String, Integer> denominator;
    
    public CrosswikisProbability() {
        numerator = initMapOfWs();
        denominator = initMapOfWs();
    }

    public static HashMap<String, Integer> initMapOfWs() {
        HashMap<String, Integer> hm = new HashMap<String, Integer>();
        hm.put("w", 0);
        hm.put("w'", 0);
        hm.put("W", 0);
        hm.put("Wx", 0);
        return hm;
    }

    public String toString() {
        String rez = "";
        for (String s : numerator.keySet()) {
            rez += s + ":" + numerator.get(s) + "/" + denominator.get(s) + " "; 
        }
        return rez;
    }
    
    public double getScore() {
        double num = 0.0, den = 0.0;
        if (numerator.size() != 4 || denominator.size() != 4) {
            System.err.println("[ERROR] Fatal, numerator and denominator sizes:" + numerator.size() + " " + denominator.size() );
            System.exit(1);
        }
        for (String key : numerator.keySet()) {
            num += numerator.get(key);
        }
        for (String key : denominator.keySet()) {
            den += denominator.get(key);
        }
        return (0.0 + num)/den;
    }
}


class CrosswikisProbabilityForDict extends CrosswikisProbability {
    public void setDenominator(HashMap<String, Integer> den) {
        this.denominator = den;
    }
       
    // Add values to the numerator and possibily add more entries to the denominator (if they are missing).
    // EncodedValue is a string of form "key:numerator/denominator" where
    // key is one of "w", "w'", "W", "Wx".
    public void addNumeratorsAndUpdateDenominators(String encodedValue, HashMap<String, Integer> den) {
        encodedValue = encodedValue.trim();
        if (!encodedValue.contains(":")) {
            return;
        }
                
        String key = encodedValue.substring(0,encodedValue.indexOf(':'));
        
        if (key.compareTo("w") != 0 && key.compareTo("w'") != 0 && key.compareTo("W") != 0 && key.compareTo("Wx") != 0) {
            return;
        }
        
        if (!encodedValue.contains("/")) {
            System.err.println("[ERROR] Fatal, unexpected format of key in Crosswikis:" + encodedValue);
            System.exit(1);            
        }
        
        StringTokenizer st = new StringTokenizer(encodedValue.substring(encodedValue.indexOf(':') + 1), "/");
        numerator.put(key, Integer.parseInt(st.nextToken()) + numerator.get(key));
        
        if (!den.containsKey(key) || den.get(key) == 0) {
            String token = st.nextToken();
            den.put(key, Integer.parseInt(token));

        }
    }
}


class CrosswikisProbabilityForInvdict extends CrosswikisProbabilityForDict {
    protected int getNumerator(String key) {
        return numerator.get(key);
    }

    protected HashMap<String, Integer> getDenominator() {
        return denominator;
    }

    // Add values to the numerator.
    public void addNumerator(CrosswikisProbabilityForInvdict cp) {
        for (String key : numerator.keySet()) {
            numerator.put(key, cp.getNumerator(key) + numerator.get(key));
        }
    }    

    public void addDenominator(HashMap<String, Integer> den) {
        for (String key : den.keySet()) {
            denominator.put(key, den.get(key) + denominator.get(key));
        }
    }    
}