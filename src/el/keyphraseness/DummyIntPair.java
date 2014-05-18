package el.keyphraseness;


// Class used to store the counters for dummy probabilities P(dummy | name)
public class DummyIntPair {
    
    public String name;
    public int numDocsWithAnchorName = 0;
    public int numDocsWithName = 0;
    
    public DummyIntPair(int x, int y) {
        numDocsWithAnchorName = x;      
        numDocsWithName = y;
    }
    
    public DummyIntPair(String name) {
        this.name = name;
        numDocsWithName = 0;
        numDocsWithAnchorName = 0;      
    }    
    
    public double getScore() {
        return (numDocsWithAnchorName + 0.0) / numDocsWithName;
    }
}