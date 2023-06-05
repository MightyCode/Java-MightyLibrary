package MightyLibrary.mightylib.network;

public abstract class ServerThread extends CommunicationThread{
    private ServerTcp server;
    private volatile boolean listening = false;
    private final boolean serverConnected = false;

    public ServerThread() {
        this.running = true;listening = true;
    }

    @Override
    public void setup(String configurationName){
        super.setup(configurationName);
        System.out.println("[Debug] Setting up the server...");
        server = new ServerTcp(this.configuration);
        server.tryCreateConnection();
    }
    public boolean isListening() {
        return listening;
    }
    public void run(String configurationName) {
        this.setup(configurationName);
        //Todo
    }

    public void doStop() {
        this.running = false;
        if (server != null && server.hasClient()) {
            server.closeConnexion();
        }
    }
}
