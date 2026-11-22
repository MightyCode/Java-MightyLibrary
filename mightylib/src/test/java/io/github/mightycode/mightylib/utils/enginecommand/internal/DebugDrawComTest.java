package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.debug.DebugDraw2D;
import io.github.mightycode.mightylib.graphics.debug.DebugDraws;
import io.github.mightycode.mightylib.graphics.debug.DebugLifetimes;
import io.github.mightycode.mightylib.graphics.debug.EDebugSpace;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.utils.enginecommand.CommandOwner;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The command debugdraw: on, off, clear, the drawings of each view by lifetime
class DebugDrawComTest {
    private static final Color4f WHITE = new Color4f(1f, 1f, 1f, 1f);

    private DebugDraws draws;
    private DebugDraw2D draw;
    private Commands commands;

    @BeforeEach
    void setUp() {
        MainThreadTestAccess.setCurrentAsMain();
        draws = new DebugDraws(true);
        draw = new DebugDraw2D(draws, new IView() {
            @Override
            public Engine engine() {
                return null;
            }

            @Override
            public WindowContext context() {
                return null;
            }

            @Override
            public Camera2D camera2D() {
                return null;
            }

            @Override
            public Camera3D camera3D() {
                return null;
            }
        }, "MyScene");
        commands = new Commands(ECommandLevel.DEV, ECommandLevel.DEV);
        commands.register(new DebugDrawGenCom(() -> draws), CommandOwner.Plugin(Identifier.ENGINE_NAMESPACE));
    }

    @Test
    void theDrawingsAreCountedByLifetime() {
        draw.line(0, 0, 1, 1, WHITE);
        draw.line(EDebugSpace.WORLD, 0, 0, 1, 1, WHITE, 1, DebugLifetimes.seconds(2));
        draw.line(EDebugSpace.WORLD, 0, 0, 1, 1, WHITE, 1, DebugLifetimes.seconds(3));

        CommandResult result = commands.execute("debugdraw list");
        assertTrue(result.isSuccess(), result.getMessage());
        assertTrue(result.getMessage().startsWith("Debug drawings on, 3 alive"), result.getMessage());
        assertTrue(result.getMessage().contains("MyScene: oneFrame x1, seconds x2"), result.getMessage());
        assertEquals(3, result.getData().getInt("drawings"));
        assertEquals(result.getMessage(), commands.execute("debugdraw").getMessage());
    }

    @Test
    void offStopsAndDropsOnStartsAgain() {
        draw.line(0, 0, 1, 1, WHITE);
        assertTrue(commands.execute("debugdraw off").isSuccess());
        assertFalse(draws.isEnabled());
        assertEquals(0, draw.size());

        assertTrue(commands.execute("debugdraw on").isSuccess());
        assertTrue(draws.isEnabled());
        draw.line(0, 0, 1, 1, WHITE);
        assertEquals(1, draw.size());
    }

    @Test
    void clearDropsTheDrawings() {
        draw.cross(EDebugSpace.SCREEN, 5, 5, 3, WHITE, 1, DebugLifetimes.untilRemoved());
        CommandResult result = commands.execute("debugdraw clear");
        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("1 debug drawings dropped", result.getMessage());
        assertEquals(0, draw.size());
    }
}
