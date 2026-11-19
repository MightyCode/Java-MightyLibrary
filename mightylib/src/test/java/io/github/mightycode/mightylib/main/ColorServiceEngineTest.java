package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.color.ColorService;
import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.color.IColorService;
import io.github.mightycode.mightylib.graphics.color.PictureSettings;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.services.EngineServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

// The color service of an engine without window: created from the configuration, or replaced by a plugin
public class ColorServiceEngineTest {
    private Engine engine;
    private Views.Scope scope;

    @AfterEach
    void stop() {
        if (engine != null)
            assertEquals(ListError.NO_ERROR, engine.shutdown(ListError.NO_ERROR));
        if (scope != null)
            scope.close();
    }

    private Engine start(Path resources, EngineConfig config) {
        System.setProperty(EngineConfig.SOUND_PROPERTY, "silent");
        try {
            engine = new Engine(config.setResourcesFolder(resources.toString()));
            scope = Views.enter(engine);
            assertEquals(ListError.NO_ERROR, engine.setupHeadless(new HeadlessEngineTest.RecordingSink()));
            return engine;
        } finally {
            System.clearProperty(EngineConfig.SOUND_PROPERTY);
        }
    }

    // An engine without window, entered as the current view, for the other tests
    public static void WithHeadlessEngine(Path resources, Consumer<Engine> test) {
        ColorServiceEngineTest owner = new ColorServiceEngineTest();
        try {
            test.accept(owner.start(resources, new EngineConfig()));
        } finally {
            owner.stop();
        }
    }

    @Test
    void createdWithTheModeOfTheConfiguration(@TempDir Path resources) {
        Engine started = start(resources, new EngineConfig().setColorSpace(ERenderColorSpace.LINEAR));

        assertInstanceOf(ColorService.class, started.colors());
        assertEquals(ERenderColorSpace.LINEAR, started.colors().colorSpace());
        // The engine as a view inherits
        assertEquals(ERenderColorSpace.INHERIT, started.colorSpace());
        assertNull(started.picture());
        assertNull(started.finalPassShader());
    }

    // A replacement keeps the resolution of the contract (default methods)
    @Test
    void replacedByAPlugin(@TempDir Path resources) {
        IColorService mine = new IColorService() {
            @Override
            public ERenderColorSpace colorSpace() {
                return ERenderColorSpace.LINEAR;
            }

            @Override
            public void setColorSpace(ERenderColorSpace colorSpace) {}

            @Override
            public String finalPassShader() {
                return "myPass";
            }

            @Override
            public void setFinalPassShader(String shaderName) {}

            @Override
            public PictureSettings picture() {
                return PictureSettings.DEFAULT;
            }

            @Override
            public void setPicture(PictureSettings picture) {}
        };
        IPlugin plugin = new IPlugin() {
            @Override
            public String namespace() {
                return "game";
            }

            @Override
            public void register(EngineRegistry registry) {
                registry.services().replace(EngineServices.COLORS, engine -> mine);
            }
        };

        Engine started = start(resources, new EngineConfig().addPlugin(plugin));
        assertSame(mine, started.colors());
        assertEquals(ERenderColorSpace.GAMMA, started.colors().resolve(ERenderColorSpace.INHERIT, ERenderColorSpace.GAMMA));
        assertEquals(ERenderColorSpace.LINEAR, started.colors().resolve(null, null));
        assertEquals("myPass", started.colors().resolveFinalPassShader(null));
    }
}
