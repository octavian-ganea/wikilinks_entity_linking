package entity_linking.context_probs;

import java.util.HashMap;
import java.util.Set;

import entity_linking.Utils;

public class OverlappingTriplet {
    public String n;
    public String e;
    private int num_n_e; // #(n,e)
    
    // Map from contexts (n U n', for all possible overlapping names n')
    // to counts of these contexts when (n,e) anchor appears = #(context,n,e).
    private HashMap<String, Integer> num_context_n_e = new HashMap<String, Integer>();
    
    public OverlappingTriplet(String n, String e) {
        this.n = n;
        this.e = e;
        num_n_e = 0;
    }
    
    public int get_num_n_e() {
        return num_n_e;
    }
    
    public String serialize() {
        return e + "\t" + n;
    }
    
    public void addContext(String context) {
        num_context_n_e.put(context, 0);
    }
    
    public Set<String> allContexts() {
        return num_context_n_e.keySet();
    }
    
    public void increment_num_context_n_e(String context) {
        if (!num_context_n_e.containsKey(context)) {
            addContext(context);
        }
        num_context_n_e.put(context, 1 + num_context_n_e.get(context));
    }
    
    public void increment_num_n_e() {
        num_n_e++;
    }
    
    public double prob_context_cond_n_e (String context) {
        if (!num_context_n_e.containsKey(context)) return 1.0;
        if (num_n_e < 5) return 1.0;
        return ((double) num_context_n_e.get(context)) / num_n_e;
    }

    public String elems_of_context_cond_n_e (String context) {
        return num_context_n_e.get(context) + "\t" + num_n_e;
    }

    public void toSTDOUT() {
        System.out.println("name=" + n + " ; ent=" + e);
        System.out.println("contexts= ");
        for (String context : num_context_n_e.keySet()){
            System.out.print(context + " ;; ");
        }
        System.out.println();
        System.out.println();
    }
}
