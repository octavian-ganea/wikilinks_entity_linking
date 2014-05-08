package el.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TTags;
import el.TokenSpan;

public class Utils {
	static public MaxentTagger tagger = null;
	
    // Returns a set of PoS tags for a given input text.    
    // v[2*i] = i-th word; v[2*i+1] = i-th tag 
    public static Vector<String> getPosTags(String s) {
        if (tagger == null) {
            Properties props = new Properties();
            props.setProperty("outputFormat", "tsv");
            tagger = new MaxentTagger("lib/stanford-postagger-2014-01-04/models/english-bidirectional-distsim.tagger",
                    props);
        }
        Vector<String> v = new Vector<String>();
        StringTokenizer tags = new StringTokenizer(tagger.tagString(s), "\n");
        
        HashMap<String,String> rez = new HashMap<String,String>();
        while (tags.hasMoreTokens()) {
            StringTokenizer st = new StringTokenizer(tags.nextToken(), "\t");
            v.add(st.nextToken());
            v.add(st.nextToken());
        }
        
        return v;
    }
    
    // Maybe someone using the PoS tagger will want to use tagger.tokenizeText(r) instead of this 
    static public Vector<String> getTokens(String s) {
        Vector<String> v = new Vector<String>();
        PTBTokenizer ptbt = new PTBTokenizer(
                new StringReader(s), new CoreLabelTokenFactory(), "ptb3Escaping=false");
        List<CoreLabel> tokens = ptbt.tokenize();
        for (CoreLabel cl : tokens) {
            v.add(cl.word());
        }
        return v;
    }
    
    static public int numTokensUsingStanfordNLP(String s) {
        return getTokens(s).size();
    }
    
    
    // smallStringToEnd is needed because there might be multiple names within the same sentence
    static public Vector<String> getPreviousAndNextTokensAndPosTags(String context, String smallString, String smallStringToEnd) {
        // Add a dot at the beginning of the context to avoid having empty results.
        Vector<String> contextTags = getPosTags("." + context + ".");
        Vector<String> smallStringTags = getPosTags(smallString);
        Vector<String> smallStringToEndTags = getPosTags(smallStringToEnd + ".");
        
       
        int index = contextTags.size() - smallStringToEndTags.size();
        if (index <= 0) {
            System.err.println("[FATAL] getPreviousAndNextTokensUsingStanfordNLP fatal error. context = " + context + " ; small string = " + smallString +
                    " ; small string to the end = " + smallStringToEnd  + " " + index);
            System.exit(1);
        }

        for (int j = 0; j < smallStringTags.size(); j += 2) {
            if (!smallStringTags.get(j).equals(contextTags.get(index + j))) {
                index = -1;
                break;
            }
        }
        if (index <= 0) {
            System.err.println("[FATAL] getPreviousAndNextTokensUsingStanfordNLP fatal error. context = " + context + " ; small string = " + smallString +
                    " ; small string to the end = " + smallStringToEnd  + " " + index);
            System.exit(1);
        }
        for (int j = 0; j < smallStringToEndTags.size(); j += 2) {
            if (!smallStringToEndTags.get(j).equals(contextTags.get(index + j))) {
                index = -1;
                break;
            }
        }
        if (index <= 0) {
            System.err.println("[FATAL] getPreviousAndNextTokensUsingStanfordNLP fatal error. context = " + context + " ; small string = " + smallString +
                    " ; small string to the end = " + smallStringToEnd  + " " + index);
            System.exit(1);
        }
        
        Vector<String> rez = new Vector<String>();
        rez.add(contextTags.get(index - 2));
        rez.add(contextTags.get(index - 1));
        rez.add(contextTags.get(index + smallStringTags.size()));
        rez.add(contextTags.get(index + smallStringTags.size() + 1));
        return rez;
    }
    
    
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
	
	static public int numDictionaryWords(String s) {
	    int i = 0;
	    while (i < s.length() && isWordSeparator(s.charAt(i))) {
	        i++;
	    }
	    if (i == s.length()) return 0;
	    
	    int nrToks = 0;
        while (i < s.length()) {
            nrToks++;
            while (i < s.length() && !isWordSeparator(s.charAt(i))) {
                i++;
            }

            while (i < s.length() && isWordSeparator(s.charAt(i))) {
                i++;
            }
        }
        return nrToks;
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
                    Utils.numDictionaryWords(n2.substring(0,start)) <= numNeighTokens &&
                    Utils.numDictionaryWords(n2.substring(end)) <= numNeighTokens) {
                return n2;
            }
        }

        for (int i = 0; i < n1.length(); ++i) {
            if (( ( (n1.length() - i < n2.length()) && Utils.isWordSeparator(n2.charAt(n1.length() - i))) 
                    || n1.length() - i == n2.length())
                    && n2.startsWith(n1.substring(i))
                    && (i == 0 || Utils.isWordSeparator(n1.charAt(i-1)) ) ) {
                if (Utils.numDictionaryWords(n1.substring(0,i)) <= numNeighTokens &&
                        Utils.numDictionaryWords(n2.substring(n1.length() - i)) <= numNeighTokens) {
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
