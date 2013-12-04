
public class InvertedIndexEntry {
	String anchor;
	String url;
	double cprob;
	public InvertedIndexEntry(String anchor, String url, double cprob) {
		this.anchor = anchor;
		this.url = url;
		this.cprob = cprob;
	}
	
	public int hashCode() {
		return (anchor+";;" +url + ";;"  + cprob).hashCode();
	}
}
