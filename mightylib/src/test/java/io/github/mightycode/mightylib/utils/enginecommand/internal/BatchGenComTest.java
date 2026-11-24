package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.enginecommand.CommandOwner;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The command batch (DEV) on fake batches: its description, the change of mode, the completion
class BatchGenComTest {
    private EBatch2DMode mode;
    private boolean noWindow;
    private Commands commands;

    private JSONObject describe() {
        JSONObject causes = new JSONObject().put("io.example.MyRenderer", new JSONObject().put("count", 2)
                .put("whileSorted", 1));
        JSONObject view = new JSONObject().put("view", "MyScene").put("draws", 1000).put("drawCalls", 3)
                .put("barriers", 2).put("barriersWhileSorted", 1).put("sortKey", "layerThenDepth")
                .put("barrierCauses", causes).put("shownLastFrame", true)
                .put("formats", new JSONObject().put("sprite", 2).put("circle", 1))
                .put("bytes", 24000).put("bytesPerCall", 8000.0).put("drawsPerCall", 333.3)
                .put("viewCull", new JSONObject().put("tested", 1200).put("culled", 200).put("noBounds", 5)
                        .put("noBoundsClasses", new JSONObject().put("io.example.MyCustomRenderer", 5)));
        // A scene left, its batch not collected yet: its last frame is an old one
        JSONObject left = new JSONObject().put("view", "LeftScene").put("draws", 500).put("drawCalls", 2)
                .put("barriers", 0).put("sortKey", JSONObject.NULL).put("barrierCauses", new JSONObject())
                .put("shownLastFrame", false);
        return new JSONObject().put("mode", mode.optionName()).put("views", new JSONArray().put(view).put(left));
    }

    @BeforeEach
    void setUp() {
        MainThreadTestAccess.setCurrentAsMain();
        mode = EBatch2DMode.ORPHAN;
        commands = new Commands(ECommandLevel.DEV, ECommandLevel.DEV);
        commands.register(new BatchGenCom(this::describe, newMode -> {
            if (noWindow)
                throw new IllegalStateException("No 2D batch without window");
            mode = newMode;
        }), CommandOwner.Plugin(Identifier.ENGINE_NAMESPACE));
    }

    @Test
    void theViewsAreDescribed() {
        CommandResult result = commands.execute("batch");
        assertTrue(result.isSuccess(), result.getMessage());
        assertTrue(result.getMessage().startsWith("Mode orphan"), result.getMessage());
        assertTrue(result.getMessage().contains("MyScene: 1000 draws in 3 draw calls, 2 barriers"),
                result.getMessage());
        assertTrue(result.getMessage().contains("sorted by layerThenDepth (1 barriers cut the sort)"),
                result.getMessage());
        assertTrue(result.getMessage().contains("MyRenderer x2"), result.getMessage());
        // The draw calls by batch format, sorted by name
        assertTrue(result.getMessage().contains("formats circle x1 sprite x2"), result.getMessage());
        assertTrue(result.getMessage().contains("24000 bytes sent (8000 and 333.3 draws per call)"),
                result.getMessage());
        assertTrue(result.getMessage().contains("view culling: 200 of 1200 tested culled, 5 without box"
                + " MyCustomRenderer x5"), result.getMessage());
        assertEquals(200, result.getData().getJSONArray("views").getJSONObject(0).getJSONObject("viewCull")
                .getInt("culled"));
        assertEquals("orphan", result.getData().getString("mode"));
        // A view not displayed in the last frame is in the data only
        assertFalse(result.getMessage().contains("LeftScene"), result.getMessage());
        assertEquals(2, result.getData().getJSONArray("views").length());
    }

    @Test
    void theModeChanges() {
        CommandResult result = commands.execute("batch mode ring");
        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(EBatch2DMode.RING, mode);
        assertEquals("ring", result.getData().getString("mode"));

        assertTrue(commands.execute("batch mode OFF").isSuccess());
        assertEquals(EBatch2DMode.OFF, mode);

        assertFalse(commands.execute("batch mode fast").isSuccess());
        assertEquals(EBatch2DMode.OFF, mode);
    }

    // Without window there is no batch: an error, not an exception
    @Test
    void withoutWindowTheModeIsRefused() {
        noWindow = true;
        CommandResult result = commands.execute("batch mode ring");
        assertFalse(result.isSuccess());
        assertEquals(EBatch2DMode.ORPHAN, mode);
    }

    @Test
    void completion() {
        assertTrue(commands.complete("batch ").getCandidates().contains("mode"));
        assertTrue(commands.complete("batch mode ").getCandidates().containsAll(
                List.of("off", "orphan", "ring", "instanced")));
    }

    @Test
    void itIsADeveloperCommand() {
        assertEquals(ECommandLevel.DEV, new BatchGenCom(this::describe, newMode -> mode = newMode).getLevel());
    }
}
