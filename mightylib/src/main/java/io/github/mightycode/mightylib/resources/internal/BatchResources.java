package io.github.mightycode.mightylib.resources.internal;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.data.JSONFile;

public final class BatchResources extends JSONFile {
    public BatchResources(String dataName, String path) {
        super(TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT, dataName, path);
    }

    @Override
    protected DataType createReloadCopy() {
        return new BatchResources(getDataName(), path());
    }
}
