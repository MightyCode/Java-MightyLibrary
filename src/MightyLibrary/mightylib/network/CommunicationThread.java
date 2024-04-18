package MightyLibrary.mightylib.network;

import MightyLibrary.mightylib.resources.Resources;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class CommunicationThread extends Thread {
    public static final int SPECIAL_ID = -1;
    private int messageIdCounter;
    protected int processedMessageNumber;
    protected int receivedMessageNumber;

    protected volatile boolean running;
    protected ArrayList<NetworkMessage> receivedMessages;

    protected HashMap<Integer, String> messageToSend;

    protected NetworkConfiguration configuration;


    CommunicationThread(){
        receivedMessages = new ArrayList<>();
        messageToSend = new HashMap<>();
    }

    public void setup(String configurationName){
        configuration = Resources.getInstance().getResource(NetworkConfiguration.class,
                configurationName);

        messageIdCounter = 0;
        processedMessageNumber = 0;
        receivedMessageNumber = 0;
    }

    public void reset(){
        messageIdCounter = 0;
        processedMessageNumber = 0;
        receivedMessageNumber = 0;

        receivedMessages.clear();
        messageToSend.clear();
    }

    public void run() {}
    public boolean isRunning(){ return running; }

    public boolean didReceiveMessage() {
        return receivedMessages.size() != 0;
    }

    public final NetworkMessage[] message() {
        NetworkMessage[] result = new NetworkMessage[receivedMessages.size()];
        receivedMessages.toArray(result);
        receivedMessages.clear();

        return result;

    }

    public final int sendMessage(String message) {
        messageToSend.put(messageIdCounter, message);
        return messageIdCounter++;
    }

    public final boolean didProcessedMessage() {
        return false;
    }

    public abstract void doStop();
}
