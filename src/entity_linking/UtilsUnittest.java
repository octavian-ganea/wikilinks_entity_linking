package entity_linking;
import static org.junit.Assert.*;
import entity_linking.context_probs.OverlappingTriplet;


public class UtilsUnittest {
    public static void run() {
        test_Utils_NumTokens();
        test_Utils_getContext();
    }

    public static void test_Utils_NumTokens() {
        assertEquals(Utils.NumTokens("   a    ,.;:b"), 2);
        assertEquals(Utils.NumTokens("   a    ,.;:"), 1);
        assertEquals(Utils.NumTokens("antivirus software"), 2);
        assertEquals(Utils.NumTokens("  "), 0);
        assertEquals(Utils.NumTokens(""), 0);
        assertEquals(Utils.NumTokens("aaaaa"), 1);
        assertEquals(Utils.NumTokens("   a    ,.;:b c "), 3);
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

