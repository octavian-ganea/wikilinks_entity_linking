import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Utils {
	private static HashMap<String,String> wikiRedirects = null;
	
	public static void loadWikiRedirects(String filename) throws IOException {
		wikiRedirects = new HashMap<String,String>();
		
		if (new java.io.File(filename).exists() == false) {
			System.err.println("[FATAL] Wikipedia Redirects file does not exist.");
			System.exit(1);
		}
		
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = in.readLine();
		while (line != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			wikiRedirects.put(st.nextToken(), st.nextToken());
			line = in.readLine();
		}
	}
	
	public static String pruneURL(String s) {
		String url = s.trim();
		if (url.contains("wiki/")) {
			url = url.substring(url.lastIndexOf("wiki/") + "wiki/".length());
		}
		if (url.contains("wikipedia.org")) {
			url = url.substring(url.indexOf("wikipedia.org") + "wikipedia.org".length());
		}
		String final_url = url;
		try {
			final_url = URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		} catch (java.lang.IllegalArgumentException e) {
	    }	
		
		final_url = final_url.replace('_', ' ');
		if (wikiRedirects.containsKey(final_url)) {
			final_url = wikiRedirects.get(final_url);
		}
		
		final_url = final_url.replace(' ', '_');
		StringBuilder sb = new StringBuilder();
		boolean good = true;
		for (char c : final_url.toCharArray()) {
			if (!Character.isSpace(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	// Comparator used for mentions_hashtable.
	static class StringComp implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			// Try to match longer anchor texts first. For example: "University of Oklahoma" will be replaced
			// with its anchor before it will be "Oklahoma".
			if (o1.length() - o2.length() != 0) return o2.length() - o1.length();
			return o2.compareTo(o1);
		}
	}
	
	static 	public boolean isWordSeparator(char c) {
		if (c == ' ' || c == ',' || c == '"' || c == ':' || c == '.' || c == '?' || c == '!' || c == '(' ||
				c == ')' || c == '[' || c == ']' || c == '+' || c == '=' || c == '\'' || c == '`')
			return true;
		return false;
	}
	
	// Tokenization function: decides if a substring of a text is a separate word.
	static public boolean isSeparateWord(String text, int lastIndex, String word) {		
		if (lastIndex > 0 && (!Utils.isWordSeparator(text.charAt(lastIndex - 1)))) {
			return false;
		}
		if (lastIndex + word.length() < text.length() && (!Utils.isWordSeparator(text.charAt(lastIndex + word.length())))) {
			return false;
		}
		// Avoid situations like "Alexander II,"
		if (Utils.isWordSeparator(text.charAt(lastIndex)) || Utils.isWordSeparator(text.charAt(lastIndex+ word.length() - 1))) {
			return false;
		}		
		return true;
	}
}
