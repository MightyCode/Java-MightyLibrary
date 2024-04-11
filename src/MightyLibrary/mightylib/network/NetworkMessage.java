package MightyLibrary.mightylib.network;

public class NetworkMessage {
    private final int id;
    private final String message;

    NetworkMessage (int id, String message){
        this.id = id;
        this.message = message;
    }

    public String getMessage() { return message; }
    public int getId() { return id; }
}