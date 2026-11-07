package io.github.mightycode.mightylib.network;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;

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
    protected DataType createReloadCopy() {
        return getClass() == NetworkConfiguration.class ? new NetworkConfiguration(getDataName(), path()) : null;
    }

    @Override
    protected void swapContent(DataType copy) {
        NetworkConfiguration other = (NetworkConfiguration) copy;
        String otherAddress = other.address;
        String otherId = other.id;
        int otherPort = other.port;
        other.init(address, id, port);
        init(otherAddress, otherId, otherPort);
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
