package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.color.ColorService;
import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.color.PictureSettings;
import io.github.mightycode.mightylib.main.ColorServiceEngineTest;
import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.enginecommand.CommandOwner;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The commands colorspace (DEV) and picture (USER) on a color service without configuration
class ColorCommandsTest {
    private ColorService colors;
    private Commands commands;

    @BeforeEach
    void setUp() {
        MainThreadTestAccess.setCurrentAsMain();
        colors = new ColorService(ERenderColorSpace.GAMMA);
        commands = new Commands(ECommandLevel.DEV, ECommandLevel.DEV);
        CommandOwner engine = CommandOwner.Plugin(Identifier.ENGINE_NAMESPACE);
        commands.register(new ColorSpaceGenCom(() -> colors, () -> null), engine);
        commands.register(new PictureGenCom(() -> colors), engine);
    }

    @Test
    void colorspaceShowsAndChangesTheEngine() {
        CommandResult result = commands.execute("colorspace");
        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("Engine gamma", result.getMessage());
        assertEquals("gamma", result.getData().getString("resolved"));

        result = commands.execute("colorspace engine linear");
        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(ERenderColorSpace.LINEAR, colors.colorSpace());

        assertFalse(commands.execute("colorspace engine inherit").isSuccess());
        assertEquals(ERenderColorSpace.LINEAR, colors.colorSpace());
    }

    @Test
    void completion() {
        assertTrue(commands.complete("colorspace engine ").getCandidates().containsAll(
                List.of("linear", "gamma")));
        assertTrue(commands.complete("colorspace ").getCandidates().containsAll(List.of("engine", "view")));
        assertTrue(commands.complete("picture ").getCandidates().containsAll(
                List.of("gamma", "brightness", "contrast", "reset")));
    }

    // No scene (no engine here): the branch of the view is refused
    @Test
    void viewWithoutScene() {
        CommandResult result = commands.execute("colorspace view linear");
        assertFalse(result.isSuccess());
        assertEquals(CommandCondition.SCENE_STILL_LOADING, result.getMessage());
    }

    @Test
    void toggleIsTheOtherModeThanTheResolvedOne() {
        assertEquals(ERenderColorSpace.GAMMA, ColorSpaceGenCom.Toggled(ERenderColorSpace.LINEAR));
        assertEquals(ERenderColorSpace.LINEAR, ColorSpaceGenCom.Toggled(ERenderColorSpace.GAMMA));
    }

    @Test
    void pictureChangesOneValueAndResets() {
        CommandResult result = commands.execute("picture gamma 1.5");
        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(PictureSettings.of(1.5f, 1f, 1f), colors.picture());
        assertEquals(1.5, result.getData().getDouble("gamma"), 1e-6);

        assertTrue(commands.execute("picture contrast 0.5").isSuccess());
        assertEquals(PictureSettings.of(1.5f, 1f, 0.5f), colors.picture());

        // Out of bounds or not a number: refused, unchanged
        assertFalse(commands.execute("picture brightness 5").isSuccess());
        assertFalse(commands.execute("picture brightness 0.1").isSuccess());
        assertFalse(commands.execute("picture brightness high").isSuccess());
        assertEquals(PictureSettings.of(1.5f, 1f, 0.5f), colors.picture());

        assertTrue(commands.execute("picture reset").isSuccess());
        assertEquals(PictureSettings.DEFAULT, colors.picture());
    }

    @Test
    void pictureIsForThePlayer() {
        ICommandSource player = ICommandSource.Named("player", ECommandLevel.USER);

        assertTrue(commands.execute("picture brightness 1.2", player).isSuccess());
        assertFalse(commands.execute("colorspace", player).isSuccess());
    }

    // An engine without window: the view can't be changed
    @Test
    void viewWithoutWindow(@TempDir Path resources) {
        ColorServiceEngineTest.WithHeadlessEngine(resources, engine -> {
            Commands own = new Commands(ECommandLevel.DEV, ECommandLevel.DEV);
            own.register(new ColorSpaceGenCom(engine::colors, () -> null),
                    CommandOwner.Plugin(Identifier.ENGINE_NAMESPACE));

            CommandResult result = own.execute("colorspace view toggle");
            assertFalse(result.isSuccess());
            assertEquals(CommandCondition.NO_WINDOW, result.getMessage());
            assertTrue(own.execute("colorspace engine linear").isSuccess());
            assertEquals(ERenderColorSpace.LINEAR, engine.colors().colorSpace());
        });
    }
}
