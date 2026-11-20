package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

/**
 * The order of the draws of a view (IView.setDrawSortKey, DrawBatch2D.setDrawSortKey): a key computed for each draw
 * when it is asked; at the flush, the draws are sent in the ascending order of their keys (compared as signed
 * longs), draws of equal keys in the order they were asked. Without a key (null, the default), the draws are sent in
 * the order they were asked: no key is computed and nothing is sorted.
 * <p>
 * The low DrawBatch2D.CALL_INDEX_BITS bits of the key are replaced by the index of the draw (that keeps the sort
 * stable and deterministic): the function gives the high part, those bits are ignored. DrawSortKeys has ready ones.
 * <p>
 * The sort never crosses a barrier: what is drawn alone (a renderer that is not batched, a clear, a scissor, a
 * framebuffer bound, a flush) sends the draws asked before it. A layer can't move a draw over something drawn
 * after a barrier; the command batch counts the barriers of a sorted frame and names their causes.
 */
@FunctionalInterface
public interface IDrawSortKey {
    /**
     * Called for each batched draw when it is asked (main thread): must be cheap and allocate nothing.
     *
     * @param layer the layer of the draw (Renderer.setLayer, 0 by default)
     * @param depth the depth of the draw (Renderer.setSortDepth, 0 by default)
     * @param state the state of the draw: equal for draws that can be sent together (program, textures, OpenGL
     *              state, camera, color mode), a non-negative int
     */
    long key(int layer, float depth, int state);
}
