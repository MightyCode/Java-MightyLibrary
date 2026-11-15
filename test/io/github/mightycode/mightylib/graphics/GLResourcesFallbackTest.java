package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.graphics.GLResourcesTest.BreakableElement;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FakeData;
import io.github.mightycode.mightylib.resources.Resources;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/** The GL elements of the fallback data: declared by each data, found by data type, loaded now or in the next frames. */
class GLResourcesFallbackTest {
    // A fallback data whose element is a BreakableElement named FALLBACK_NAME
    static class FallbackFake extends FakeData {
        final boolean neededBeforeDrawing;
        final boolean broken;

        FallbackFake(boolean neededBeforeDrawing, boolean broken) {
            super(MAIN_CONTEXT, Resources.FALLBACK_NAME);
            this.neededBeforeDrawing = neededBeforeDrawing;
            this.broken = broken;
        }

        @Override
        public Class<? extends GLElement> glElementType() {
            return BreakableElement.class;
        }

        @Override
        public boolean glElementNeededBeforeDrawing() {
            return neededBeforeDrawing;
        }

        @Override
        public GLElement createGLElement() {
            BreakableElement element = new BreakableElement(this);
            element.broken = broken;
            return element;
        }
    }

    // Without a fallback of its own: uses the one of FakeData
    static class SubFake extends FakeData {
        SubFake() {
            super(MAIN_CONTEXT, "sub");
        }
    }

    // With a fallback of its own, its element of the same type and name as the one of FakeData
    static class OtherFake extends FakeData {
        OtherFake() {
            super(MAIN_CONTEXT, "other");
        }
    }

    private static Map<Class<? extends DataType>, DataType> fallbacks(Object... typesAndData) {
        Map<Class<? extends DataType>, DataType> map = new LinkedHashMap<>();
        for (int i = 0; i < typesAndData.length; i += 2) {
            @SuppressWarnings("unchecked")
            Class<? extends DataType> type = (Class<? extends DataType>) typesAndData[i];
            map.put(type, (DataType) typesAndData[i + 1]);
        }

        return map;
    }

    @Test
    void anElementNeededBeforeDrawingIsLoadedDuringTheCall() {
        GLResources glResources = new GLResources(-1);
        glResources.fallbackDataLoaded(fallbacks(FakeData.class, new FallbackFake(true, false)));

        GLElement element = glResources.fallback(FakeData.class);
        assertTrue(element.isLoaded());
        assertTrue(glResources.namesWaiting(true).isEmpty());
    }

    @Test
    void theOtherElementsAreSentInTheNextFrames() {
        GLResources glResources = new GLResources(-1);
        glResources.fallbackDataLoaded(fallbacks(FakeData.class, new FallbackFake(false, false)));

        GLElement element = glResources.fallback(FakeData.class);
        assertFalse(element.isLoaded());
        assertEquals(List.of(Resources.FALLBACK_NAME), glResources.namesWaiting(true));

        glResources.process(Long.MAX_VALUE);
        assertTrue(element.isLoaded());
    }

    // Nothing could replace it: the program stops
    @Test
    void aFailureBeforeDrawingStopsTheProgram() {
        GLResources glResources = new GLResources(-1);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> glResources.fallbackDataLoaded(fallbacks(FakeData.class, new FallbackFake(true, true))));
        assertTrue(error.getMessage().contains("FakeData"), error.getMessage());
    }

    // Its users keep their placeholder
    @Test
    void aLaterFailureOnlyMarksTheElement() {
        GLResources glResources = new GLResources(-1);
        glResources.fallbackDataLoaded(fallbacks(FakeData.class, new FallbackFake(false, true)));
        glResources.process(Long.MAX_VALUE);

        GLElement element = glResources.fallback(FakeData.class);
        assertTrue(element.hasFailed());
        assertEquals(List.of("BreakableElement " + Resources.FALLBACK_NAME), glResources.namesFailed());
    }

    @Test
    void aTypeUsesTheFallbackOfItsClosestParent() {
        GLResources glResources = new GLResources(-1);
        assertNull(glResources.fallback(FakeData.class));

        glResources.fallbackDataLoaded(fallbacks(FakeData.class, new FallbackFake(true, false)));
        assertSame(glResources.fallback(FakeData.class), glResources.fallback(SubFake.class));
        assertNull(glResources.fallback(DataType.class));
    }

    @Test
    void twoFallbacksOfTheSameElementTypeDoNotCollide() {
        GLResources glResources = new GLResources(-1);
        glResources.fallbackDataLoaded(fallbacks(FakeData.class, new FallbackFake(true, false),
                OtherFake.class, new FallbackFake(true, false)));

        GLElement fake = glResources.fallback(FakeData.class);
        GLElement other = glResources.fallback(OtherFake.class);
        assertNotSame(fake, other);
        assertEquals(fake.getName(), other.getName());
        assertTrue(fake.isLoaded() && other.isLoaded());

        // Out of the table by name: an element of that name is another one
        assertNull(glResources.getResource(BreakableElement.class, Resources.FALLBACK_NAME));
    }

    @Test
    void theFallbacksAreDescribed() {
        GLResources glResources = new GLResources(-1);
        glResources.fallbackDataLoaded(fallbacks(FakeData.class, new FallbackFake(true, false),
                OtherFake.class, new FallbackFake(false, true)));
        glResources.process(Long.MAX_VALUE);

        JSONArray list = glResources.describeFallbacks();
        assertEquals(2, list.length());
        JSONObject fake = list.getJSONObject(0);
        assertEquals("FakeData", fake.getString("dataType"));
        assertEquals("BreakableElement", fake.getString("elementType"));
        assertTrue(fake.getBoolean("neededBeforeDrawing"));
        assertEquals("loaded", fake.getString("state"));
        JSONObject other = list.getJSONObject(1);
        assertFalse(other.getBoolean("neededBeforeDrawing"));
        assertEquals("failed", other.getString("state"));
    }

    @Test
    void acquiringAFallbackDataGivesItsElement() {
        GLResources glResources = new GLResources(-1);
        FallbackFake data = new FallbackFake(true, false);
        glResources.fallbackDataLoaded(fallbacks(FakeData.class, data));

        GLElement element = glResources.acquire(data, true);
        assertSame(glResources.fallback(FakeData.class), element);
        assertEquals(2, element.getNumberReference());

        // Released by its user, kept for the whole program
        glResources.release(element, true);
        assertTrue(element.isLoaded());
        assertEquals(1, element.getNumberReference());
    }
}
