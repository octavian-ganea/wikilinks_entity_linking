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
            // Remove duplicate mentions:
            if (!serializedMentions.contains(tm.toString())) {
                this.truthMentions.add(tm);
                
                /*
                int start = Math.max(0, tm.mentionOffsetInText - 30);
                int end = Math.min(rawText.length() - 1, tm.mentionOffsetInText + 30);
                String context = rawText.substring(start, end).replace('\n', ' ');
                System.err.println("  DOC: " + pageName + "\n  NAME: " + tm.anchorText + "\n  OFFSET: " + tm.mentionOffsetInText + "\n  URL: " + tm.wikiUrl +
                        "\n  CONTEXT: " + context + "\n");
                */
            }
            serializedMentions.add(tm.toString());
        }        
    }
}
