package io.github.mightycode.mightylib.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Hot reload: the configuration stays usable while its new version is read apart
class NetworkConfigurationTest {
    @Test
    void reloadCopyExchangesTheContent() {
        NetworkConfiguration configuration = new NetworkConfiguration("server", "config/server");
        configuration.init("127.0.0.1", "old", 1000);

        NetworkConfiguration copy = (NetworkConfiguration) configuration.createReloadCopy();
        assertNotNull(copy);
        assertEquals("server", copy.getDataName());
        assertEquals("config/server", copy.path());
        copy.init("10.0.0.1", "new", 2000);

        configuration.swapContent(copy);
        assertEquals("10.0.0.1", configuration.getAddress());
        assertEquals("new", configuration.getId());
        assertEquals(2000, configuration.getPort());
        assertEquals("old", copy.getId(), "the copy gets the previous version, unloaded with it");
    }
}
