package io.github.mightycode.mightylib.graphics.internal;

import io.github.mightycode.mightylib.main.utils.EngineStats;

import java.util.Arrays;

/**
 * The OpenGL state of one context as last set by the engine: a change equal to the known state is not sent again.
 * Each GLContext has its own (the state belongs to the context, not to the shared objects). Without OpenGL: the
 * calls go to an IGLStateApplier, so the logic is tested in JUnit.
 * <p>
 * A state starts at the OpenGL defaults of a new context. Code that changes the state behind it (a third-party
 * library, a raw GL call) must call invalidate: every next change is then sent.
 * <p>
 * Main thread only.
 */
public final class GLStateCache {
    // Capabilities, indexes of the cached enabled flags
    public static final int BLEND = 0;
    public static final int DEPTH_TEST = 1;
    public static final int CULL_FACE = 2;
    public static final int FRAMEBUFFER_SRGB = 3;
    public static final int SCISSOR_TEST = 4;
    private static final int CAPABILITIES = 5;

    // Units whose bindings are cached; a higher unit is always sent
    public static final int CACHED_UNITS = 32;

    // Targets whose bindings are cached (GL constants); another target is always sent
    private static final int[] TARGETS = {
            0x0DE1, // GL_TEXTURE_2D
            0x8513, // GL_TEXTURE_CUBE_MAP
            0x8C1A  // GL_TEXTURE_2D_ARRAY
    };

    private static final byte UNKNOWN = -1;
    private static final int UNKNOWN_ID = -1;

    private final IGLStateApplier applier;

    private final byte[] capabilities = new byte[CAPABILITIES];
    private int blendSource, blendDestination;
    private byte depthMask;
    private int scissorX, scissorY, scissorWidth, scissorHeight;
    private boolean scissorKnown;
    private int program;
    private int activeUnit;
    // Bound texture by unit then target
    private final int[][] textures = new int[CACHED_UNITS][TARGETS.length];

    // Counters of this context (EngineStats counts them for every context, per frame)
    private long changes;
    private long skipped;
    private long textureBinds;

    public GLStateCache(IGLStateApplier applier) {
        this.applier = applier;
        setDefaults();
    }

    // The state of a new context (OpenGL specification)
    private void setDefaults() {
        Arrays.fill(capabilities, (byte) 0);
        // GL_ONE, GL_ZERO
        blendSource = 1;
        blendDestination = 0;
        depthMask = 1;
        // The scissor box starts at the size of the window: unknown here
        scissorKnown = false;
        program = 0;
        activeUnit = 0;
        for (int[] unit : textures)
            Arrays.fill(unit, 0);
    }

    /** Forget the whole state: every next change is sent. After code that changed the state behind the cache. */
    public void invalidate() {
        Arrays.fill(capabilities, UNKNOWN);
        blendSource = blendDestination = UNKNOWN_ID;
        depthMask = UNKNOWN;
        scissorKnown = false;
        program = UNKNOWN_ID;
        activeUnit = UNKNOWN_ID;
        for (int[] unit : textures)
            Arrays.fill(unit, UNKNOWN_ID);
    }

    private void changed() {
        ++changes;
        EngineStats.countGLStateChange();
    }

    private boolean skip() {
        ++skipped;
        EngineStats.countGLStateSkipped();
        return false;
    }

    // ---- Capabilities ----

    /** @return true when the call was sent */
    public boolean capability(int capability, boolean enabled) {
        byte wanted = (byte) (enabled ? 1 : 0);
        if (capabilities[capability] == wanted)
            return skip();

        capabilities[capability] = wanted;
        changed();
        applier.capability(capability, enabled);
        return true;
    }

    public boolean isEnabled(int capability) {
        return capabilities[capability] == 1;
    }

    // ---- Blend ----

    /** Blend disabled when the source is GL_ONE and the destination GL_ZERO (nothing to mix). */
    public void blend(boolean enabled, int source, int destination) {
        capability(BLEND, enabled);
        if (!enabled)
            return;

        if (blendSource == source && blendDestination == destination) {
            skip();
            return;
        }

        blendSource = source;
        blendDestination = destination;
        changed();
        applier.blendFunc(source, destination);
    }

    public int blendSource() { return blendSource; }

    public int blendDestination() { return blendDestination; }

    // ---- Depth ----

    public boolean depthMask(boolean writable) {
        byte wanted = (byte) (writable ? 1 : 0);
        if (depthMask == wanted)
            return skip();

        depthMask = wanted;
        changed();
        applier.depthMask(writable);
        return true;
    }

    public boolean isDepthMaskWritable() {
        return depthMask == 1;
    }

    // ---- Scissor ----

    /** Draws limited to the rectangle (pixels of the bound framebuffer, origin bottom left). */
    public void scissor(int x, int y, int width, int height) {
        capability(SCISSOR_TEST, true);
        if (scissorKnown && scissorX == x && scissorY == y && scissorWidth == width && scissorHeight == height) {
            skip();
            return;
        }

        scissorKnown = true;
        scissorX = x;
        scissorY = y;
        scissorWidth = width;
        scissorHeight = height;
        changed();
        applier.scissor(x, y, width, height);
    }

    public void disableScissor() {
        capability(SCISSOR_TEST, false);
    }

    // ---- Program ----

    public int program() {
        return program;
    }

    /** @return true when glUseProgram was sent */
    public boolean useProgram(int newProgram) {
        if (program == newProgram)
            return skip();

        program = newProgram;
        changed();
        applier.useProgram(newProgram);
        return true;
    }

    /** The program is deleted (its id can be reused): not considered bound anymore. */
    public void forgetProgram(int deleted) {
        if (program == deleted)
            program = UNKNOWN_ID;
    }

    // ---- Textures ----

    public boolean activeTexture(int unit) {
        if (activeUnit == unit)
            return skip();

        activeUnit = unit;
        changed();
        applier.activeTexture(unit);
        return true;
    }

    /** The texture bound to the unit (made active). */
    public void bindTexture(int unit, int target, int texture) {
        activeTexture(unit);
        bindTexture(target, texture);
    }

    /** The texture bound to the active unit (an upload binds where it is). */
    public void bindTexture(int target, int texture) {
        int targetIndex = targetIndex(target);
        boolean cached = activeUnit >= 0 && activeUnit < CACHED_UNITS && targetIndex != -1;
        if (cached && textures[activeUnit][targetIndex] == texture) {
            skip();
            return;
        }

        if (cached)
            textures[activeUnit][targetIndex] = texture;
        changed();
        ++textureBinds;
        EngineStats.countTextureBind();
        applier.bindTexture(target, texture);
    }

    /** The bound texture known on the unit, -1 when unknown or not cached. */
    public int boundTexture(int unit, int target) {
        int targetIndex = targetIndex(target);
        if (unit < 0 || unit >= CACHED_UNITS || targetIndex == -1)
            return UNKNOWN_ID;
        return textures[unit][targetIndex];
    }

    /** The texture is deleted (its id can be reused): unbound on every unit. */
    public void forgetTexture(int deleted) {
        for (int[] unit : textures) {
            for (int i = 0; i < unit.length; ++i) {
                if (unit[i] == deleted)
                    unit[i] = UNKNOWN_ID;
            }
        }
    }

    private static int targetIndex(int target) {
        for (int i = 0; i < TARGETS.length; ++i) {
            if (TARGETS[i] == target)
                return i;
        }
        return -1;
    }

    // ---- Counters ----

    public long changes() { return changes; }

    public long skipped() { return skipped; }

    public long textureBinds() { return textureBinds; }
}
