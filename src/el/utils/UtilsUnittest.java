package el.utils;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Vector;

import el.TokenSpan;
import el.context_probs.OverlappingTriplet;


public class UtilsUnittest {
    public static void run() {
        test_Utils_NumTokensUsingStanfordNLP();
        test_Utils_NumDictionaryWords();
        test_Utils_getContext();
        test_getTokenSpans();
        test_getPreviousAndNextTokensUsingStanfordNLP();
        test_getPosTags();
    }

    
    public static void test_getPosTags() {
        // first test
        Vector<String> v = Utils.getPosTags("I love to go running.");
        assertEquals(v.get(0), "I");
        assertEquals(v.get(1), "PRP");
        
        assertEquals(v.get(2), "love");        
        assertEquals(v.get(3), "VBP");
        
        assertEquals(v.get(4), "to");
        assertEquals(v.get(5), "TO");

        assertEquals(v.get(6), "go");
        assertEquals(v.get(7), "VB");
        
        assertEquals(v.get(8), "running");
        assertEquals(v.get(9), "VBG");
        
        assertEquals(v.get(10), ".");
        assertEquals(v.get(11), ".");

        // second test
        v = Utils.getPosTags("I would like, but it's expensive.");
        assertEquals(v.get(0), "I");
        assertEquals(v.get(1), "PRP");

        assertEquals(v.get(2), "would");
        assertEquals(v.get(3), "MD");

        assertEquals(v.get(4), "like");
        assertEquals(v.get(5), "VB");
        
        assertEquals(v.get(6), ",");
        assertEquals(v.get(7), ",");
        
        assertEquals(v.get(8), "but");
        assertEquals(v.get(9), "CC");
        
        assertEquals(v.get(10), "it");
        assertEquals(v.get(11), "PRP");
        
        assertEquals(v.get(12), "'s");
        assertEquals(v.get(13), "VBZ");
        
        assertEquals(v.get(14), "expensive");
        assertEquals(v.get(15), "JJ");
        
        assertEquals(v.get(16), ".");
        assertEquals(v.get(17), ".");
    }
    
    
    public static void test_getPreviousAndNextTokensUsingStanfordNLP() {
        Vector<String> v= Utils.getPreviousAndNextTokensAndPosTags("I love you", "love", "love you");
        assertEquals(v.size(), 4);
        assertEquals(v.get(0), "I");
        assertEquals(v.get(1), "PRP");
        assertEquals(v.get(2), "you");
        assertEquals(v.get(3), "PRP");

        v = Utils.getPreviousAndNextTokensAndPosTags("I love you,but not too much", "you", "you,but not too much");
        assertEquals(v.size(), 4);
        assertEquals(v.get(0), "love");
        assertEquals(v.get(1), "VBP");
        assertEquals(v.get(2), ",");
        assertEquals(v.get(3), ",");
    
        v = Utils.getPreviousAndNextTokensAndPosTags("I love you,but not too much", "love   you", "love you,but not too much");
        assertEquals(v.size(), 4);
        assertEquals(v.get(0), "I");
        assertEquals(v.get(1), "PRP");
        assertEquals(v.get(2), ",");
        assertEquals(v.get(3), ",");
        
        v = Utils.getPreviousAndNextTokensAndPosTags("I love you,but not too much", "love you  ,   but not", "love you,but not too much");
        assertEquals(v.size(), 4);
        assertEquals(v.get(0), "I");
        assertEquals(v.get(1), "PRP");
        assertEquals(v.get(2), "too");
        assertEquals(v.get(3), "RB");
        
        v= Utils.getPreviousAndNextTokensAndPosTags("I love you", "I", "I love you");
        assertEquals(v.size(), 4);
        assertEquals(v.get(0), ".");
        assertEquals(v.get(1), ".");
        assertEquals(v.get(2), "love");
        assertEquals(v.get(3), "VBP");
        
        v= Utils.getPreviousAndNextTokensAndPosTags("I love you", "you", "you");
        assertEquals(v.size(), 4);
        assertEquals(v.get(0), "love");
        assertEquals(v.get(1), "VBP");
        assertEquals(v.get(2), ".");
        assertEquals(v.get(3), ".");

        v= Utils.getPreviousAndNextTokensAndPosTags("I love you!", "you", "you !");
        assertEquals(v.size(), 4);
        assertEquals(v.get(0), "love");
        assertEquals(v.get(1), "VBP");
        assertEquals(v.get(2), "!");
        assertEquals(v.get(3), ".");

    }
    
    
    public static void test_one_getTokenSpan(String text, String word, String expected) {
        Vector<TokenSpan> v = Utils.getTokenSpans(text, text.indexOf(word), word.length());
        String serialize = "";
        for (TokenSpan tksp : v) {
            serialize += text.substring(tksp.start, tksp.end) + " ; ";
        }
        assertEquals(serialize, expected);
    }

