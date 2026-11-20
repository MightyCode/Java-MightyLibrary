package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.enginecommand.CommandOwner;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The syntax of gl materials (the content needs a window: SharedMeshTest, MaterialTest of the kit)
class GLMaterialsComTest {
    private final Commands commands = newCommands();

    private static Commands newCommands() {
        Commands created = new Commands(ECommandLevel.DEV, ECommandLevel.DEV);
        created.register(new GLGenCom(), CommandOwner.Plugin(Identifier.ENGINE_NAMESPACE));
        return created;
    }

    @Test
    void materialsIsProposedAfterGl() {
        List<String> candidates = commands.complete("gl ").getCandidates();
        assertTrue(candidates.contains("materials"), candidates.toString());
        assertEquals(List.of("materials"), commands.complete("gl mat").getCandidates());
    }

    @Test
    void withoutMaterialsAliveTheListsAreEmpty() {
        CommandResult result = commands.execute("gl materials");

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(0, result.getData().getInt("sharedMaterials"));
        assertEquals(0, result.getData().getJSONArray("materials").length());
        assertTrue(result.getMessage().contains("material(s) alive"), result.getMessage());
    }

    @Test
    void anUnknownWordAfterMaterialsIsAnError() {
        assertFalse(commands.execute("gl materials extra").isSuccess());
    }
}
