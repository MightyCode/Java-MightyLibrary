package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

/**
 * Ready orders of the draws (IDrawSortKey). Their key, high to low: the layer (16 bits, clamped to the range of a
 * short), then 24 bits (the depth or the state), then the index of the draw (DrawBatch2D.CALL_INDEX_BITS).
 */
public final class DrawSortKeys {
    private static final int LAYER_SHIFT = 48;
    private static final int MIDDLE_SHIFT = DrawBatch2D.CALL_INDEX_BITS;
    private static final long MIDDLE_MASK = 0xFFFFFFL;

    private static final IDrawSortKey LAYER_THEN_DEPTH = (layer, depth, state) ->
            layerBits(layer) | (orderedDepth(depth) & MIDDLE_MASK) << MIDDLE_SHIFT;

    private static final IDrawSortKey LAYER_THEN_STATE = (layer, depth, state) ->
            layerBits(layer) | (state & MIDDLE_MASK) << MIDDLE_SHIFT;

    private DrawSortKeys() {}

    /**
     * Higher layers over lower ones; in a layer, higher depths over lower ones (a depth of the Y of the sprite draws
     * the lower ones over); equal layer and depth in the order of the draws. The depth keeps the 24 high bits of its
     * float: depths closer than about 1/30000 of their value are equal.
     */
    public static IDrawSortKey layerThenDepth() {
        return LAYER_THEN_DEPTH;
    }

    /**
     * Higher layers over lower ones; in a layer, the draws of the same state together (fewer draw calls), the order
     * between draws of different states of a layer is lost: for a layer whose draws do not overlap, or overlap
     * without a visible order (same sprite).
     */
    public static IDrawSortKey layerThenState() {
        return LAYER_THEN_STATE;
    }

    private static long layerBits(int layer) {
        int clamped = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, layer));
        return (long) clamped << LAYER_SHIFT;
    }

    // The 24 high bits of the float as an unsigned int ordered like the float (negatives included, -0 equal to 0)
    static long orderedDepth(float depth) {
        int bits = Float.floatToIntBits(depth + 0.0f);
        int ordered = bits < 0 ? ~bits : bits ^ 0x80000000;
        return (ordered & 0xFFFFFFFFL) >>> 8;
    }
}
