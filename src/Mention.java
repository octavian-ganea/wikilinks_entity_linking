import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class Mention {
	// This url is not decoded !!! Use prunnedURL() for this
	String wiki_url = null;
	
	String freebase_id = null;
	String anchor_text = null;
	
	// The index from the page's text before removing the tags [[[start num]]]
	int old_text_offset = -1;
    // The index from the page's clean text after removing the tags [[[start num]]]
	int text_offset = -1;
	
	// e.g. http://en.wikipedia.org/wiki/Pharoahe_Monch; /m/03f5_l2 --> Pharoahe Monch
	public Mention(String serializable) {
		if (!serializable.contains(" --> ")) {
			System.err.println("Wrong string as a mention.");
			System.exit(1);
		}
		anchor_text = serializable.substring(serializable.indexOf("--> ") + "--> ".length());
		if (serializable.indexOf("; /m/") != -1) {
			freebase_id = serializable.substring(serializable.indexOf("; /m/") + 2, serializable.indexOf(" --> "));
		}
		wiki_url = serializable.substring(0,serializable.indexOf("; "));
		wiki_url = Utils.pruneURL(wiki_url);
	}
	
	public String toString() {
		return wiki_url + "; " + freebase_id + " --> " + anchor_text + " ------- offset= " + text_offset;
	}
	
	public Mention clone() {
		return new Mention(wiki_url +"; " + freebase_id + " --> " + anchor_text);
	}
}
