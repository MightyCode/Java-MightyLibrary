package io.github.mightycode.mightylib.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Creates the given FakeData instead of exploring files
public class FakeLoader extends ResourceLoader {
    private final List<FakeData> toCreate = new ArrayList<>();
    // Given by createFallback when set
    FakeData fallback;

    FakeLoader add(FakeData data) {
        toCreate.add(data);
        return this;
    }

    @Override
    public Class<? extends DataType> getType() {
        return FakeData.class;
    }

    @Override
    public String getResourceNameType() {
        return "Fake";
    }

    @Override
    public void create(Map<String, DataType> data) {
        for (FakeData fake : toCreate)
            data.put(fake.getDataName(), fake);
    }

    @Override
    public DataType createFallback() {
        return fallback;
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
    }

    @Override
    public String filterFile(String path) {
        return null;
    }

    @Override
    protected void initWithFile(DataType dataType) {
        ((FakeData) dataType).preloadStep();
    }
}
