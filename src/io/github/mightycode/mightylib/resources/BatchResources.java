package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.data.JSONFile;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = INTERNAL)
public class BatchResources extends JSONFile {
    public BatchResources(String dataName, String path) {
        super(TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT, dataName, path);
    }

    @Override
    protected DataType createReloadCopy() {
        return new BatchResources(getDataName(), path());
    }
}
