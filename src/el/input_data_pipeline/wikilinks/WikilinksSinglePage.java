package el.input_data_pipeline.wikilinks;

import java.util.Vector;

import el.input_data_pipeline.GenericSinglePage;
import el.input_data_pipeline.TruthMention;
import el.utils.Utils;

public class WikilinksSinglePage extends GenericSinglePage {
	int docId;
	String pageUrl;
	
	public WikilinksSinglePage() {
	    truthMentions = new Vector<TruthMention>();
		docId = 0;
	}
	
	public void setAnnotatedText(String annotatedText) {
	    rawText = annotatedText;
	    cleanText();
	    
	    Vector<TruthMention> cleanedTruthMentions = new Vector<TruthMention>();
	    for (TruthMention tm : truthMentions) {
	        if (tm.mentionOffsetInText != -1) {
	            cleanedTruthMentions.add(tm);
	        }
	    }
	    truthMentions = cleanedTruthMentions;
	}
	
	// Remove tags in the text.
	private void cleanText() {
		StringBuilder sb = new StringBuilder();
		
		char lastAddedChar = ' ';
		for (int i = 0; i < rawText.length(); ++i) {
			if (rawText.charAt(i) == '[' && rawText.indexOf("[[[start", i) == i) {
				int mentionIndex = Integer.parseInt(
				        rawText.substring(i + "[[[start ".length(), rawText.indexOf("]]]",i)));

                i = rawText.indexOf("]]]",i) + "]]]".length();
                
                // including \n
				while (Character.isWhitespace(rawText.charAt(i))) {
				    if (!Character.isSpaceChar(rawText.charAt(i)) || !Character.isSpaceChar(lastAddedChar)) {
				        sb.append(rawText.charAt(i));
				        lastAddedChar = rawText.charAt(i);
				    }
                    i++;
				}
				int mentionOffset = sb.length();
				
				String anchor = truthMentions.get(mentionIndex).anchorText;
				sb.append(anchor);
				lastAddedChar = anchor.charAt(anchor.length() - 1);
				
				i = rawText.indexOf("[[[end", i);
				i = rawText.indexOf("]]]",i) + "]]]".length();
				
				truthMentions.get(mentionIndex).mentionOffsetInText = mentionOffset;
			} else {
                if (!Character.isSpaceChar(rawText.charAt(i)) || !Character.isSpaceChar(lastAddedChar)) {
                    sb.append(rawText.charAt(i));
                    lastAddedChar = rawText.charAt(i);
                }
			}
		}
		rawText = sb.toString();
	}
}
