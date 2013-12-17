import java.util.TreeMap;
import java.util.Vector;

// Given a set of mentions and a text representing the text from a HTML file, 
// this class returns the sentences from the text that contain at least one anchor. 
// The returned sentences will be annotated with the mentions (wikipedia link + freebaseId).
// For example: 
// 		text = "Barack Obama met with Putin.";
//		mentions = ("Barack Obama":(wikipedia/Barak_Obrama; freebaseid= 920))
//				   ("Putin":(wikipedia/Vladimir_Putin; freebaseid= 230))
// returns: "[Barack Obama]<#wikipedia/Barack_Obama;920#> met with [Putin]<#wikipedia/Putin;230#>."
public class AnnotateSentFromCurrentPage implements AnnotateSentences {
	// Num of mentions after string matching. Tries to find in the current page
	// as many strings representing the same entity as possible based 
	// just on the mentions from the current page.
	private int total_num_local_matched_entities; 	
	
	public AnnotateSentFromCurrentPage() {
		total_num_local_matched_entities = 0;
	}
	
	// Given the input string, it inserts the mentions in it near the corresponding substrings. 
	private String insertMentions(String text, TreeMap<String, Mention> mentions) {
		String rez = text;
		int nr_anchors = 0;
		int nr_null_freebase_ids = 0;
		
		for (Mention m : mentions.values()) {
			int lastIndex = 0;
			while (lastIndex != -1) {
				lastIndex = rez.indexOf(m.anchor_text, lastIndex);
				if ( lastIndex != -1){
					int f = rez.indexOf("[",lastIndex), l = rez.indexOf("#>",lastIndex);
					if (l != -1 && (f == -1 || f > l)) { // We are inside an anchor already
						lastIndex += m.anchor_text.length();
					} else {
						StringBuilder sb = new StringBuilder();
						sb.append(rez.substring(0, lastIndex));
						sb.append("[");
						sb.append(rez.substring(lastIndex, lastIndex + m.anchor_text.length()));
						String t = "]<#" + m.wiki_url.substring(7) + ";" + m.freebase_id + "#>";
						sb.append(t);
						sb.append(rez.substring(lastIndex + m.anchor_text.length()));
						rez = sb.toString();
						nr_anchors ++;
						if (m.freebase_id == null) {
						    nr_null_freebase_ids++;
						}

						lastIndex += m.anchor_text.length() + 1 + t.length();
					}
				}
			}
		}
		if (nr_anchors == 0) return null;
		total_num_local_matched_entities += nr_anchors;
		return rez;
	}

	// Tries to find in the current page as many strings representing the same entity as possible based 
	// just on the mentions from the current page.
	public void annotateSentences(WikiLinkItem item) {
		// Remove duplicates in mentions:
		TreeMap<String, Mention> mentions_hashtable = new TreeMap<String,Mention>(new Utils.StringComp());
		for (Mention m : item.mentions) {
			if (!mentions_hashtable.containsKey(m.anchor_text)) {
				mentions_hashtable.put(m.anchor_text, m);
			}
		}

		boolean has_one_sent = false;
		// Vector with sentences annotated with their freebase ids.
		for (String s : item.sentences) {
			String ss = insertMentions(s, mentions_hashtable);
			if (ss != null) {
				System.out.println(">>>>>\n" + ss);		
				has_one_sent = true;
			}
		}

		if (has_one_sent)
			System.out.println("------- Finished page " + item.page_num + " ----");
	}

	@Override
	public int getTotalNumAnnotations() {
		return total_num_local_matched_entities;
	}
}
