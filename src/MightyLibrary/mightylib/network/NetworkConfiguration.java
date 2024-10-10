package MightyLibrary.mightylib.network;

import MightyLibrary.mightylib.resources.SingleSourceDataType;

public class NetworkConfiguration extends SingleSourceDataType {
    private String address;
    private String id;
    private int port;

    public NetworkConfiguration(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);
    }

    public void init(String address, String id, int port) {
        this.address = address;
        this.id = id;
        this.port = port;
    }

    @Override
    protected boolean internLoad() {
        return true;
    }

    public String getAddress() { return address; }
    public String getId() { return id; }

    public int getPort() { return port; }

    @Override
    public void internUnload() {}

}
