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
}