    public static void test_getTokenSpans() {
        test_one_getTokenSpan("I love my job.", "my", "my ; love my ; my job ; love my job ; ");
        test_one_getTokenSpan("I love my job.", "job", "job ; my job ; ");

    }

    
    public static void test_Utils_NumTokensUsingStanfordNLP() {
        assertEquals(Utils.numTokensUsingStanfordNLP("   a  b"), 2);
        assertEquals(Utils.numTokensUsingStanfordNLP("   a    "), 1);
        assertEquals(Utils.numTokensUsingStanfordNLP("antivirus software"), 2);
        assertEquals(Utils.numTokensUsingStanfordNLP("  "), 0);
        assertEquals(Utils.numTokensUsingStanfordNLP(""), 0);
        assertEquals(Utils.numTokensUsingStanfordNLP("aaaaa"), 1);
        assertEquals(Utils.numTokensUsingStanfordNLP("   a b c "), 3);
        assertEquals(Utils.numTokensUsingStanfordNLP("   a  ,b c d   "), 5);
        assertEquals(Utils.numTokensUsingStanfordNLP("   Where's he?"), 4);
    }
    
    public static void test_Utils_NumDictionaryWords() {
        assertEquals(Utils.numDictionaryWords("   a    ,.;:b"), 2);
        assertEquals(Utils.numDictionaryWords("   a    ,.;:"), 1);
        assertEquals(Utils.numDictionaryWords("antivirus software"), 2);
        assertEquals(Utils.numDictionaryWords("  "), 0);
        assertEquals(Utils.numDictionaryWords(""), 0);
        assertEquals(Utils.numDictionaryWords("aaaaa"), 1);
        assertEquals(Utils.numDictionaryWords("   a    ,.;:b c "), 3);
        assertEquals(Utils.numDictionaryWords("   a    ,.;:b c d   "), 4);

    }
    
    public static void test_Utils_getContext() {
        assertEquals(Utils.getContext("Wikapedia - diabetes", "diabetes"), "Wikapedia - diabetes");

        assertEquals(Utils.getContext("1", "18q21"), "");
        assertEquals(Utils.getContext("18q21", "1"), "");

        assertEquals(Utils.getContext("a b c d", "d e f"), ""); // More than 2 tokens
        assertEquals(Utils.getContext("   ;'b d", "d e "), "   ;'b d e ");
        assertEquals(Utils.getContext("a b c ", "d e f"), "");
        assertEquals(Utils.getContext("a b c", "d e f"), "");

        assertEquals(Utils.getContext("antivirus", "antivirus"), "antivirus");
        assertEquals(Utils.getContext("antivirus software", "antivirus"), "antivirus software");
        assertEquals(Utils.getContext("antivirus software", "software"), "antivirus software");
        assertEquals(Utils.getContext("antivirus", "antivirus software"), "antivirus software");
        assertEquals(Utils.getContext("software", "antivirus software"), "antivirus software");

        assertEquals(Utils.getContext("n1 n2", "n2  n3"), "n1 n2  n3");
        assertEquals(Utils.getContext("aaaaaaaaa b", "bbbbbbb c"), "");
        assertEquals(Utils.getContext("aaaaaaaaa bbb", "bbb c"), "aaaaaaaaa bbb c");

        assertEquals(Utils.getContext("...Wikipedia \"Homer\"", "Homer"), "...Wikipedia \"Homer\"");
        assertEquals(Utils.getContext(" c a d", "a"), " c a d");
        assertEquals(Utils.getContext("a", " a "), " a ");
        assertEquals(Utils.getContext("d b c a d eee f g h ", "a"), "");

    }
}

