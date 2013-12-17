import java.util.Vector;

public class WikiLinkItem {
	int page_num; // from the current shard
	int doc_id;
	String url;
	Vector<Mention> mentions;
	Vector<String> sentences;
	String all_text;
	
	public WikiLinkItem() {
		mentions = new Vector<Mention>();
		sentences = new Vector<String>();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("-------------------------------------\n");
		sb.append("--- Page num: " + page_num + "\n");
		sb.append("--- Docid: " + doc_id + "\n");
		sb.append("--- URL: " + url + "\n");
		sb.append("--- Mentions:" + "\n");
		for (Mention m : mentions) {
			sb.append(m.toString() + "\n");
		}
		sb.append("--- Sentences:" + "\n");
		for (String s : sentences) sb.append(s + "\n");
		sb.append("--- All text:" + "\n" + all_text);
		
		return sb.toString();
	}
	
	// Remove tags in the text.
	public void cleanText() {
		//System.out.println(all_text);
		
		int k = 0;
		for (Mention m : mentions) {
			m.old_text_offset = all_text.indexOf("[[[start " + k + "]]]");
			k++;
		}
		
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < all_text.length(); ++i) {
			if (all_text.charAt(i) == '[' && all_text.indexOf("[[[start", i) == i) {
				int mentionIndex = Integer.parseInt(
						all_text.substring(i + "[[[start ".length(), all_text.indexOf("]]]",i)));
				int mentionOffset = sb.length();
				
				boolean wasSpace = (i > 0 && all_text.charAt(i-1) == ' ');

				i = all_text.indexOf("]]]",i) + "]]]".length();
				if (wasSpace && all_text.charAt(i) == ' ') {
				    i++;
				}
				if (all_text.charAt(i) == ' ') {
				    mentionOffset++;
				}
				
				int j = all_text.indexOf("[[[end", i);
				for (;i<j;++i) {
				    sb.append(all_text.charAt(i));
				}
				
				wasSpace = (all_text.charAt(j-1) == ' ');
				i = all_text.indexOf("]]]",j) + "]]]".length() - 1;
				if (wasSpace && i < all_text.length() - 1 && all_text.charAt(i) == ' ') {
				    i++;
				}
				
				mentions.get(mentionIndex).text_offset = mentionOffset;
			} else {
				sb.append(all_text.charAt(i));
			}
		}
		all_text = sb.toString();
	}
}
