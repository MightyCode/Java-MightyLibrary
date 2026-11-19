package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The stroke of the vector shapes and the margin of their quad (the same formula as utils/shape2DVertex.glsl)
class ShapeStrokeTest {
    private static final float EPSILON = 1e-6f;

    @Test
    void defaultsAreNoStrokeCenteredInWorldUnits() {
        ShapeStroke stroke = new ShapeStroke();
        assertEquals(0f, stroke.width());
        assertEquals(EStrokeAlignment.CENTERED, stroke.alignment());
        assertEquals(EStrokeUnit.WORLD, stroke.unit());
        assertEquals(1f, stroke.color().getA(), EPSILON);
    }

    @Test
    void theAlignmentGivesThePartOutsideTheEdge() {
        assertEquals(0f, EStrokeAlignment.INNER.outsideFraction());
        assertEquals(0.5f, EStrokeAlignment.CENTERED.outsideFraction());
        assertEquals(1f, EStrokeAlignment.OUTER.outsideFraction());
    }

    @Test
    void theMarginCoversTheOuterPartOfTheStrokeAndOnePixel() {
        ShapeStroke stroke = new ShapeStroke();
        stroke.setWidth(10f);

        // Two world units per pixel (zoomed out)
        stroke.setAlignment(EStrokeAlignment.INNER);
        assertEquals(2f, stroke.quadMargin(2f, true, 0f), EPSILON);
        stroke.setAlignment(EStrokeAlignment.CENTERED);
        assertEquals(7f, stroke.quadMargin(2f, true, 0f), EPSILON);
        stroke.setAlignment(EStrokeAlignment.OUTER);
        assertEquals(12f, stroke.quadMargin(2f, true, 0f), EPSILON);

        // Without anti-aliasing, no pixel; the extra margin of the shape is added, a negative one ignored
        assertEquals(10f, stroke.quadMargin(2f, false, 0f), EPSILON);
        assertEquals(13f, stroke.quadMargin(2f, false, 3f), EPSILON);
        assertEquals(10f, stroke.quadMargin(2f, false, -3f), EPSILON);
    }

    @Test
    void aWidthInScreenPixelsFollowsTheSizeOfAPixel() {
        ShapeStroke stroke = new ShapeStroke();
        stroke.setWidth(3f);
        stroke.setUnit(EStrokeUnit.SCREEN_PIXELS);

        // Zoomed in four times: a pixel is a quarter of a world unit
        assertEquals(0.75f, stroke.worldWidth(0.25f), EPSILON);
        assertEquals(0.375f, stroke.outerOffset(0.25f), EPSILON);
        assertEquals(0.625f, stroke.quadMargin(0.25f, true, 0f), EPSILON);

        stroke.setUnit(EStrokeUnit.WORLD);
        assertEquals(3f, stroke.worldWidth(0.25f), EPSILON);
    }

    @Test
    void aNegativeWidthIsNoStrokeAndNullsAreRefused() {
        ShapeStroke stroke = new ShapeStroke();
        stroke.setWidth(-4f);
        assertEquals(0f, stroke.width());
        assertEquals(1f, stroke.quadMargin(1f, true, 0f), EPSILON);

        assertThrows(IllegalArgumentException.class, () -> stroke.setColor(null));
        assertThrows(IllegalArgumentException.class, () -> stroke.setAlignment(null));
        assertThrows(IllegalArgumentException.class, () -> stroke.setUnit(null));
    }
}
