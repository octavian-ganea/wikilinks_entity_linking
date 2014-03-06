package entity_linking.input_data_pipeline;

import java.util.HashSet;
import java.util.Vector;

public class IITBSinglePage extends GenericSinglePage {
    public IITBSinglePage(String pageName, String rawText, Vector<TruthMention> truthMentions) {
        super();
        this.pageName = pageName;
        this.rawText = rawText;

        this.truthMentions = new Vector<TruthMention>();
        HashSet<String> serializedMentions = new HashSet<String>();
        for (TruthMention tm : truthMentions) {            
            if (!serializedMentions.contains(tm.toString())) {
                this.truthMentions.add(tm);
            }
            serializedMentions.add(tm.toString());
        }        
    }
}
