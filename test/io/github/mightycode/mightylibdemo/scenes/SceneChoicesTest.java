package io.github.mightycode.mightylibdemo.scenes;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneChoicesTest {
    private static List<String> labels(List<SceneChoices.Entry> entries) {
        List<String> labels = new ArrayList<>();
        for (SceneChoices.Entry entry : entries)
            labels.add(entry.label());
        return labels;
    }

    private static List<String> commands(List<SceneChoices.Entry> entries) {
        List<String> commands = new ArrayList<>();
        for (SceneChoices.Entry entry : entries)
            commands.add(entry.command());
        return commands;
    }

    @Test
    void sortsByTheShortNameIgnoringTheCase() {
        SceneChoices choices = new SceneChoices(List.of("Scene3D", "GLLoading", "Box2D", "gamepadScene", "Platformer2D.PlatformerBox2D"),
                Set.of());

        assertEquals(List.of("Box2D", "gamepadScene", "GLLoading", "PlatformerBox2D", "Scene3D"), labels(choices.entries()));
    }

    @Test
    void theCommandKeepsTheRelativeName() {
        SceneChoices choices = new SceneChoices(List.of("Platformer2D.PlatformerBox2D"), Set.of());

        SceneChoices.Entry entry = choices.entries().get(0);
        assertEquals("PlatformerBox2D", entry.label());
        assertEquals("Platformer2D.PlatformerBox2D", entry.command());
        assertEquals("PlatformerBox2D", entry.shortName());
    }

    @Test
    void hiddenScenesAreNotOffered() {
        SceneChoices choices = new SceneChoices(List.of("MenuScene", "Scene2D", "loadingScenes.LoadingSceneImplementation"),
                Set.of("MenuScene", "loadingScenes.LoadingSceneImplementation"));

        assertEquals(List.of("Scene2D"), commands(choices.entries()));
    }

    @Test
    void relativeNameIsShownOnlyForScenesWithTheSameShortName() {
        SceneChoices choices = new SceneChoices(List.of("Game.Level", "Other.Level", "Alone", "Game.Menu"), Set.of());

        assertEquals(List.of("Alone", "Game.Level", "Menu", "Other.Level"), labels(choices.entries()));
        assertEquals(List.of("Alone", "Game.Level", "Game.Menu", "Other.Level"), commands(choices.entries()));
    }

    @Test
    void aHiddenSceneDoesNotMakeTheNameShared() {
        SceneChoices choices = new SceneChoices(List.of("Game.Level", "Other.Level"), Set.of("Other.Level"));

        assertEquals(List.of("Level"), labels(choices.entries()));
    }

    @Test
    void filterContainsTheTextIgnoringTheCase() {
        SceneChoices choices = new SceneChoices(List.of("Scene2D", "Scene3D", "Scene3DBis", "Box2D", "PathfindingScene2D"), Set.of());

        assertEquals(List.of("Scene3D", "Scene3DBis"), labels(choices.filter("e3d")));
        assertEquals(List.of("Box2D", "PathfindingScene2D", "Scene2D"), labels(choices.filter("2D")));
        assertEquals(List.of("PathfindingScene2D", "Scene2D"), labels(choices.filter(" SCENE2 ")));
    }

    @Test
    void filterLooksAtTheShortNameOnly() {
        SceneChoices choices = new SceneChoices(List.of("Platformer2D.PlatformerBox2D", "Scene2D"), Set.of());

        // Not "Platformer2D": it is the package, not the short name
        assertTrue(choices.filter("Platformer2D").isEmpty());
        assertEquals(List.of("PlatformerBox2D"), labels(choices.filter("platformer")));
        assertTrue(choices.filter("2D.").isEmpty());
    }

    @Test
    void emptyFilterGivesEverything() {
        SceneChoices choices = new SceneChoices(List.of("B", "A"), Set.of());

        assertEquals(List.of("A", "B"), labels(choices.filter("")));
        assertEquals(List.of("A", "B"), labels(choices.filter("   ")));
        assertEquals(List.of("A", "B"), labels(choices.filter(null)));
    }

    @Test
    void noMatchGivesAnEmptyList() {
        SceneChoices choices = new SceneChoices(List.of("A"), Set.of());

        assertTrue(choices.filter("zzz").isEmpty());
    }
}
