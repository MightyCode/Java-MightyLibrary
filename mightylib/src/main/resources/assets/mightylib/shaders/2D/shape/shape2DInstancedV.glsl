// Batch program of the vector shapes (VectorShape2DRenderer), one instance per shape (BatchFormats.SHAPE): the values
// of shape2DV.glsl are attributes of each instance, then shapeVertex computes the quad and the outs as for a shape
// drawn alone. Each shape keeps its fragment shader.
#define SHAPE_BATCH

// Corner of the unit square of the shape (the vertices of Shape2DRenderer before the reference point)
layout (location = 0) in vec2 aCorner;
// Rows of the 2D affine model: x = dot(aModel0, (p, 1)), y = dot(aModel1, (p, 1))
layout (location = 1) in vec3 aModel0;
layout (location = 2) in vec3 aModel1;
// Size (xy) and reference point (zw)
layout (location = 3) in vec4 aSizeReference;
layout (location = 4) in int aColor;
layout (location = 5) in int aStrokeColor;
// Stroke width, alignment, unit, then antialiasing
layout (location = 6) in vec4 aStroke;
// Margin, textured (0 or 1)
layout (location = 7) in vec2 aMarginTextured;
layout (location = 8) in vec4 aTexturePosition;
layout (location = 9) in vec4 aParams0;
layout (location = 10) in vec4 aParams1;

#include "utils/shape2DInstance.glsl"

void main() {
    shapeReadInstance();
    shapeSize = aSizeReference.xy;
    shapeReference = aSizeReference.zw;
    shapeVertex(aCorner - shapeReference);
}
