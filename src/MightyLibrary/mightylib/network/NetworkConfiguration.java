package MightyLibrary.mightylib.network;

import MightyLibrary.mightylib.resources.DataType;

public class NetworkConfiguration extends DataType {
    private String address;
    private String id;
    private int port;
    private boolean initialized;

    public NetworkConfiguration(String dataName, String path) {
        super(dataName, path);
    }

    public void init(String address, String id, int port){
        if (!initialized){
            this.address = address;
            this.id = id;
            this.port = port;

            initialized = true;
        }
    }

    public String getAddress() { return address; }
    public String getId() { return id; }

    public int getPort() { return port; }

    @Override
    public void unload() {}

}
