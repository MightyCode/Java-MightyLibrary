package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.graphics.shader.ShaderAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * What a batch program reads from the 2D batch: its attributes in the order of their locations (layout(location = i)
 * in GLSL for the i-th attribute), each read per vertex or per instance. Declared in Java for each batch program
 * (EngineRegistry.shaders().batchFormat), checked against the program once linked (check): a program that reads
 * otherwise is drawn alone.
 * <p>
 * A format without instance attributes: each draw writes its vertices (their attributes in order) and the indices of
 * its triangles. A format with instance attributes: each draw writes instances (their attributes in order), each
 * instance drawn as the template of the format (its vertices, whose attributes are the vertex attributes, floats
 * only, and the indices of its triangles; gl_VertexID is the index of the template vertex). Immutable.
 */
public final class BatchFormat {
    // Most attributes of a format (GL_MAX_VERTEX_ATTRIBS is at least 16)
    public static final int MAX_ATTRIBUTES = 16;

    // Kinds of the values written (slots)
    static final byte SLOT_FLOAT = 0, SLOT_INT = 1, SLOT_COLOR = 2;
    // The values of a vertex of BatchFormats.SPRITE: x, y, u, v, region (left, right, top, bottom), color
    private static final byte[] SPRITE_SLOTS = {SLOT_FLOAT, SLOT_FLOAT, SLOT_FLOAT, SLOT_FLOAT, SLOT_FLOAT, SLOT_FLOAT,
            SLOT_FLOAT, SLOT_FLOAT, SLOT_COLOR};

    /** An attribute of a format: its name in GLSL, components, type, read per instance or per vertex. */
    public static final class Attribute {
        private final String name;
        private final int components;
        private final EBatchAttributeType type;
        private final boolean perInstance;
        private final int location;
        private final int offset;

        private Attribute(String name, int components, EBatchAttributeType type, boolean perInstance, int location,
                          int offset) {
            this.name = name;
            this.components = components;
            this.type = type;
            this.perInstance = perInstance;
            this.location = location;
            this.offset = offset;
        }

        public String name() { return name; }
        public int components() { return components; }
        public EBatchAttributeType type() { return type; }
        public boolean perInstance() { return perInstance; }
        // Its index in the format: its location in the program
        public int location() { return location; }
        // Bytes from the start of its vertex (or its instance)
        public int offset() { return offset; }

        public int bytes() {
            return type.bytes(components);
        }

        @Override
        public String toString() {
            return name + " " + components + " " + type + (perInstance ? " instance" : "");
        }
    }

    private final String name;
    private final List<Attribute> attributes;
    private final int vertexBytes;
    private final int instanceBytes;
    // The values written in a vertex (or an instance), in order: their kind and their offset in bytes
    private final byte[] vertexSlots;
    private final int[] vertexSlotOffsets;
    private final byte[] instanceSlots;
    private final int[] instanceSlotOffsets;
    // Vertices of the values of BatchFormats.SPRITE (x, y, u, v, region, color), without instances:
    // DrawBatch2D.triangles
    private final boolean spriteValues;
    // Formats with instance attributes: the vertices (vertex attributes, floats) and the triangles of an instance
    private final float[] templateVertices;
    private final int[] templateIndices;
    private final int templateVertexCount;

