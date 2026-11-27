package io.github.mightycode.mightylib.network;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;

import io.github.mightycode.mightylib.utils.Logger;

import java.util.Map;

public final class NetworkConfigurationLoader extends ResourceLoader {
    // Logical folder, in the sources of the resources
    static final String CONFIG_FOLDER = "networkConfigs";

    @Override
    public Class<? extends DataType> getType() {
        return NetworkConfiguration.class;
    }

    @Override
    public String getResourceNameType() {
        return "configuration";
    }

    @Override
    public void create(Map<String, DataType> data) {
        exploreResourcesFile(data, CONFIG_FOLDER);
    }

    @Override
    public String filterFile(String path) {
        return getFileName(path);
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new NetworkConfiguration(name, currentPath));
    }


    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof NetworkConfiguration)) {
            Logger.Error(Logger.CATEGORY_ENGINE, "NetworkConfigurationLoader can't load a " + dataType.getClass().getSimpleName());
            return;
        }

        char symbol = '=';
        String[] values = {"","","","",""};
        int i = 0;
        // Missing or unreadable: a ResourceException (the data fails)
        String content = FileMethods.readString(resources().sources(), ((NetworkConfiguration) dataType).path());
        for (String line : content.split("\n")) {
            if (!line.contains("#") && !line.isEmpty()){
                int symbolPosition = line.indexOf(symbol);
                String newline = line.substring(symbolPosition + 2);
                values[i] = newline;
                i = i+1;
            }
        }

        String address = values[0];
        String id = values[1];
        int port = Integer.parseInt(values[2]);

        NetworkConfiguration configuration = (NetworkConfiguration) dataType;
        configuration.init(address, id, port);
    }
}
