package io.github.mightycode.mightylib.network;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.utils.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class NetworkConfigurationLoader extends ResourceLoader {
    private static final String CONFIG_FOLDER = "resources/networkConfigs";

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
            System.err.println("ERROR LOAD");
            return;
        }

        char symbol = '=';
        String[] values = {"","","","",""};
        int i = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(((NetworkConfiguration) dataType).path(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();

            while (line != null) {
                if (!line.contains("#") && !line.isEmpty()){
                    int symbolPosition = line.indexOf(symbol);
                    String newline = line.substring(symbolPosition + 2);
                    values[i] = newline;
                    i = i+1;
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            Logger.LogException("Can't read the network configuration " + ((NetworkConfiguration) dataType).path(), e);
        }

        String address = values[0];
        String id = values[1];
        int port = Integer.parseInt(values[2]);

        NetworkConfiguration configuration = (NetworkConfiguration) dataType;
        configuration.init(address, id, port);
    }
}
