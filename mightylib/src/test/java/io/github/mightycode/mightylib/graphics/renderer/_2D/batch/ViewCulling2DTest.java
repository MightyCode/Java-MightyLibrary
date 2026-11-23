package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchStateTable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchBackend;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchMaterial;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchTarget;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchUploader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

// The view culling of the batched draws without OpenGL: box of the view of a camera, boxes of the draws, margins
class ViewCulling2DTest {
    private static final class Part implements IBatchMaterial {
        @Override
        public BatchFormat format() {
            return BatchFormats.SPRITE;
        }

        @Override
        public void apply(boolean linear, Matrix4f projection, Matrix4f view) {}

        @Override
        public boolean equals(Object other) {
            return other instanceof Part;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static final class Uploader implements IBatchUploader {
        @Override
        public void upload(ByteBuffer vertices, IntBuffer indices, ByteBuffer instances) {}

        @Override
        public void draw(BatchFormat format, int firstIndex, int indexCount) {}

        @Override
        public void drawInstances(BatchFormat format, int firstByte, int count) {}
    }

    private final BatchStateTable table = new BatchStateTable();
    private int viewportWidth = 800;
    private int viewportHeight = 600;
    private boolean viewCulling = true;

    private final IBatchTarget target = new IBatchTarget() {
        private final Uploader uploader = new Uploader();

        @Override
        public void begin(DrawBatch2D batch) {}

        @Override
        public void ended(DrawBatch2D batch) {}

        @Override
        public IBatchUploader uploader() {
            return uploader;
        }

        @Override
        public BatchStateTable table() {
            return table;
        }

        @Override
        public int viewportWidth() {
            return viewportWidth;
        }

        @Override
        public int viewportHeight() {
            return viewportHeight;
        }
    };

    private DrawBatch2D batch;
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final ViewCullBounds bounds = new ViewCullBounds();

    @BeforeEach
    void create() {
        batch = new DrawBatch2D(new IBatchBackend() {
            @Override
            public IBatchTarget target() {
                return target;
            }

            @Override
            public EBatch2DMode mode() {
                return EBatch2DMode.ORPHAN;
            }

            @Override
            public boolean viewCulling() {
                return viewCulling;
            }
        });
        // Like Camera2D: a virtual size of 800 x 600, y down
        projection.setOrtho(0, 800, 600, 0, -10, 10);
        camera(0, 0, 1, 0);
    }

    // Like Camera2D.updateView
    private void camera(float x, float y, float zoom, float rotation) {
        view.identity().translate(-x, -y, 0f).scaleLocal(zoom, zoom, 1).rotate(rotation, new Vector3f(0, 0, 1));
    }

    private int state() {
        BatchStateTable stateTable = batch.stateTable();
        return stateTable.state(stateTable.materialPart(new Part()), stateTable.camera(projection, view), false);
    }

    private boolean culled(float minX, float minY, float maxX, float maxY) {
        return batch.isCulled(state(), bounds.clear(null, this).world(minX, minY, maxX, maxY));
    }

    @Test
    void outsideTheViewIsCulledTouchingItIsNot() {
        assertFalse(culled(10, 10, 20, 20));
        assertFalse(culled(790, 0, 810, 10));
        assertTrue(culled(801, 0, 810, 10));
        assertTrue(culled(-20, 0, -1, 10));
        assertTrue(culled(0, 601, 10, 700));
        assertTrue(culled(0, -50, 10, -0.5f));
        // Touching an edge or a corner: not culled
        assertFalse(culled(800, 0, 810, 10));
        assertFalse(culled(-10, -10, 0, 0));
        assertFalse(culled(800, 600, 900, 700));
        // Larger than the view
        assertFalse(culled(-1000, -1000, 1000, 1000));
    }

    @Test
    void theBoxFollowsTheCamera() {
        camera(1000, -200, 1, 0);
        assertFalse(culled(1000, -200, 1010, -190));
        assertFalse(culled(1790, 390, 1800, 400));
        assertTrue(culled(0, 0, 10, 10));
        assertTrue(culled(1801, 0, 1810, 10));
    }

    // Zoom 2 shows 400 x 300 world units; zoom 0.5 shows 1600 x 1200
    @Test
    void theZoomScalesTheBox() {
        camera(0, 0, 2, 0);
        assertFalse(culled(390, 290, 399, 299));
        assertTrue(culled(401, 0, 450, 10));
        assertTrue(culled(0, 301, 10, 350));

        camera(0, 0, 0.5f, 0);
        assertFalse(culled(1500, 1100, 1599, 1199));
        assertTrue(culled(1601, 0, 1700, 10));
    }

    // A turned camera: its box is larger than its view (nothing visible is culled), far boxes are culled
    @Test
    void aTurnedCameraNeverCullsWhatItShows() {
        float rotation = 0.6f;
        camera(300, 200, 1.5f, rotation);
        Matrix4f clip = new Matrix4f(projection).mul(view);
        Random random = new Random(7);
        int culledCount = 0;
        for (int i = 0; i < 4000; ++i) {
            float minX = random.nextFloat() * 4000 - 2000, minY = random.nextFloat() * 4000 - 2000;
            float maxX = minX + random.nextFloat() * 200, maxY = minY + random.nextFloat() * 200;
            if (!culled(minX, minY, maxX, maxY))
                continue;

            ++culledCount;
            // No point of a culled box is in the clip space
            for (int sx = 0; sx <= 10; ++sx) {
                for (int sy = 0; sy <= 10; ++sy) {
                    Vector4f point = clip.transform(new Vector4f(minX + (maxX - minX) * sx / 10f,
                            minY + (maxY - minY) * sy / 10f, 0, 1));
                    assertFalse(Math.abs(point.x) <= 1 && Math.abs(point.y) <= 1,
                            "box " + minX + " " + minY + " " + maxX + " " + maxY + " culled but visible");
                }
            }
        }

        assertTrue(culledCount > 1000, "far boxes are culled: " + culledCount);
    }

    // Perspective (Game3D2D): no box of the view, nothing culled, the draws still counted as tested
    @Test
    void aPerspectiveCullsNothing() {
        projection.setPerspective(1f, 4f / 3f, 0.1f, 100f);
        view.identity().translate(0, 0, -10);
        assertFalse(culled(10000, 10000, 10010, 10010));
        batch.endView();
        assertEquals(1, batch.lastFrameCullTested());
        assertEquals(0, batch.lastFrameCulled());
    }

    @Test
    void worldMarginsWidenTheBox() {
        int state = state();
        assertTrue(batch.isCulled(state, bounds.clear(null, this).world(810, 0, 820, 10)));
        assertFalse(batch.isCulled(state, bounds.clear(null, this).world(810, 0, 820, 10).margin(10, 0)));
        assertTrue(batch.isCulled(state, bounds.clear(null, this).world(810, 0, 820, 10).margin(9, 0)));
        // Margins add up, negative ones are ignored
        assertFalse(batch.isCulled(state, bounds.clear(null, this).world(810, 0, 820, 10).margin(5, 0)
                .margin(5, 0).margin(-100, 0)));
    }

    // Margins in pixels of the viewport: a virtual size of 800 x 600 in a viewport of 1600 x 1200, at two zooms
    @Test
    void pixelMarginsUseTheViewportOfTheBatch() {
        viewportWidth = 1600;
        viewportHeight = 1200;
        int state = state();
        // Half a world unit per pixel
        assertFalse(batch.isCulled(state, bounds.clear(null, this).world(805, 0, 810, 10).margin(0, 10)));
        assertTrue(batch.isCulled(state, bounds.clear(null, this).world(805, 0, 810, 10).margin(0, 8)));
        // Margins in world units and pixels add up
        assertFalse(batch.isCulled(state, bounds.clear(null, this).world(805, 0, 810, 10).margin(1, 8)));
        batch.endView();

        // Zoom 4: a pixel is an eighth of a world unit, the view ends at x 200
        camera(0, 0, 4, 0);
        state = state();
        assertFalse(batch.isCulled(state, bounds.clear(null, this).world(201, 0, 210, 10).margin(0, 8)));
        assertTrue(batch.isCulled(state, bounds.clear(null, this).world(201, 0, 210, 10).margin(0, 7)));
    }

    // Without a known viewport, a margin in pixels can't be measured: never culled
    @Test
    void pixelMarginsWithoutViewportCullNothing() {
        viewportWidth = 0;
        assertFalse(batch.isCulled(state(), bounds.clear(null, this).world(900, 0, 910, 10).margin(0, 1)));
        assertTrue(batch.isCulled(state(), bounds.clear(null, this).world(900, 0, 910, 10)));
    }

    // A local box is transformed by the model matrix: the box of its 4 corners
    @Test
    void aLocalBoxIsTransformedByTheModel() {
        int state = state();
        Matrix4f model = new Matrix4f().translate(900, 300, 0).rotateZ((float) Math.PI / 4);
        // Half diagonal of 50 * sqrt(2) = 70.7: from x 829.3
        assertTrue(batch.isCulled(state, bounds.clear(model, this).local(-50, -50, 50, 50)));
        model.identity().translate(860, 300, 0).rotateZ((float) Math.PI / 4);
        assertFalse(batch.isCulled(state, bounds.clear(model, this).local(-50, -50, 50, 50)));
        // Without rotation, the same box at 860 starts at 810
        model.identity().translate(860, 300, 0);
        assertTrue(batch.isCulled(state, bounds.clear(model, this).local(-50, -50, 50, 50)));
        // Scaled by 2: from x 760
        model.scale(2);
        assertFalse(batch.isCulled(state, bounds.clear(model, this).local(-50, -50, 50, 50)));
        // A world box ignores the model
        model.identity().translate(-5000, 0, 0);
        assertFalse(batch.isCulled(state, bounds.clear(model, this).world(10, 10, 20, 20)));
        // A model that is not affine in the plane: never culled
        model.setPerspective(1f, 1f, 0.1f, 10f);
        assertFalse(batch.isCulled(state, bounds.clear(model, this).local(5000, 5000, 5010, 5010)));
    }

    @Test
    void theCountersAreThoseOfTheLastFrame() {
        int state = state();
        batch.isCulled(state, bounds.clear(null, this).world(0, 0, 10, 10));
        batch.isCulled(state, bounds.clear(null, this).world(900, 0, 910, 10));
        batch.isCulled(state, bounds.clear(null, this).world(-900, 0, -890, 10));
        batch.isCulled(state, bounds.clear(null, this));
        batch.isCulled(state, null);
        assertEquals(0, batch.lastFrameCullTested());
        batch.endView();

        assertEquals(3, batch.lastFrameCullTested());
        assertEquals(2, batch.lastFrameCulled());
        assertEquals(2, batch.lastFrameCullNoBounds());

        batch.endView();
        assertEquals(0, batch.lastFrameCullTested());
        assertEquals(0, batch.lastFrameCulled());
        assertEquals(0, batch.lastFrameCullNoBounds());
    }

    // The view culling of the engine off: nothing culled, nothing counted
    @Test
    void offCullsAndCountsNothing() {
        viewCulling = false;
        int state = state();
        assertFalse(batch.isCulled(state, bounds.clear(null, this).world(900, 0, 910, 10)));
        assertFalse(batch.isCulled(state, null));
        batch.endView();
        assertEquals(0, batch.lastFrameCullTested());
        assertEquals(0, batch.lastFrameCulled());
        assertEquals(0, batch.lastFrameCullNoBounds());

        viewCulling = true;
        assertTrue(batch.isCulled(state(), bounds.clear(null, this).world(900, 0, 910, 10)));
    }

    @Test
    void theBoundsAreReusable() {
        bounds.clear(null, this).local(10, 20, 0, -5).margin(1, 2);
        assertTrue(bounds.hasBox());
        assertTrue(bounds.isLocal());
        assertEquals(0, bounds.minX());
        assertEquals(-5, bounds.minY());
        assertEquals(10, bounds.maxX());
        assertEquals(20, bounds.maxY());
        assertEquals(1, bounds.worldMargin());
        assertEquals(2, bounds.pixelMargin());
        assertSame(this, bounds.owner());

        bounds.clear(null, null);
        assertFalse(bounds.hasBox());
        assertEquals(0, bounds.worldMargin());
        assertEquals(0, bounds.pixelMargin());
        assertNull(bounds.owner());
    }

    @Test
    void aStateUnknownToTheTableIsRefused() {
        state();
        assertThrows(IllegalArgumentException.class, () -> batch.isCulled(-1, bounds.clear(null, this)));
    }
}
