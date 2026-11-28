package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.graphics.shader.ShaderAttribute;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// BatchFormat without GL: sizes, offsets, refusals, check of the attributes of a program
class BatchFormatTest {
    private static final BatchFormat SHAPE = BatchFormat.builder("shape")
            .instance("aRow0", 3, EBatchAttributeType.FLOAT)
            .instance("aColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            .instance("aFlags", 2, EBatchAttributeType.INT)
            .template(null, new int[]{0, 3, 2, 0, 2, 1})
            .build();

    // The sprite format: position, texture coordinates, region of the texture (4 floats at 16), color at 32
    @Test
    void theSpriteFormatIsTheFixedOne() {
        BatchFormat sprite = BatchFormats.SPRITE;
        assertEquals(36, sprite.vertexBytes());
        assertEquals(36, DrawBatch2D.VERTEX_BYTES);
        assertEquals(0, sprite.instanceBytes());
        assertFalse(sprite.hasInstances());
        assertEquals(16, sprite.attribute("inTexRegion").offset());
        assertEquals(4, sprite.attribute("inTexRegion").components());
        assertEquals(2, sprite.attribute("inTexRegion").location());
        assertEquals(32, sprite.attribute("inColor").offset());
        assertEquals(3, sprite.attribute("inColor").location());
        // Two floats, two floats, four floats, one color
        assertEquals(9, sprite.slotsPerVertex());
        assertTrue(sprite.hasSpriteValues());
    }

    // The instance of a quad: its region at 48 (one per instance), its color at 64
    @Test
    void theInstancedSpriteFormatIsTheFixedOne() {
        BatchFormat instanced = BatchFormats.SPRITE_INSTANCED;
        assertEquals(68, instanced.instanceBytes());
        assertEquals(68, DrawBatch2D.INSTANCE_BYTES);
        assertEquals(8, instanced.vertexBytes());
        assertEquals(48, instanced.attribute("aTexRegion").offset());
        assertTrue(instanced.attribute("aTexRegion").perInstance());
        assertEquals(5, instanced.attribute("aTexRegion").location());
        assertEquals(64, instanced.attribute("aColor").offset());
        assertEquals(6, instanced.attribute("aColor").location());
        assertEquals(4, instanced.templateVertexCount());
        assertArrayEquals(new int[]{0, 3, 2, 0, 2, 1}, instanced.templateIndices());
    }

    // The shapes: one instance of the values of shape2DV.glsl (model rows, place, two colors, stroke...) then the
    // kind of the shape, the points the same with their center and without kind; the common batch program of the
    // shapes of the engine has the format of the shapes, no batch program of one shape is left
    @Test
    void theShapeFormatsHoldTheValuesOfAShape() {
        assertEquals(4 * (3 + 3 + 4 + 1 + 1 + 4 + 2 + 4 + 4 + 4 + 1), BatchFormats.SHAPE.instanceBytes());
        assertEquals(124, BatchFormats.SHAPE.instanceBytes());
        List<BatchFormat.Attribute> attributes = BatchFormats.SHAPE.attributes();
        BatchFormat.Attribute last = attributes.get(attributes.size() - 1);
        assertEquals("aShapeType", last.name());
        assertEquals(EBatchAttributeType.INT, last.type());
        assertEquals(11, last.location());
        assertTrue(last.perInstance());
        assertNull(BatchFormats.POINT.attribute("aShapeType"));
        assertEquals(BatchFormats.SHAPE.instanceBytes() - 8 - 4, BatchFormats.POINT.instanceBytes());
        assertEquals(BatchFormats.SHAPE.attribute("aColor").location(), BatchFormats.POINT.attribute("aColor")
                .location());
        assertEquals(EBatchAttributeType.INT, BatchFormats.SHAPE.attribute("aStrokeColor").type());
        assertArrayEquals(new int[]{0, 3, 2, 0, 2, 1}, BatchFormats.SHAPE.templateIndices());

        assertSame(BatchFormats.SHAPE, BatchFormats.engineFormats().get("shape2DBatch"));
        for (String shape : new String[]{"roundedRectangle2D", "circle2D", "ellipse2D", "triangle2D", "arc2D",
                "regularPolygon2D", "grid2D"})
            assertFalse(BatchFormats.engineFormats().containsKey(shape + "Batch"), shape);
        assertSame(BatchFormats.POINT, BatchFormats.engineFormats().get("points2DBatch"));
    }

    // Bytes padded to 4 for the colors, ints of 4 bytes; gl_VertexID only: the vertices come from the indices
    @Test
    void sizesAndOffsetsFollowTheTypes() {
        assertEquals(12 + 4 + 8, SHAPE.instanceBytes());
        assertEquals(0, SHAPE.vertexBytes());
        assertEquals(16, SHAPE.attribute("aFlags").offset());
        assertEquals(3 + 1 + 2, SHAPE.slotsPerInstance());
        assertEquals(4, SHAPE.templateVertexCount());
    }

    @Test
    void wrongDeclarationsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> BatchFormat.builder(" "));
        assertThrows(IllegalArgumentException.class,
                () -> BatchFormat.builder("f").vertex("", 2, EBatchAttributeType.FLOAT));
        assertThrows(IllegalArgumentException.class, () -> BatchFormat.builder("f")
                .vertex("a", 2, EBatchAttributeType.FLOAT).vertex("a", 2, EBatchAttributeType.FLOAT));
        assertThrows(IllegalArgumentException.class,
                () -> BatchFormat.builder("f").vertex("a", 0, EBatchAttributeType.FLOAT));
        assertThrows(IllegalArgumentException.class,
                () -> BatchFormat.builder("f").vertex("a", 5, EBatchAttributeType.FLOAT));
        // No attribute; instances without template; a template without instances
        assertThrows(IllegalArgumentException.class, () -> BatchFormat.builder("f").build());
        assertThrows(IllegalArgumentException.class,
                () -> BatchFormat.builder("f").instance("a", 2, EBatchAttributeType.FLOAT).build());
        assertThrows(IllegalArgumentException.class, () -> BatchFormat.builder("f")
                .vertex("a", 2, EBatchAttributeType.FLOAT).template(null, new int[]{0, 1, 2}).build());
        // Template values that are not whole vertices, indices out of them, not triangles
        assertThrows(IllegalArgumentException.class, () -> BatchFormat.builder("f")
                .vertex("c", 2, EBatchAttributeType.FLOAT).instance("a", 1, EBatchAttributeType.FLOAT)
                .template(new float[]{0, 1, 2}, new int[]{0, 0, 0}).build());
        assertThrows(IllegalArgumentException.class, () -> BatchFormat.builder("f")
                .vertex("c", 2, EBatchAttributeType.FLOAT).instance("a", 1, EBatchAttributeType.FLOAT)
                .template(new float[]{0, 1}, new int[]{0, 0, 1}).build());
        assertThrows(IllegalArgumentException.class, () -> BatchFormat.builder("f")
                .instance("a", 1, EBatchAttributeType.FLOAT).template(null, new int[]{0, 1}).build());
        // A template vertex attribute that is not a float
        assertThrows(IllegalArgumentException.class, () -> BatchFormat.builder("f")
                .vertex("c", 1, EBatchAttributeType.INT).instance("a", 1, EBatchAttributeType.FLOAT)
                .template(new float[]{0, 1, 2}, new int[]{0, 1, 2}).build());
    }

    @Test
    void tooManyAttributesAreRefused() {
        BatchFormat.Builder builder = BatchFormat.builder("f");
        for (int i = 0; i < BatchFormat.MAX_ATTRIBUTES; ++i)
            builder.vertex("a" + i, 1, EBatchAttributeType.FLOAT);
        assertThrows(IllegalArgumentException.class, () -> builder.vertex("last", 1, EBatchAttributeType.FLOAT));
    }

    // The program of sprite2D as the driver reports it: read
    @Test
    void aMatchingProgramReadsTheFormat() {
        assertNull(BatchFormats.SPRITE.check(List.of(
                new ShaderAttribute("inColor", 3, ShaderAttribute.GL_FLOAT_VEC4),
                new ShaderAttribute("inTexRegion", 2, ShaderAttribute.GL_FLOAT_VEC4),
                new ShaderAttribute("aPos", 0, ShaderAttribute.GL_FLOAT_VEC2),
                new ShaderAttribute("inTexCoords", 1, ShaderAttribute.GL_FLOAT_VEC2))));
        // Unused attributes removed by the compiler, built-in inputs
        assertNull(SHAPE.check(List.of(new ShaderAttribute("gl_VertexID", -1, ShaderAttribute.GL_INT),
                new ShaderAttribute("aFlags", 2, ShaderAttribute.GL_UNSIGNED_INT_VEC2))));
    }

    @Test
    void aProgramReadingOtherwiseIsRefused() {
        // Unknown attribute, other location, other size, float for an int, int for a color
        assertNotNull(BatchFormats.SPRITE.check(List.of(new ShaderAttribute("aOther", 0,
                ShaderAttribute.GL_FLOAT_VEC2))));
        assertNotNull(BatchFormats.SPRITE.check(List.of(new ShaderAttribute("aPos", 1,
                ShaderAttribute.GL_FLOAT_VEC2))));
        assertNotNull(BatchFormats.SPRITE.check(List.of(new ShaderAttribute("aPos", 0,
                ShaderAttribute.GL_FLOAT_VEC3))));
        assertNotNull(SHAPE.check(List.of(new ShaderAttribute("aFlags", 2, ShaderAttribute.GL_FLOAT_VEC2))));
        assertNotNull(SHAPE.check(List.of(new ShaderAttribute("aColor", 1, ShaderAttribute.GL_INT_VEC4))));
        // A matrix is not an attribute of a format
        assertNotNull(SHAPE.check(List.of(new ShaderAttribute("aRow0", 0, 0x8B5B))));
    }
}