    private BatchFormat(Builder builder) {
        name = builder.name;
        attributes = Collections.unmodifiableList(new ArrayList<>(builder.attributes));

        int vertexFloats = 0;
        int vertex = 0, instance = 0;
        int vertexSlotCount = 0, instanceSlotCount = 0;
        for (Attribute attribute : attributes) {
            int slots = attribute.type.slots(attribute.components);
            if (attribute.perInstance) {
                instance += attribute.bytes();
                instanceSlotCount += slots;
            } else {
                vertex += attribute.bytes();
                vertexSlotCount += slots;
                vertexFloats += attribute.components;
            }
        }
        vertexBytes = vertex;
        instanceBytes = instance;

        vertexSlots = new byte[vertexSlotCount];
        vertexSlotOffsets = new int[vertexSlotCount];
        instanceSlots = new byte[instanceSlotCount];
        instanceSlotOffsets = new int[instanceSlotCount];
        int v = 0, i = 0;
        for (Attribute attribute : attributes) {
            byte slot = attribute.type == EBatchAttributeType.FLOAT ? SLOT_FLOAT
                    : attribute.type == EBatchAttributeType.INT ? SLOT_INT : SLOT_COLOR;
            int slots = attribute.type.slots(attribute.components);
            for (int s = 0; s < slots; ++s) {
                if (attribute.perInstance) {
                    instanceSlots[i] = slot;
                    instanceSlotOffsets[i++] = attribute.offset + 4 * s;
                } else {
                    vertexSlots[v] = slot;
                    vertexSlotOffsets[v++] = attribute.offset + 4 * s;
                }
            }
        }
        spriteValues = instance == 0 && Arrays.equals(vertexSlots, SPRITE_SLOTS);

        if (instance > 0) {
            if (builder.templateIndices == null)
                throw new IllegalArgumentException("Batch format " + name + ": instance attributes without template");
            templateIndices = builder.templateIndices.clone();
            if (vertexFloats == 0) {
                if (builder.templateVertices != null && builder.templateVertices.length > 0)
                    throw new IllegalArgumentException("Batch format " + name
                            + ": template vertices without vertex attributes (gl_VertexID gives the vertex)");
                templateVertices = new float[0];
                int max = -1;
                for (int index : templateIndices)
                    max = Math.max(max, index);
                templateVertexCount = max + 1;
            } else {
                for (Attribute attribute : attributes) {
                    if (!attribute.perInstance && attribute.type != EBatchAttributeType.FLOAT)
                        throw new IllegalArgumentException("Batch format " + name + ": the vertex attribute "
                                + attribute.name + " of a template is not FLOAT");
                }
                float[] values = builder.templateVertices == null ? new float[0] : builder.templateVertices;
                if (values.length == 0 || values.length % vertexFloats != 0)
                    throw new IllegalArgumentException("Batch format " + name + ": " + values.length
                            + " template values for vertices of " + vertexFloats + " floats");
                templateVertices = values.clone();
                templateVertexCount = values.length / vertexFloats;
            }

            if (templateIndices.length == 0 || templateIndices.length % 3 != 0)
                throw new IllegalArgumentException("Batch format " + name + ": " + templateIndices.length
                        + " template indices, not triangles");
            for (int index : templateIndices) {
                if (index < 0 || index >= templateVertexCount)
                    throw new IllegalArgumentException("Batch format " + name + ": template index " + index
                            + " out of its " + templateVertexCount + " vertices");
            }
        } else {
            if (builder.templateIndices != null)
                throw new IllegalArgumentException("Batch format " + name + ": a template without instance attributes");
            if (vertex == 0)
                throw new IllegalArgumentException("Batch format " + name + ": no attribute");
            templateVertices = null;
            templateIndices = null;
            templateVertexCount = 0;
        }
    }

    /** A format named name (shown by the command batch), its attributes added in the order of their locations. */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    // In the order of their locations
    public List<Attribute> attributes() {
        return attributes;
    }

    // Bytes of a vertex (0 for a format whose instances read gl_VertexID only)
    public int vertexBytes() {
        return vertexBytes;
    }

    // Bytes of an instance (0 without instance attributes)
    public int instanceBytes() {
        return instanceBytes;
    }

    // Draws write instances (and no index)
    public boolean hasInstances() {
        return instanceBytes > 0;
    }

    // Values a draw writes per vertex (or per instance): a float or an int per component, one int per color
    public int slotsPerVertex() {
        return vertexSlots.length;
    }

    public int slotsPerInstance() {
        return instanceSlots.length;
    }

    // Vertices of the values of BatchFormats.SPRITE (x, y, u, v, region, color), no instance: what
    // DrawBatch2D.triangles writes
    boolean hasSpriteValues() {
        return spriteValues;
    }

    // Template of an instance (null without instance attributes): vertex attributes of each vertex, then triangles
    public float[] templateVertices() {
        return templateVertices == null ? null : templateVertices.clone();
    }

    public int[] templateIndices() {
        return templateIndices == null ? null : templateIndices.clone();
    }

    public int templateVertexCount() {
        return templateVertexCount;
    }

    // Read by the batch without copy
    byte[] slots(boolean instance) {
        return instance ? instanceSlots : vertexSlots;
    }

    int[] slotOffsets(boolean instance) {
        return instance ? instanceSlotOffsets : vertexSlotOffsets;
    }

