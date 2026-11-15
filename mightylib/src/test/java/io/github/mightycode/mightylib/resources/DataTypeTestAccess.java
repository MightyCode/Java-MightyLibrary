package io.github.mightycode.mightylib.resources;

// Puts a data in the loaded state without a Resources: for the tests of the data that only need to be loaded
// to change their behaviour (the changes of a loaded map are tracked)
public final class DataTypeTestAccess {
    private DataTypeTestAccess() {
    }

    public static void markLoaded(DataType data) {
        data.loadDone(true);
    }
}
