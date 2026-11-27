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

    private static NetworkConfiguration read(Path root, String content) throws IOException {
        Path folder = Files.createDirectories(root.resolve(NetworkConfigurationLoader.CONFIG_FOLDER));
        Files.writeString(folder.resolve("server.cfg"), content);
        NetworkConfiguration configuration = new NetworkConfiguration("server", "networkConfigs/server.cfg");
        LoaderTestAccess.attached(new NetworkConfigurationLoader(), root).initWithFile(configuration);
        return configuration;
    }

    // Bug: a malformed file gave a raw ArrayIndexOutOfBounds or NumberFormatException
    private static void assertRefused(Path root, String content, String line) {
        ResourceException error = assertThrows(ResourceException.class, () -> read(root, content));
        assertTrue(error.getMessage().contains("server.cfg"), error.getMessage());
        assertTrue(error.getMessage().contains("(line " + line + ")"), error.getMessage());
    }

    @Test
    void spacesAndWindowsLineEndingsAreAccepted(@TempDir Path root) throws IOException {
        NetworkConfiguration configuration = read(root, "# server\r\naddress=localhost\r\n\r\nid =  a b \r\nport = 7000\r\n");
        assertEquals("localhost", configuration.getAddress());
        assertEquals("a b", configuration.getId());
        assertEquals(7000, configuration.getPort());
    }

    @Test
    void moreThanFiveValuesAreRefused(@TempDir Path root) {
        assertRefused(root, "a = 1\nb = 2\nc = 3\nd = 4\ne = 5\nf = 6\n", "6");
    }

    @Test
    void aLineWithoutEqualsIsRefused(@TempDir Path root) {
        assertRefused(root, "address = localhost\nid test\nport = 1\n", "2");
    }

    @Test
    void aMissingPortIsRefused(@TempDir Path root) {
        assertRefused(root, "address = localhost\nid = test\n", "3");
    }

    @Test
    void anEmptyPortIsRefused(@TempDir Path root) {
        assertRefused(root, "address = localhost\nid = test\nport =\n", "3");
    }

    @Test
    void aPortThatIsNotANumberIsRefused(@TempDir Path root) {
        assertRefused(root, "address = localhost\nid = test\n# the port\nport = http\n", "4");
    }

    @Test
    void aPortOutOfRangeIsRefused(@TempDir Path root) {
        assertRefused(root, "address = localhost\nid = test\nport = 70000\n", "3");
        assertRefused(root, "address = localhost\nid = test\nport = -1\n", "3");
    }

    @Test
    void aMissingFileIsAResourceFailure(@TempDir Path folder) {
        NetworkConfigurationLoader loader = LoaderTestAccess.attached(new NetworkConfigurationLoader(), folder);
        assertThrows(ResourceException.class,
                () -> loader.initWithFile(new NetworkConfiguration("missing", "networkConfigs/missing.cfg")));
    }
}
