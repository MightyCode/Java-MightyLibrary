package io.github.mightycode.mightylibdemo.scenes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneHistoryTest {
    private static final String DIR = SceneHistory.SCENES_DIRECTORY;

    @Test
    void addedIsTheFirstCommitAndModifiedTheLast() {
        String log = "@100\n\nA\t" + DIR + "Scene2D.java\n\n"
                + "@200\n\nA\t" + DIR + "Box2D.java\nM\t" + DIR + "Scene2D.java\n\n"
                + "@300\n\nM\t" + DIR + "Box2D.java\n";

        Map<String, SceneHistory.Dates> dates = SceneHistory.ByCommand(SceneHistory.ParseLog(log));

        assertEquals(100, dates.get("Scene2D").added());
        assertEquals(200, dates.get("Scene2D").modified());
        assertEquals(200, dates.get("Box2D").added());
        assertEquals(300, dates.get("Box2D").modified());
    }

    @Test
    void aRenameKeepsTheDateOfAddition() {
        String log = "@100\nA\t" + DIR + "Old.java\n"
                + "@200\nR087\t" + DIR + "Old.java\t" + DIR + "Platformer2D/New.java\n";

        Map<String, SceneHistory.Dates> dates = SceneHistory.ByCommand(SceneHistory.ParseLog(log));

        assertFalse(dates.containsKey("Old"));
        assertEquals(100, dates.get("Platformer2D.New").added());
        assertEquals(200, dates.get("Platformer2D.New").modified());
    }

    @Test
    void aDeletedSceneHasNoDates() {
        String log = "@100\nA\t" + DIR + "Gone.java\n@200\nD\t" + DIR + "Gone.java\n";

        assertTrue(SceneHistory.ParseLog(log).isEmpty());
    }

    @Test
    void changesNotCommittedTakeTheDateOfTheFile() {
        Map<String, SceneHistory.Dates> byPath = SceneHistory.ParseLog("@100\nA\t" + DIR + "Scene2D.java\n");
        String status = " M " + DIR + "Scene2D.java\n?? " + DIR + "NewScene2D.java\n";

        SceneHistory.ApplyStatus(byPath, status, path -> 500);
        Map<String, SceneHistory.Dates> dates = SceneHistory.ByCommand(byPath);

        assertEquals(100, dates.get("Scene2D").added());
        assertEquals(500, dates.get("Scene2D").modified());
        assertEquals(500, dates.get("NewScene2D").added());
        assertEquals(500, dates.get("NewScene2D").modified());
    }

    @Test
    void onlyTheSourcesOfTheScenesCount() {
        String log = "@100\nA\t" + DIR + "notes.txt\nA\tsrc/Other.java\nA\t" + DIR + "Scene2D.java\n";

        assertEquals(1, SceneHistory.ByCommand(SceneHistory.ParseLog(log)).size());
    }
}
