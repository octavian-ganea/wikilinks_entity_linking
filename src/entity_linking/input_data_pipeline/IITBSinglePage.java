package entity_linking.input_data_pipeline;

import java.util.Vector;

public class IITBSinglePage extends GenericSinglePage {
    public IITBSinglePage(String pageName, String rawText, Vector<TruthMention> truthMentions) {
        super();
        this.pageName = pageName;
        this.rawText = rawText;
        this.truthMentions = truthMentions;
    }
    
    

}
