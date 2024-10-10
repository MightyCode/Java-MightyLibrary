package MightyLibrary.mightylib.resources.models;

import MightyLibrary.mightylib.resources.SingleSourceDataType;

public class Model extends SingleSourceDataType {
    public Model(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);
    }

    @Override
    public boolean internLoad() {
        return false;
    }

    @Override
    public void internUnload() {

    }
}
