package el;

public class Candidate {
    String entityURL;
    String freebaseID;
    String name;
    int textIndex;
    double prob_n_cond_e; // probability P(n|e)

    // numerator(e,n,E) = p(e|n)/(p(e \in E)
    // posteriorProb(candidate,E) = score(e,n,E) = numerator(n,e,E) / (denominator(n,e,E) + p(dummy|n)/(1 - p(dummy|n))))
    // Compute also debug values for candidate: 
    //    - dummyPosteriorProb = score(dummy,n,E) = p(dummy|n) / ((1-p(dummy|n)) * (numerator(n,e,E)+denominator(n,e,E)))
    double posteriorProb;
    Debug debug;

    class Debug {
        // Debug values
        double dummyPosteriorProb; // the posterior prob for the dummy entity
        double prob_e_cond_n;
        int prob_e_in_E_times_nr_docs;
        double denominator;
        double dummy_contribution;
        String context;
        String initialName;  // the initial token span of this candidate. A new subtoken span of n-nn+ might be generated (it will be stored in this.name)
    }

    public Candidate(String entityURL, String freebaseID, String name, int textIndex, double prob_n_cond_e) {
        this.entityURL = entityURL;
        this.freebaseID= freebaseID ;
        this.name = name;
        this.textIndex= textIndex ;
        this.debug = new Debug();
        this.prob_n_cond_e = prob_n_cond_e;
        this.debug.dummyPosteriorProb = -1;
    }
}