    int templateIndexCount() {
        return templateIndices == null ? 0 : templateIndices.length;
    }

    /**
     * Null when a program with these active attributes reads this format: each active attribute is an attribute of
     * the format, at its location, with its components and a matching GLSL type (float vector for FLOAT and
     * NORMALIZED_UBYTE, int vector for INT). An attribute of the format absent from the program is not read (removed
     * by the compiler when unused): accepted. Otherwise the reason, one line.
     */
    public String check(List<ShaderAttribute> active) {
        for (ShaderAttribute attribute : active) {
            // Built-in inputs (gl_VertexID...) are not attributes of a format
            if (attribute.name().startsWith("gl_"))
                continue;

            Attribute expected = attribute(attribute.name());
            if (expected == null)
                return "its attribute " + attribute.name() + " is not in the batch format " + name;
            if (attribute.location() != expected.location)
                return "its attribute " + attribute.name() + " is at location " + attribute.location()
                        + ", the batch format " + name + " puts it at " + expected.location
                        + " (layout(location = " + expected.location + "))";
            boolean typeMatches = expected.type == EBatchAttributeType.INT ? attribute.isInt() : attribute.isFloat();
            if (!typeMatches || attribute.components() != expected.components)
                return "its attribute " + attribute.name() + " (GL type 0x" + Integer.toHexString(attribute.glType())
                        + ") does not read " + expected.components + " " + expected.type + " (batch format " + name
                        + ")";
        }

        return null;
    }

    // The attribute of this name, null when none
    public Attribute attribute(String attributeName) {
        for (Attribute attribute : attributes) {
            if (attribute.name.equals(attributeName))
                return attribute;
        }

        return null;
    }

    @Override
    public String toString() {
        return "BatchFormat[" + name + ", " + attributes + "]";
    }

    /** Builds a BatchFormat: the attributes in the order of their locations, then the template if instanced. */
    public static final class Builder {
        private final String name;
        private final List<Attribute> attributes = new ArrayList<>();
        private final Set<String> names = new HashSet<>();
        private int vertexOffset;
        private int instanceOffset;
        private float[] templateVertices;
        private int[] templateIndices;

        private Builder(String name) {
            Objects.requireNonNull(name, "name");
            if (name.trim().isEmpty())
                throw new IllegalArgumentException("A batch format has a name");
            this.name = name;
        }

        /** An attribute read per vertex, at the next location. */
        public Builder vertex(String attributeName, int components, EBatchAttributeType type) {
            return add(attributeName, components, type, false);
        }

        /** An attribute read per instance, at the next location. */
        public Builder instance(String attributeName, int components, EBatchAttributeType type) {
            return add(attributeName, components, type, true);
        }

        private Builder add(String attributeName, int components, EBatchAttributeType type, boolean perInstance) {
            Objects.requireNonNull(attributeName, "attributeName");
            Objects.requireNonNull(type, "type");
            if (attributeName.trim().isEmpty())
                throw new IllegalArgumentException("Batch format " + name + ": an attribute without name");
            if (!names.add(attributeName))
                throw new IllegalArgumentException("Batch format " + name + ": attribute " + attributeName + " twice");
            if (components < 1 || components > 4)
                throw new IllegalArgumentException("Batch format " + name + ": attribute " + attributeName + " of "
                        + components + " components (1 to 4)");
            if (attributes.size() == MAX_ATTRIBUTES)
                throw new IllegalArgumentException("Batch format " + name + ": more than " + MAX_ATTRIBUTES
                        + " attributes");

            int offset;
            if (perInstance) {
                offset = instanceOffset;
                instanceOffset += type.bytes(components);
            } else {
                offset = vertexOffset;
                vertexOffset += type.bytes(components);
            }
            attributes.add(new Attribute(attributeName, components, type, perInstance, attributes.size(), offset));
            return this;
        }

        /**
         * The geometry of each instance (formats with instance attributes): its vertices (the vertex attributes of
         * each vertex in order, floats; none when the program reads gl_VertexID only) and its triangles.
         */
        public Builder template(float[] vertexValues, int[] indices) {
            templateVertices = vertexValues == null ? null : vertexValues.clone();
            templateIndices = Objects.requireNonNull(indices, "indices").clone();
            return this;
        }

        public BatchFormat build() {
            return new BatchFormat(this);
        }
    }
}
