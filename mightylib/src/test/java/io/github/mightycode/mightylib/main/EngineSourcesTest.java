package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.Registrations;
import io.github.mightycode.mightylib.resources.source.ClasspathSource;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.IResourceSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The source of the engine: its classpath prefix, or a folder of development (-Dmightylib.resources.engine)
class EngineSourcesTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(EngineConfig.ENGINE_RESOURCES_PROPERTY);
    }

    private static List<IResourceSource> engineSources(EngineConfig config) {
        Registrations registrations = EngineRegistry.Collect(new EnginePlugin(config, () -> null, false), List.of());
        List<IResourceSource> sources = registrations.sources("mightylib");
        ResourceSources stack = registrations.resourceSources(new FolderSource("resources"));
        // Below the folder of the game
        assertEquals(ResourceSources.GAME_OWNER, stack.owner(stack.sources().get(1)));
        return sources;
    }

    @Test
    void classpathByDefault() {
        List<IResourceSource> sources = engineSources(new EngineConfig());

        assertEquals(1, sources.size());
        assertInstanceOf(ClasspathSource.class, sources.get(0));
        assertEquals("classpath:assets/mightylib/", sources.get(0).name());
    }

    @Test
    void folderOfDevelopmentReplacesTheClasspath() {
        List<IResourceSource> configured = engineSources(new EngineConfig().setEngineResourcesFolder("dev/engine"));
        assertEquals("folder:dev/engine/", configured.get(0).name());

        System.setProperty(EngineConfig.ENGINE_RESOURCES_PROPERTY, "from/property");
        List<IResourceSource> property = engineSources(new EngineConfig().setEngineResourcesFolder("dev/engine"));
        assertEquals(1, property.size());
        assertInstanceOf(FolderSource.class, property.get(0));
        assertEquals("folder:from/property/", property.get(0).name());
    }
}
