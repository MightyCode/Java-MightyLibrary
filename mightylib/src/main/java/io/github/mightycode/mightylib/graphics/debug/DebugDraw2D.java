package io.github.mightycode.mightylib.graphics.debug;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormats;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.text.FontFace;
import io.github.mightycode.mightylib.graphics.text.TextLayout;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.apiguardian.api.API;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector2i;
import org.joml.Vector4f;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * Drawings for debugging, asked from anywhere in the code of a view (IView.debugDraw), without renderer: lines,
 * polylines, rectangles, circles, crosses, arrows, texts, in the coordinates of the world (the 2D camera of the view)
 * or of the screen (EDebugSpace), widths in pixels of the screen. Drawn over everything at the end of the display of
 * the view (before its final pass), in its 2D batch; each lives one frame by default (asked again at each frame), or
 * as long as its IDebugLifetime says (DebugLifetimes), or until its handle is removed. Dropped when the scene is left
 * or the window closed. Off with the switch of the engine (DebugDraws). Main thread.
 */
public final class DebugDraw2D {
    // Default width of the lines, pixels of the screen
    public static final float DEFAULT_WIDTH = 1f;
    // Default size of the texts: height of a line in pixels of the screen (SCREEN), world units (WORLD)
    public static final float DEFAULT_TEXT_SIZE = 16f;
    // Length of the head of an arrow, in widths of its line (at least ARROW_HEAD_MIN pixels)
    private static final float ARROW_HEAD_WIDTHS = 6f;
    private static final float ARROW_HEAD_MIN = 8f;

    private final DebugDraws owner;
    private final IView view;
    private final DebugDrawQueue queue = new DebugDrawQueue();
    // The handle of the drawing being recorded
    private DebugDrawHandle lastHandle = DebugDrawHandle.NONE;

    // Created at the first drawing drawn
    private Material lineMaterial;
    private Material circleMaterial;
    private Material textMaterial;
    private String textTexture;
    private Camera2D screenCamera;
    private final Matrix4f matrix = new Matrix4f();
    private final Vector4f bounds = new Vector4f();
    private final Vector4f glyph = new Vector4f();
    private final CharacterQuads characters = new CharacterQuads();

