package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.algorithms.OutputCapture;
import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.ELogLevel;
import io.github.mightycode.mightylib.utils.ILogListener;
import io.github.mightycode.mightylib.utils.LogEntry;
import io.github.mightycode.mightylib.utils.Logger;
import org.joml.Vector2f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The text of the camera debug information, without GL: order of addition, precision, unknown names
class DebugInfoCamera2DTest {
    private final List<LogEntry> entries = new CopyOnWriteArrayList<>();
    private final ILogListener listener = entries::add;

    @AfterEach
    void removeListener() {
        Logger.RemoveListener(listener);
    }

    private static Camera2D camera() throws ReflectiveOperationException {
        Constructor<WindowInfo> constructor = WindowInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        WindowInfo info = constructor.newInstance();
        info.getVirtualSizeRef().set(1000, 500);
        return new Camera2D(info, new Vector2f(1.23456f, 2f));
    }

    private static List<String> lines(String text) {
        return List.of(text.split("\n"));
    }

    @Test
    void linesAreInTheOrderOfAddition() throws ReflectiveOperationException {
        Camera2D camera = camera();
        Set<String> info = new LinkedHashSet<>();
        for (String name : new String[]{"zoom", "rotation", "position"})
            assertTrue(DebugInfoCamera2D.AddInfo(info, name));

        List<String> lines = lines(DebugInfoCamera2D.Describe(camera, info, "%.2f"));
        assertEquals(4, lines.size(), lines.toString());
        assertTrue(lines.get(0).startsWith("camera2D "), lines.get(0));
        assertTrue(lines.get(1).startsWith("Zoom"), lines.get(1));
        assertTrue(lines.get(2).startsWith("Rotation"), lines.get(2));
        assertTrue(lines.get(3).startsWith("Position"), lines.get(3));

        Set<String> reversed = new LinkedHashSet<>();
        for (String name : new String[]{"position", "rotation", "zoom"})
            DebugInfoCamera2D.AddInfo(reversed, name);
        List<String> other = lines(DebugInfoCamera2D.Describe(camera, reversed, "%.2f"));
        assertTrue(other.get(1).startsWith("Position"), other.get(1));
        assertTrue(other.get(3).startsWith("Zoom"), other.get(3));
    }

    @Test
    void theRotationIsShownAtThePrecision() throws ReflectiveOperationException {
        Camera2D camera = camera();
        camera.setRotation(0.123456f);
        Set<String> info = new LinkedHashSet<>();
        DebugInfoCamera2D.AddInfo(info, "rotation");

        assertEquals("Rotation: " + String.format("%.3f", 0.123456f),
                lines(DebugInfoCamera2D.Describe(camera, info, "%.3f")).get(1));
        assertEquals("Rotation: " + String.format("%.0f", 0.123456f),
                lines(DebugInfoCamera2D.Describe(camera, info, "%.0f")).get(1));
    }

    @Test
    void anUnknownNameIsAWarningAndShowsNothing() throws ReflectiveOperationException {
        Logger.AddListener(listener);
        Set<String> info = new LinkedHashSet<>();
        String output = OutputCapture.errorsOf(() -> assertFalse(DebugInfoCamera2D.AddInfo(info, "speed")));

        assertTrue(info.isEmpty());
        List<LogEntry> warnings = entries.stream()
                .filter(entry -> entry.message().contains("'speed'")).collect(Collectors.toList());
        assertEquals(1, warnings.size(), output);
        assertEquals(ELogLevel.WARNING, warnings.get(0).level());
        assertEquals(Logger.CATEGORY_ENGINE, warnings.get(0).category());
        assertEquals(1, lines(DebugInfoCamera2D.Describe(camera(), info, "%.2f")).size());
    }

    @Test
    void aNameInUpperCaseIsAccepted() {
        Logger.AddListener(listener);
        Set<String> info = new LinkedHashSet<>();
        assertTrue(DebugInfoCamera2D.AddInfo(info, "ZOOM"));
        assertTrue(DebugInfoCamera2D.AddInfo(info, "Position"));
        assertFalse(DebugInfoCamera2D.AddInfo(info, "zoom"));

        assertEquals(List.of("zoom", "position"), List.copyOf(info));
        assertTrue(entries.stream().noneMatch(entry -> entry.message().startsWith("DebugInfoCamera2D")));
    }
}
