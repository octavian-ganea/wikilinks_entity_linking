package el.utils;

public class PairOfInts {
    public int x;
    public int y;
    public PairOfInts(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

}
