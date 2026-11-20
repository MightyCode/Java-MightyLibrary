package io.github.mightycode.mightylib.graphics.internal;

import io.github.mightycode.mightylib.graphics.renderer.EBlendMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GLStateCacheTest {
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int CUBE_MAP = 0x8513;

    // Records the calls sent to OpenGL
    private static final class Recorder implements IGLStateApplier {
        final List<String> calls = new ArrayList<>();

        @Override public void capability(int capability, boolean enabled) { calls.add("cap " + capability + " " + enabled); }
        @Override public void blendFunc(int source, int destination) { calls.add("blend " + source + " " + destination); }
        @Override public void depthMask(boolean writable) { calls.add("depthMask " + writable); }
        @Override public void scissor(int x, int y, int width, int height) { calls.add("scissor " + x + " " + y + " " + width + " " + height); }
        @Override public void useProgram(int program) { calls.add("program " + program); }
        @Override public void activeTexture(int unit) { calls.add("unit " + unit); }
        @Override public void bindTexture(int target, int texture) { calls.add("bind " + target + " " + texture); }
    }

    @Test
    void aRedundantChangeIsNotSent() {
        Recorder recorder = new Recorder();
        GLStateCache cache = new GLStateCache(recorder);

        assertTrue(cache.capability(GLStateCache.DEPTH_TEST, true));
        assertFalse(cache.capability(GLStateCache.DEPTH_TEST, true));
        assertTrue(cache.useProgram(4));
        assertFalse(cache.useProgram(4));
        cache.bindTexture(2, TEXTURE_2D, 9);
        cache.bindTexture(2, TEXTURE_2D, 9);
        cache.scissor(1, 2, 3, 4);
        cache.scissor(1, 2, 3, 4);

        assertEquals(List.of("cap 1 true", "program 4", "unit 2", "bind " + TEXTURE_2D + " 9",
                "cap 4 true", "scissor 1 2 3 4"), recorder.calls);
        assertEquals(6, cache.changes());
        assertEquals(1, cache.textureBinds());
        assertTrue(cache.skipped() >= 5);
    }

    @Test
    void theDefaultsAreThoseOfANewContext() {
        Recorder recorder = new Recorder();
        GLStateCache cache = new GLStateCache(recorder);

        // Disabled, writable, program 0, unit 0, nothing bound: nothing to send
        assertFalse(cache.capability(GLStateCache.BLEND, false));
        assertFalse(cache.capability(GLStateCache.FRAMEBUFFER_SRGB, false));
        assertFalse(cache.depthMask(true));
        assertFalse(cache.useProgram(0));
        assertFalse(cache.activeTexture(0));
        cache.bindTexture(0, TEXTURE_2D, 0);
        assertTrue(recorder.calls.isEmpty(), recorder.calls.toString());
    }

    @Test
    void invalidateSendsTheNextChanges() {
        Recorder recorder = new Recorder();
        GLStateCache cache = new GLStateCache(recorder);
        cache.capability(GLStateCache.CULL_FACE, true);
        cache.depthMask(false);
        cache.useProgram(3);
        cache.bindTexture(1, TEXTURE_2D, 5);
        cache.scissor(0, 0, 10, 10);
        cache.blend(true, 1, 0);
        recorder.calls.clear();

        cache.invalidate();
        cache.capability(GLStateCache.CULL_FACE, true);
        cache.depthMask(false);
        cache.useProgram(3);
        cache.bindTexture(1, TEXTURE_2D, 5);
        cache.scissor(0, 0, 10, 10);
        cache.blend(true, 1, 0);

        assertEquals(List.of("cap 2 true", "depthMask false", "program 3", "unit 1", "bind " + TEXTURE_2D + " 5",
                "cap 4 true", "scissor 0 0 10 10", "cap 0 true", "blend 1 0"), recorder.calls);
    }

    @Test
    void theCachesOfTwoContextsAreIndependent() {
        Recorder first = new Recorder();
        Recorder second = new Recorder();
        GLStateCache a = new GLStateCache(first);
        GLStateCache b = new GLStateCache(second);

        a.useProgram(7);
        a.capability(GLStateCache.BLEND, true);
        assertTrue(b.useProgram(7));
        assertTrue(b.capability(GLStateCache.BLEND, true));
        assertEquals(2, first.calls.size());
        assertEquals(2, second.calls.size());
    }

    @Test
    void eachBlendModeGivesItsFunction() {
        for (EBlendMode mode : EBlendMode.values()) {
            Recorder recorder = new Recorder();
            GLStateCache cache = new GLStateCache(recorder);
            cache.blend(mode.isEnabled(), mode.source(), mode.destination());

            if (mode.isEnabled()) {
                assertTrue(cache.isEnabled(GLStateCache.BLEND), mode.name());
                assertEquals(mode.source(), cache.blendSource(), mode.name());
                assertEquals(mode.destination(), cache.blendDestination(), mode.name());
            } else {
                assertFalse(cache.isEnabled(GLStateCache.BLEND), mode.name());
                assertTrue(recorder.calls.isEmpty(), mode.name());
            }
        }

        // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA (the default of the engine)
        assertEquals(0x0302, EBlendMode.ALPHA.source());
        assertEquals(0x0303, EBlendMode.ALPHA.destination());
        assertEquals(1, EBlendMode.PREMULTIPLIED.source());
        assertEquals(1, EBlendMode.ADDITIVE.destination());
        assertEquals(0x0306, EBlendMode.MULTIPLY.source());
    }

    @Test
    void aSameBlendFunctionIsNotSentAgain() {
        Recorder recorder = new Recorder();
        GLStateCache cache = new GLStateCache(recorder);
        cache.blend(true, 0x0302, 0x0303);
        cache.blend(false, 0, 0);
        cache.blend(true, 0x0302, 0x0303);

        assertEquals(List.of("cap 0 true", "blend 770 771", "cap 0 false", "cap 0 true"), recorder.calls);
    }

    @Test
    void aDeletedTextureOrProgramIsNotConsideredBound() {
        Recorder recorder = new Recorder();
        GLStateCache cache = new GLStateCache(recorder);
        cache.bindTexture(0, TEXTURE_2D, 12);
        cache.bindTexture(3, CUBE_MAP, 12);
        cache.useProgram(5);
        recorder.calls.clear();

        cache.forgetTexture(12);
        cache.forgetProgram(5);
        assertEquals(-1, cache.boundTexture(0, TEXTURE_2D));
        assertEquals(-1, cache.boundTexture(3, CUBE_MAP));

        // A new object with the same id is bound again
        cache.bindTexture(0, TEXTURE_2D, 12);
        cache.useProgram(5);
        assertEquals(List.of("unit 0", "bind " + TEXTURE_2D + " 12", "program 5"), recorder.calls);
    }

    @Test
    void bindingsAreKeptPerUnitAndTarget() {
        Recorder recorder = new Recorder();
        GLStateCache cache = new GLStateCache(recorder);
        cache.bindTexture(0, TEXTURE_2D, 1);
        cache.bindTexture(0, CUBE_MAP, 2);
        cache.bindTexture(1, TEXTURE_2D, 1);
        recorder.calls.clear();

        cache.bindTexture(0, TEXTURE_2D, 1);
        cache.bindTexture(0, CUBE_MAP, 2);
        cache.bindTexture(1, TEXTURE_2D, 1);
        // Only the unit changes are sent
        assertEquals(List.of("unit 0", "unit 1"), recorder.calls);

        // A unit out of the cached ones, or an unknown target, is always sent
        recorder.calls.clear();
        cache.bindTexture(GLStateCache.CACHED_UNITS, TEXTURE_2D, 1);
        cache.bindTexture(GLStateCache.CACHED_UNITS, TEXTURE_2D, 1);
        cache.bindTexture(0, 0x1234, 1);
        cache.bindTexture(0, 0x1234, 1);
        assertEquals(6, recorder.calls.size(), recorder.calls.toString());
    }
}
