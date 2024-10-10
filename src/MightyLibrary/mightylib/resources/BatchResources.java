package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.resources.data.JSONFile;

public class BatchResources extends JSONFile {
    public BatchResources(String dataName, String path) {
        super(TYPE_SET_UP.IMMEDIATELY_BY_MAIN_CONTEXT, dataName, path);
    }
}
