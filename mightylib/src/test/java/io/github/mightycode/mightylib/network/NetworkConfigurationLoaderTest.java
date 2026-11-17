package io.github.mightycode.mightylib.network;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.LoaderTestAccess;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// The network configurations: found in networkConfigs/ of every source, read in the highest one
class NetworkConfigurationLoaderTest {
    private static void config(Path root, String name, String address, int port) throws IOException {
        Path folder = Files.createDirectories(root.resolve(NetworkConfigurationLoader.CONFIG_FOLDER));
        Files.writeString(folder.resolve(name), "# server\naddress = " + address + "\nid = test\nport = " + port + "\n");
    }

    @Test
    void foundInEverySourceAndReadInTheHighest(@TempDir Path low, @TempDir Path high) throws IOException {
        config(low, "server.cfg", "low.example", 1000);
        config(high, "server.cfg", "high.example", 2000);
        config(low, "local.cfg", "localhost", 3000);

        NetworkConfigurationLoader loader = LoaderTestAccess.attached(new NetworkConfigurationLoader(),
                ResourceSources.of(new FolderSource(low), new FolderSource(high)));
        Map<String, DataType> data = new HashMap<>();
        loader.create(data);

        assertEquals(Set.of("server", "local"), data.keySet());
        NetworkConfiguration server = (NetworkConfiguration) data.get("server");
        assertEquals("networkConfigs/server.cfg", server.path());

        loader.initWithFile(server);
        assertEquals("high.example", server.getAddress());
        assertEquals(2000, server.getPort());
    }

    @Test
    void aMissingFileIsAResourceFailure(@TempDir Path folder) {
        NetworkConfigurationLoader loader = LoaderTestAccess.attached(new NetworkConfigurationLoader(), folder);
        assertThrows(ResourceException.class,
                () -> loader.initWithFile(new NetworkConfiguration("missing", "networkConfigs/missing.cfg")));
    }
}
