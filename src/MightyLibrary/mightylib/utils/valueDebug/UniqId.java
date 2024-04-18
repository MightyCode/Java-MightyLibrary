package MightyLibrary.mightylib.utils.valueDebug;

public class UniqId {
    private static int COUNT = 0;
    private final int id;

    public UniqId(){
        id = COUNT++;
    }

    public int id(){
        return id;
    }
}
