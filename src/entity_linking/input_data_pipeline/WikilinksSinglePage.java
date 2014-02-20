package entity_linking.input_data_pipeline;

import java.util.Vector;

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
	}
	
	// Remove tags in the text.
	private void cleanText() {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < rawText.length(); ++i) {
			if (rawText.charAt(i) == '[' && rawText.indexOf("[[[start", i) == i) {
				int mentionIndex = Integer.parseInt(
				        rawText.substring(i + "[[[start ".length(), rawText.indexOf("]]]",i)));
				int mentionOffset = sb.length();
				
				boolean wasSpace = (i > 0 && rawText.charAt(i-1) == ' ');

				i = rawText.indexOf("]]]",i) + "]]]".length();
				if (wasSpace && rawText.charAt(i) == ' ') {
				    i++;
				}
				if (rawText.charAt(i) == ' ') {
				    mentionOffset++;
				}
				
				int j = rawText.indexOf("[[[end", i);
				for (;i<j;++i) {
				    sb.append(rawText.charAt(i));
				}
				
				wasSpace = (rawText.charAt(j-1) == ' ');
				i = rawText.indexOf("]]]",j) + "]]]".length() - 1;
				if (wasSpace && i < rawText.length() - 1 && rawText.charAt(i) == ' ') {
				    i++;
				}
				
				truthMentions.get(mentionIndex).mentionOffsetInText = mentionOffset;
			} else {
				sb.append(rawText.charAt(i));
			}
		}
		rawText = sb.toString();
	}
}