    /** The drawings of a view, registered in the debug drawings of its engine (named for the command debugdraw). */
    @API(status = INTERNAL)
    public DebugDraw2D(DebugDraws owner, IView view, String viewName) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.view = Objects.requireNonNull(view, "view");
        owner.register(this, viewName);
    }

    /** False when the debug drawings of the engine are off: nothing is recorded. */
    public boolean isEnabled() {
        return owner.isEnabled();
    }

    // ---- Drawings ----

    public DebugDrawHandle line(float x1, float y1, float x2, float y2, Color4f color) {
        return line(EDebugSpace.WORLD, x1, y1, x2, y2, color, DEFAULT_WIDTH, DebugLifetimes.oneFrame());
    }

    public DebugDrawHandle line(EDebugSpace space, float x1, float y1, float x2, float y2, Color4f color, float width,
                                IDebugLifetime lifetime) {
        if (!start(DebugDrawQueue.LINE, space, color, width, null, lifetime))
            return DebugDrawHandle.NONE;
        return values(x1, y1, x2, y2);
    }

    public DebugDrawHandle polyline(List<? extends Vector2fc> points, boolean closed, Color4f color) {
        return polyline(EDebugSpace.WORLD, points, closed, color, DEFAULT_WIDTH, DebugLifetimes.oneFrame());
    }

    /** The points are copied: changing them afterwards changes nothing. */
    public DebugDrawHandle polyline(EDebugSpace space, List<? extends Vector2fc> points, boolean closed,
                                    Color4f color, float width, IDebugLifetime lifetime) {
        Objects.requireNonNull(points, "points");
        if (!start(closed ? DebugDrawQueue.CLOSED_POLYLINE : DebugDrawQueue.POLYLINE, space, color, width, null,
                lifetime))
            return DebugDrawHandle.NONE;

        for (Vector2fc point : points) {
            queue.value(point.x());
            queue.value(point.y());
        }
        return lastHandle;
    }

    // The outline of a rectangle: its top left corner (x, y) and its size
    public DebugDrawHandle rect(float x, float y, float width, float height, Color4f color) {
        return rect(EDebugSpace.WORLD, x, y, width, height, color, DEFAULT_WIDTH, DebugLifetimes.oneFrame());
    }

    public DebugDrawHandle rect(EDebugSpace space, float x, float y, float width, float height, Color4f color,
                                float lineWidth, IDebugLifetime lifetime) {
        if (!start(DebugDrawQueue.RECT, space, color, lineWidth, null, lifetime))
            return DebugDrawHandle.NONE;
        return values(x, y, width, height);
    }

    // The outline of a circle (smooth, its line in pixels of the screen)
    public DebugDrawHandle circle(float x, float y, float radius, Color4f color) {
        return circle(EDebugSpace.WORLD, x, y, radius, color, DEFAULT_WIDTH, DebugLifetimes.oneFrame());
    }

    public DebugDrawHandle circle(EDebugSpace space, float x, float y, float radius, Color4f color, float width,
                                  IDebugLifetime lifetime) {
        if (!start(DebugDrawQueue.CIRCLE, space, color, width, null, lifetime))
            return DebugDrawHandle.NONE;
        return values(x, y, Math.abs(radius), 0f);
    }

    // A cross of two lines of length size, centered on (x, y), its size in the units of the space
    public DebugDrawHandle cross(float x, float y, float size, Color4f color) {
        return cross(EDebugSpace.WORLD, x, y, size, color, DEFAULT_WIDTH, DebugLifetimes.oneFrame());
    }

    public DebugDrawHandle cross(EDebugSpace space, float x, float y, float size, Color4f color, float width,
                                 IDebugLifetime lifetime) {
        if (!start(DebugDrawQueue.CROSS, space, color, width, null, lifetime))
            return DebugDrawHandle.NONE;
        return values(x, y, size, 0f);
    }

    // A line from (x1, y1) with a head at (x2, y2)
    public DebugDrawHandle arrow(float x1, float y1, float x2, float y2, Color4f color) {
        return arrow(EDebugSpace.WORLD, x1, y1, x2, y2, color, DEFAULT_WIDTH, DebugLifetimes.oneFrame());
    }

    public DebugDrawHandle arrow(EDebugSpace space, float x1, float y1, float x2, float y2, Color4f color,
                                 float width, IDebugLifetime lifetime) {
        if (!start(DebugDrawQueue.ARROW, space, color, width, null, lifetime))
            return DebugDrawHandle.NONE;
        return values(x1, y1, x2, y2);
    }

    // A text in the font of the engine, its top left corner at (x, y), its lines split at '\n'
    public DebugDrawHandle text(float x, float y, String text, Color4f color) {
        return text(EDebugSpace.WORLD, x, y, text, color, DEFAULT_TEXT_SIZE, DebugLifetimes.oneFrame());
    }

    /** size: the height of a line, pixels of the screen (SCREEN) or world units (WORLD). */
    public DebugDrawHandle text(EDebugSpace space, float x, float y, String text, Color4f color, float size,
                                IDebugLifetime lifetime) {
        Objects.requireNonNull(text, "text");
        if (!start(DebugDrawQueue.TEXT, space, color, size, text, lifetime))
            return DebugDrawHandle.NONE;
        return values(x, y, 0f, 0f);
    }

    // Records the drawing when on; its values follow
    private boolean start(byte kind, EDebugSpace space, Color4f color, float width, String text,
                          IDebugLifetime lifetime) {
        Objects.requireNonNull(space, "space");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(lifetime, "lifetime");
        if (!owner.isEnabled())
            return false;

        lastHandle = queue.add(kind, space, color.toIntRGBA(), width, text, lifetime);
        return true;
    }

    private DebugDrawHandle values(float a, float b, float c, float d) {
        queue.value(a);
        queue.value(b);
        queue.value(c);
        queue.value(d);
        return lastHandle;
    }

    /** The drawings waiting are dropped (the view is left, or the code asks it). */
    public void clear() {
        queue.clear();
    }

    // Drawings alive (drawn at the next frame)
    public int size() {
        return queue.size();
    }

    // Drawings alive by the name of their lifetime (command debugdraw)
    Map<String, Integer> countByLifetime() {
        return queue.countByLifetime();
    }

    // ---- Frame ----

    /**
     * End of the display of the view (Scene, WindowView), once per frame: the lifetimes are asked, then the
     * drawings alive are drawn in the batch after a barrier (over everything), then those of one frame dropped.
     */
    @API(status = INTERNAL)
    public void drawFrame(DrawBatch2D batch) {
        if (!owner.isEnabled()) {
            queue.clear();
            return;
        }

        queue.beginFrame();
        if (queue.size() > 0 && batch != null) {
            batch.flush();
            drawAll(batch);
            batch.flush();
        }
        queue.endFrame();
    }

    private void drawAll(DrawBatch2D batch) {
        if (lineMaterial == null) {
            lineMaterial = new Material(EngineResources.COLOR_SHAPE_2D);
            circleMaterial = new Material(EngineResources.CIRCLE_2D);
            textMaterial = new Material(EngineResources.COLORED_TEXT);
            lineMaterial.retain();
            circleMaterial.retain();
            textMaterial.retain();
        }

        Camera world = view.camera2D();
        Camera screen = screenCamera();
        Vector2i size = view.context().getWindow().getInfo().getVirtualSizeRef();
        float worldPixel = worldPerPixel(world, size);
        float screenPixel = worldPerPixel(screen, size);
        Resources resources = view.engine().resources();
        FontFace font = resources.usable(FontFace.class,
                resources.getResourceOrFallback(FontFace.class, view.engine().config().getDefaultFont()));
        String atlas = font.getTexture().getDataName();
        if (!atlas.equals(textTexture)) {
            textMaterial.setMainTexture(atlas);
            textTexture = atlas;
        }

        for (int i = 0; i < queue.size(); ++i) {
            boolean inWorld = queue.space(i) == EDebugSpace.WORLD;
            Camera camera = inWorld ? world : screen;
            float pixel = inWorld ? worldPixel : screenPixel;
            switch (queue.kind(i)) {
                case DebugDrawQueue.CIRCLE:
                    drawCircle(batch, i, camera);
                    break;
                case DebugDrawQueue.TEXT:
                    drawText(batch, i, camera, font);
                    break;
                default:
                    drawLines(batch, i, camera, pixel);
                    break;
            }
        }
    }

    // The camera of the screen: the virtual size of the view, its origin at the top left, never moved
    private Camera2D screenCamera() {
        if (screenCamera == null)
            screenCamera = new Camera2D(view.context().getWindow().getInfo(), new Vector2f());
        return screenCamera;
    }

    // World units covered by a pixel of the virtual size along x, through the camera
    private float worldPerPixel(Camera camera, Vector2i size) {
        ((Matrix4f) camera.getProjection().getObject()).mul((Matrix4f) camera.getView().getObject(), matrix);
        float x = matrix.m00() * size.x * 0.5f, y = matrix.m01() * size.y * 0.5f;
        float pixelsPerUnit = (float) Math.sqrt(x * x + y * y);
        return pixelsPerUnit > 0f ? 1f / pixelsPerUnit : 1f;
    }

    private void drawLines(DrawBatch2D batch, int i, Camera camera, float pixel) {
        int state = batch.state(lineMaterial, camera);
        if (state < 0)
            return;

        int color = queue.color(i);
        float width = Math.max(queue.width(i), 0f) * pixel;
        float a = queue.value(i, 0), b = queue.value(i, 1), c = queue.value(i, 2), d = queue.value(i, 3);
        switch (queue.kind(i)) {
            case DebugDrawQueue.LINE:
                segment(batch, state, a, b, c, d, width, color);
                break;
            case DebugDrawQueue.RECT:
                segment(batch, state, a, b, a + c, b, width, color);
                segment(batch, state, a + c, b, a + c, b + d, width, color);
                segment(batch, state, a + c, b + d, a, b + d, width, color);
                segment(batch, state, a, b + d, a, b, width, color);
                break;
            case DebugDrawQueue.CROSS:
                segment(batch, state, a - c / 2, b, a + c / 2, b, width, color);
                segment(batch, state, a, b - c / 2, a, b + c / 2, width, color);
                break;
            case DebugDrawQueue.ARROW:
                drawArrow(batch, state, a, b, c, d, width, Math.max(ARROW_HEAD_MIN * pixel, width * ARROW_HEAD_WIDTHS),
                        color);
                break;
            default:
                drawPolyline(batch, state, i, width, color);
                break;
        }
    }

    private void drawPolyline(DrawBatch2D batch, int state, int i, float width, int color) {
        int points = queue.valueCount(i) / 2;
        for (int p = 0; p + 1 < points; ++p)
            segment(batch, state, queue.value(i, 2 * p), queue.value(i, 2 * p + 1), queue.value(i, 2 * p + 2),
                    queue.value(i, 2 * p + 3), width, color);

        if (queue.kind(i) == DebugDrawQueue.CLOSED_POLYLINE && points > 2)
            segment(batch, state, queue.value(i, 2 * points - 2), queue.value(i, 2 * points - 1), queue.value(i, 0),
                    queue.value(i, 1), width, color);
    }

    private static void drawArrow(DrawBatch2D batch, int state, float x1, float y1, float x2, float y2, float width,
                                  float head, int color) {
        segment(batch, state, x1, y1, x2, y2, width, color);
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0f)
            return;

        // Two lines back from the head, at 30 degrees from the line
        float ux = dx / length, uy = dy / length;
        float cos = 0.8660254f, sin = 0.5f;
        segment(batch, state, x2, y2, x2 - head * (ux * cos - uy * sin), y2 - head * (uy * cos + ux * sin), width,
                color);
        segment(batch, state, x2, y2, x2 - head * (ux * cos + uy * sin), y2 - head * (uy * cos - ux * sin), width,
                color);
    }

    // A segment as a quad of this width (world units of the state), its ends cut square
    private static void segment(DrawBatch2D batch, int state, float x1, float y1, float x2, float y2, float width,
                                int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0f)
            return;

        float nx = -dy / length * width * 0.5f, ny = dx / length * width * 0.5f;
        batch.quad(state, 0, 0f,
                x1 + nx, y1 + ny, 0, 0,
                x1 - nx, y1 - ny, 0, 0,
                x2 - nx, y2 - ny, 0, 0,
                x2 + nx, y2 + ny, 0, 0,
                color);
    }

    // One instance of the circle program (BatchFormats.SHAPE): transparent fill, a centered stroke in screen pixels
    private void drawCircle(DrawBatch2D batch, int i, Camera camera) {
        int state = batch.state(circleMaterial, camera);
        if (state < 0 || batch.formatOf(state) != BatchFormats.SHAPE)
            return;

        float x = queue.value(i, 0), y = queue.value(i, 1), diameter = 2 * queue.value(i, 2);
        batch.beginDraw(state);
        batch.put(diameter, 0f, x);
        batch.put(0f, diameter, y);
        batch.put(diameter, diameter, 0.5f, 0.5f);
        batch.putInt(0);
        batch.putInt(queue.color(i));
        // Width, centered, screen pixels, antialiasing
        batch.put(Math.max(queue.width(i), 0f), 0.5f, 1f, 1f);
        // No margin, not textured; texture position; no parameter
        batch.put(0f, 0f);
        batch.put(0f, 1f, 0f, 1f);
        batch.put(0f, 0f, 0f, 0f);
        batch.put(0f, 0f, 0f, 0f);
        batch.endDraw();
    }

    // The characters of the text as quads of the atlas of the font, tinted by its color
    private void drawText(DrawBatch2D batch, int i, Camera camera, FontFace font) {
        int state = batch.state(textMaterial, camera);
        if (state < 0)
            return;

        float lineHeight = (float) font.getFontFile().getLineHeight();
        float scale = lineHeight > 0 ? queue.width(i) / lineHeight : 1f;
        characters.batch = batch;
        characters.state = state;
        characters.color = queue.color(i);
        TextLayout.Place(font.getFontFile(), queue.text(i), scale, queue.value(i, 0), queue.value(i, 1), characters,
                bounds, glyph);
        characters.batch = null;
    }

    // Writes the characters of a text in the batch (reused for every text)
    private static final class CharacterQuads implements TextLayout.ICharacterSink {
        DrawBatch2D batch;
        int state;
        int color;

        @Override
        public void character(float left, float right, float top, float bottom, float u0, float u1, float v0,
                              float v1) {
            batch.quad(state, 0, 0f,
                    left, top, u0, v0,
                    left, bottom, u0, v1,
                    right, bottom, u1, v1,
                    right, top, u1, v0,
                    color);
        }
    }

    /** The view is left or its window closed: the drawings are dropped, the materials released. */
    @API(status = INTERNAL)
    public void delete() {
        queue.clear();
        if (lineMaterial != null) {
            lineMaterial.release();
            circleMaterial.release();
            textMaterial.release();
            lineMaterial = circleMaterial = textMaterial = null;
            textTexture = null;
        }
    }
}
