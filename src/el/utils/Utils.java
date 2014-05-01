package el.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import el.TokenSpan;

public class Utils {
	
	// Comparator used for mentions_hashtable.
	static class StringComp implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			// Try to match longer anchor texts first. For example: "University of Oklahoma" will be replaced
			// with its anchor before it will be "Oklahoma".
			if (o1.length() - o2.length() != 0) {
			    return o2.length() - o1.length();
			}
			return o2.compareTo(o1);
		}
	}
	
	static 	public boolean isWordSeparator(char c) {
		if (Character.isWhitespace(c) || Character.isSpaceChar(c) || c == ' ' || c == ',' || c == '"' || c == ':' || c == '.' || c == '-' ||  c == '?' || c == '!' || c == '(' ||
				c == ')' || c == '[' || c == ']' || c == '+' || c == '*' || c == '=' || c == '\'' || c == '`' || c == '\n' ||
				c == '\r' || c == ';' || c =='#') {
		    return true;
		}
		return false;
	}
	
	static public int NumTokens(String s) {
	    int i = 0;
	    while (i < s.length() && isWordSeparator(s.charAt(i))) {
	        i++;
	    }

	    if (i == s.length()) return 0;
	    
        while (i < s.length() && !isWordSeparator(s.charAt(i))) {
            i++;
        }

        while (i < s.length() && isWordSeparator(s.charAt(i))) {
            i++;
        }

        if (i == s.length()) return 1;


        while (i < s.length() && !isWordSeparator(s.charAt(i))) {
            i++;
        }

        while (i < s.length() && isWordSeparator(s.charAt(i))) {
            i++;
        }
        if (i == s.length()) return 2;

        return 3;
	}
	
	// Returns all sub-token spans of t=n-,n,n+ that contain n.
	// n is the token starting at offset from text.
	static public Vector<TokenSpan> getTokenSpans(String text, int offset, int length) {
		Vector<TokenSpan> spans = new Vector<TokenSpan>();
		
		if (offset < 0 || offset >= text.length()) {
		    return spans;
		}
		int startN = offset;
		int endN = offset + length - 1;
		
		// Start index of n-
		int i = startN - 1;
		while (i >= 0 && isWordSeparator(text.charAt(i))) {
		    i--;
		}

        int startMinusN = startN;
		if (i >= 0) {
		    while (i >= 0 && !isWordSeparator(text.charAt(i))) {
	            i--;
	        }
		    startMinusN = i+1;
		}
		
	    // End index of the last char of n+
        i = endN + 1;
        while (i < text.length() && isWordSeparator(text.charAt(i))) {
            i++;
        }

        int endPlusN = endN;
        if (i < text.length()) {
            while (i < text.length() && !isWordSeparator(text.charAt(i))) {
                i++;
            }
            endPlusN = i-1;
        }
		
		// n
		spans.add(new TokenSpan(startN, endN+1));
		if (startMinusN < startN) {
			// n-,n
			spans.add(new TokenSpan(startMinusN, endN+1));			
		}
		if (endPlusN > endN) {
			// n,n+
			spans.add(new TokenSpan(startN, endPlusN+1));			
		}
		if (endPlusN > endN && startMinusN < startN) {
			// n-,n,n+
			spans.add(new TokenSpan(startMinusN, endPlusN+1));			
		}
		
		return spans;
	}
	
	
    // Compute context = n1 U n2;
    // return it only if it contains at most two words besides n1 and at most two words besides n2
    public static String getContext(String n1, String n2) {
        String rez = getContext_(n1,n2, 1);
        if (rez.equals("")) {
            return getContext_(n2,n1, 1);
        }
        return rez;
    }

    private static String getContext_(String n1, String n2, int numNeighTokens) {
        if (n2.contains(n1)) {
            int start =  n2.indexOf(n1), end = start + n1.length();
            if ( (start == 0 || Utils.isWordSeparator(n2.charAt(start-1)) ) && 
                    (end == n2.length() || Utils.isWordSeparator(n2.charAt(end)) ) &&
                    Utils.NumTokens(n2.substring(0,start)) <= numNeighTokens &&
                    Utils.NumTokens(n2.substring(end)) <= numNeighTokens) {
                return n2;
            }
        }

        for (int i = 0; i < n1.length(); ++i) {
            if (( ( (n1.length() - i < n2.length()) && Utils.isWordSeparator(n2.charAt(n1.length() - i))) 
                    || n1.length() - i == n2.length())
                    && n2.startsWith(n1.substring(i))
                    && (i == 0 || Utils.isWordSeparator(n1.charAt(i-1)) ) ) {
                if (Utils.NumTokens(n1.substring(0,i)) <= numNeighTokens &&
                        Utils.NumTokens(n2.substring(n1.length() - i)) <= numNeighTokens) {
                    return n1.substring(0, i) + n2;
                }
            }
        }
        return "";
    }
    
    
    public static void WriteIITBGroundTruthFileInXMLFormat() throws IOException {
        System.out.println("<iitb.CSAW.entityAnnotations>");
        BufferedReader in = new BufferedReader(new FileReader("iitb_foundbyme0_0001_final"));
        String line = in.readLine();
        while (line != null) {
            if (line.contains("DOC: ")) {
                System.out.println("<annotation>");
                System.out.println("\t<docName>" + line.substring(line.indexOf("DOC: ") + "DOC: ".length()) + "</docName>");
                System.out.println("\t<userId>ganeao@inf.ethz.ch</userId>");
            }
            if (line.contains("NAME: ")) {
                int nameLen = line.substring(line.indexOf("NAME: ") + "NAME: ".length()).length();
                line = in.readLine();
                
                int offset = Integer.parseInt(line.substring(line.indexOf("OFFSET: ") + "OFFSET: ".length()));
                line = in.readLine();
                
                String url = line.substring(line.indexOf("URL: ") + "URL: ".length());

                System.out.println("\t<wikiName>" + url.substring(url.indexOf("en.wikipedia.org/wiki/") + "en.wikipedia.org/wiki/".length()).replace('_', ' ') + "</wikiName>");
                System.out.println("\t<offset>" + offset + "</offset>");
                System.out.println("\t<length>" + nameLen + "</length>");
                System.out.println("</annotation>");
            }
            
            line = in.readLine();
        }
        in.close();
        
        System.out.println("</iitb.CSAW.entityAnnotations>");
        System.exit(1);
    }
}
