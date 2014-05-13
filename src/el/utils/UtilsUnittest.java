package el.utils;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import edu.stanford.nlp.ling.TaggedWord;
import el.TokenSpan;
import el.context_probs.OverlappingTriplet;


public class UtilsUnittest {
    public static void run() {
        test_Utils_NumTokensUsingStanfordNLP();
        test_Utils_NumDictionaryWords();
        test_Utils_getContext();
        test_getTokenSpans();
        test_getFirstLastPreviousAndNextTokensAndPosTags();
        test_getPosTags();
    }

    
    public static void test_getPosTags() {
        // first test
        
        ArrayList<TaggedWord> v = Utils.getPosTags("I love to go running.");
        assertEquals(v.get(0).word(), "I");
        assertEquals(v.get(0).tag(), "PRP");
        
        assertEquals(v.get(1).word(), "love");        
        assertEquals(v.get(1).tag(), "VBP");
        
        assertEquals(v.get(2).word(), "to");
        assertEquals(v.get(2).tag(), "TO");

        assertEquals(v.get(3).word(), "go");
        assertEquals(v.get(3).tag(), "VB");
        
        assertEquals(v.get(4).word(), "running");
        assertEquals(v.get(4).tag(), "VBG");
        
        assertEquals(v.get(5).word(), ".");
        assertEquals(v.get(5).tag(), ".");

        // second test
        v = Utils.getPosTags("I would like, but it's expensive.");
        assertEquals(v.get(0).word(), "I");
        assertEquals(v.get(0).tag(), "PRP");

        assertEquals(v.get(1).word(), "would");
        assertEquals(v.get(1).tag(), "MD");

        assertEquals(v.get(2).word(), "like");
        assertEquals(v.get(2).tag(), "VB");
        
        assertEquals(v.get(3).word(), ",");
        assertEquals(v.get(3).tag(), ",");
        
        assertEquals(v.get(4).word(), "but");
        assertEquals(v.get(4).tag(), "CC");
        
        assertEquals(v.get(5).word(), "it");
        assertEquals(v.get(5).tag(), "PRP");
        
        assertEquals(v.get(6).word(), "'s");
        assertEquals(v.get(6).tag(), "VBZ");
        
        assertEquals(v.get(7).word(), "expensive");
        assertEquals(v.get(7).tag(), "JJ");
        
        assertEquals(v.get(8).word(), ".");
        assertEquals(v.get(8).tag(), ".");
        
    }
    
    
    public static void test_getFirstLastPreviousAndNextTokensAndPosTags() {
        
        String big = "I love you";
        String small = "love";
        ArrayList<TaggedWord> v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(big.indexOf(small), small));
        assertEquals(v.size(), 4);
        assertEquals(v.get(0).word(), "I");
        assertEquals(v.get(0).tag(), "PRP");
        assertEquals(v.get(1).word(), "you");
        assertEquals(v.get(1).tag(), "PRP");
        assertEquals(v.get(2).word(), small);
        assertEquals(v.get(2).tag(), "VBP");
        assertEquals(v.get(3).word(), small);
        assertEquals(v.get(3).tag(), "VBP");

        
        big = "Cheap, Off Grid Cooling";
        small = "Off";
        v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(big.indexOf(small), small));
        assertEquals(v.size(), 4);
        
        big = "Jamaica's Usain Bolt added the 200m crown to his Olympic 100m title in an incredible new world record time of 19.30 seconds in Beijing.";
        small = "200m";
        v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(big.indexOf(small), small));
        assertEquals(v.size(), 4);
        assertEquals(v.get(0).word(), "the");
        assertEquals(v.get(0).tag(), "DT");
        assertEquals(v.get(1).word(), "crown");
        assertEquals(v.get(1).tag(), "NN");
        assertEquals(v.get(2).word(), small);
        assertEquals(v.get(2).tag(), "JJ");
        assertEquals(v.get(3).word(), small);
        assertEquals(v.get(3).tag(), "JJ");
        
        big = "I love you,but not too much";
        small = "love you";
        v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(big.indexOf(small), small));
        assertEquals(v.size(), 4);
        assertEquals(v.get(0).word(), "I");
        assertEquals(v.get(0).tag(), "PRP");
        assertEquals(v.get(1).word(), ",");
        assertEquals(v.get(1).tag(), ",");
        assertEquals(v.get(2).word(), "love");
        assertEquals(v.get(2).tag(), "VBP");
        assertEquals(v.get(3).word(), "you");
        assertEquals(v.get(3).tag(), "PRP");
        
        big = "I love you,but not too much";
        small = "love you,but not";
        v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(2, small));
        assertEquals(v.size(), 4);
        assertEquals(v.get(0).word(), "I");
        assertEquals(v.get(0).tag(), "PRP");
        assertEquals(v.get(1).word(), "too");
        assertEquals(v.get(1).tag(), "RB");
        assertEquals(v.get(2).word(), "love");
        assertEquals(v.get(2).tag(), "VBP");
        assertEquals(v.get(3).word(), "not");
        assertEquals(v.get(3).tag(), "RB");
        
        big = "I love you";
        small = "I";
        v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(big.indexOf(small), small));
        assertEquals(v.size(), 4);
        assertEquals(v.get(0).word(), ".");
        assertEquals(v.get(0).tag(), ".");
        assertEquals(v.get(1).word(), "love");
        assertEquals(v.get(1).tag(), "VBP");
        assertEquals(v.get(2).word(), small);
        assertEquals(v.get(2).tag(), "PRP");
        assertEquals(v.get(3).word(), small);
        assertEquals(v.get(3).tag(), "PRP");
        
        big = "I love you";
        small = "you";
        v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(big.indexOf(small), small));
        assertEquals(v.size(), 4);
        assertEquals(v.get(0).word(), "love");
        assertEquals(v.get(0).tag(), "VBP");
        assertEquals(v.get(1).word(), ".");
        assertEquals(v.get(1).tag(), ".");
        assertEquals(v.get(2).word(), small);
        assertEquals(v.get(2).tag(), "PRP");
        assertEquals(v.get(3).word(), small);
        assertEquals(v.get(3).tag(), "PRP");

        big = "I love you!";
        small = "you";
        v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(big.indexOf(small), small));
        assertEquals(v.toString(), "[love/VBP, !/., you/PRP, you/PRP]");
        
        big = "Roger Federer(pictured left) is one of the best and most promising male tennis players in the world.";
        small = "Roger Federer";
        v= Utils.getFirstLastPreviousAndNextTokensAndPosTags(big, Utils.getPosTags(big), new TokenSpan(big.indexOf(small), small));
        assertTrue(v.toString().equals("[./., (/NNP, Roger/NNP, Federer/NNP]"));
    }
    
    
    public static void test_one_getTokenSpan(String text, String word, String expected) {
        Vector<TokenSpan> v = Utils.getTokenSpans(text, text.indexOf(word), word.length());
        String serialize = "";
        for (TokenSpan tksp : v) {
            serialize += tksp.name + " ; ";
        }
        assertEquals(serialize, expected);
    }

    public static void test_getTokenSpans() {
        test_one_getTokenSpan("I love my job.", "my", "my ; love my ; my job ; love my job ; ");
        test_one_getTokenSpan("I love my job.", "job", "job ; my job ; ");

    }

    
    public static void test_Utils_NumTokensUsingStanfordNLP() {
        assertEquals(Utils.getTokens("   a  b").size(), 2);
        assertEquals(Utils.getTokens("   a    ").size(), 1);
        assertEquals(Utils.getTokens("antivirus software").size(), 2);
        assertEquals(Utils.getTokens("  ").size(), 0);
        assertEquals(Utils.getTokens("").size(), 0);
        assertEquals(Utils.getTokens("aaaaa").size(), 1);
        assertEquals(Utils.getTokens("   a b c ").size(), 3);
        assertEquals(Utils.getTokens("   a  ,b c d   ").size(), 5);
        assertEquals(Utils.getTokens("   Where's he?").size(), 4);
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

