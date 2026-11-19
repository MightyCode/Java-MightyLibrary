package io.github.mightycode.mightylib.resources;

import org.json.JSONObject;

// Puts a data in the loaded state without a Resources: for the tests of the data that only need to be loaded
// to change their behavior (the changes of a loaded map are tracked); reaches the protected hooks of a data
public final class DataTypeTestAccess {
    private DataTypeTestAccess() {
    }

    public static void markLoaded(DataType data) {
        data.loadDone(true);
    }

    public static DataType reloadCopy(DataType data) {
        return data.createReloadCopy();
    }

    public static void describeDetails(DataType data, JSONObject details) {
        data.describeDetails(details);
    }
}
