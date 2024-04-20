package MightyLibrary.mightylib.utils.math;

public class UUID {
    private static int COUNT = 0;
    private final int id;

    public UUID(){
        id = COUNT++;
    }

    public int id(){
        return id;
    }
}
