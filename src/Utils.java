import java.util.Comparator;

public class Utils {
	
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
	static public boolean isSeparateWord(String text, int lastIndex, String anchor) {		
		if (lastIndex > 0 && (!Utils.isWordSeparator(text.charAt(lastIndex - 1)))) {
			return false;
		}
		if (lastIndex + anchor.length() < text.length() && (!Utils.isWordSeparator(text.charAt(lastIndex + anchor.length())))) {
			return false;
		}
		// Avoid situations like "Alexander II,"
		if (Utils.isWordSeparator(text.charAt(lastIndex)) || Utils.isWordSeparator(text.charAt(lastIndex+ anchor.length() - 1))) {
			return false;
		}		
		return true;
	}
}
