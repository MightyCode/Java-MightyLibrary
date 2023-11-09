package MightyLibrary.mightylib.network;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.sound.SoundData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class NetworkConfigurationLoader extends ResourceLoader {
    private static final String CONFIG_FOLDER = "resources/networkConfigs";

    @Override
    public Class<?> getType() {
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
    public void load(DataType dataType) {
        if (!(dataType instanceof NetworkConfiguration)) {
            System.err.println("ERROR LOAD");
            return;
        }

        char symbol = '=';
        BufferedReader reader;
        String[] values = {"","","","",""};
        int i = 0;
        try {
            reader = new BufferedReader(new FileReader(dataType.getPath()));
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

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String address = values[0];
        String id = values[1];
        int port = Integer.parseInt(values[2]);

        NetworkConfiguration configuration = (NetworkConfiguration) dataType;
        configuration.init(address, id, port);
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        // Todo
    }
}
