package entity_linking.input_data_pipeline;

import java.util.Vector;

// Generic class that models a raw text together with a list of ground truth entity linking annotations.
// The constructor should fill all the public fields of this object.
public abstract class GenericSinglePage {
    public String pageName;
    
    // Vector of truth mentions (in any order). Might contain duplicates!
    public Vector<TruthMention> truthMentions;
    
    protected String rawText;
    
    public GenericSinglePage() {
        truthMentions = new Vector<TruthMention>();
    }
    
    public String getRawText() {
        return rawText;
    }
}
