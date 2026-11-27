package io.github.mightycode.mightylib.network;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.TextFormat;

import io.github.mightycode.mightylib.utils.Logger;

import java.util.Map;

public final class NetworkConfigurationLoader extends ResourceLoader {
    // Logical folder, in the sources of the resources
    static final String CONFIG_FOLDER = "networkConfigs";

    private static final int ADDRESS = 0;
    private static final int ID = 1;
    private static final int PORT = 2;
    private static final int VALUES_READ = 3;
    // Room for two more values, as the format always allowed
    private static final int MAX_VALUES = 5;
    private static final int MAX_PORT = 65535;

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

        NetworkConfiguration configuration = (NetworkConfiguration) dataType;
        String path = configuration.path();
        // Missing or unreadable: a ResourceException (the data fails)
        String content = FileMethods.readString(resources().sources(), path);

        // Useful lines "name = value", in order: address, id, port (comments and blank lines skipped);
        // a malformed file is a ResourceException naming the file and the line
        String[] values = new String[MAX_VALUES];
        int[] lines = new int[MAX_VALUES];
        int count = 0;
        String[] fileLines = content.split("\n", -1);
        for (int index = 0; index < fileLines.length; ++index) {
            String line = fileLines[index].trim();
            if (line.contains("#") || line.isEmpty())
                continue;

            int lineNumber = index + 1;
            if (count == MAX_VALUES)
                throw TextFormat.error(path, lineNumber, "at most " + MAX_VALUES + " values, got another: '" + line + "'");
            int symbolPosition = line.indexOf('=');
            if (symbolPosition < 0)
                throw TextFormat.error(path, lineNumber, "expected 'name = value', got '" + line + "'");

            values[count] = line.substring(symbolPosition + 1).trim();
            lines[count] = lineNumber;
            ++count;
        }

        if (count < VALUES_READ)
            throw TextFormat.error(path, fileLines.length, "expected the address, the id and the port, found "
                    + count + " value(s)");

        String address = TextFormat.word(values, ADDRESS, path, lines[ADDRESS], "address");
        String id = TextFormat.word(values, ID, path, lines[ID], "id");
        int port = TextFormat.integer(values, PORT, path, lines[PORT], "port");
        if (port < 0 || port > MAX_PORT)
            throw TextFormat.error(path, lines[PORT], "port must be between 0 and " + MAX_PORT + ", got " + port);

        configuration.init(address, id, port);
    }
}
