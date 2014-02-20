package entity_linking.input_data_pipeline;

import entity_linking.Utils;

public class TruthMention {
    public String wikiUrl = null;
    public String freebaseId = null;
    public String anchorText = null;
	
    // The index from the clean page text after removing the tags [[[start num]]]
	public int mentionOffsetInText = -1;
	
	// Example of serializable: "http://en.wikipedia.org/wiki/Pharoahe_Monch; /m/03f5_l2 --> Pharoahe Monch"
	public TruthMention(String serializable) {
		if (!serializable.contains(" --> ")) {
			System.err.println("Wrong string as a mention.");
			System.exit(1);
		}
		anchorText = serializable.substring(serializable.indexOf("--> ") + "--> ".length());
		if (serializable.indexOf("; /m/") != -1) {
			freebaseId = serializable.substring(serializable.indexOf("; /m/") + 2, serializable.indexOf(" --> "));
		}
		wikiUrl = serializable.substring(0,serializable.indexOf("; "));
		wikiUrl = Utils.pruneURL(wikiUrl);
	}
	
	public TruthMention(String wikiUrl, String freebaseID, String anchorText, int mentionOffsetInText) {
	    wikiUrl = Utils.pruneURL(wikiUrl);
	    this.wikiUrl = wikiUrl;
	    this.freebaseId = freebaseID;
	    this.anchorText = anchorText;
	    this.mentionOffsetInText = mentionOffsetInText;
	}
	
	public String toString() {
		return wikiUrl + "; " + freebaseId + " --> " + anchorText + " ------- offset= " + mentionOffsetInText;
	}
	
	public TruthMention clone() {
		return new TruthMention(wikiUrl +"; " + freebaseId + " --> " + anchorText);
	}
}
