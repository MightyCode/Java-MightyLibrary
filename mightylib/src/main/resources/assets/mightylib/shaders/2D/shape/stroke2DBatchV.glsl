// Batch program of the geometric shapes (BatchFormats.STROKE): the attributes of stroke2DV.glsl, then the values of
// the shape given per vertex (no uniform of its own: shapes of other widths and colors share a draw call), copied
// into the globals of utils/shape2DVertex.glsl; the vertex is then computed as for a shape drawn alone
#define SHAPE_BATCH

layout (location = 0) in vec2 aBase;
layout (location = 1) in vec4 aExtrusion;
layout (location = 2) in vec4 aCoverage;
layout (location = 3) in vec2 aPaint;
// Rows of the 2D affine model
layout (location = 4) in vec3 aModel0;
layout (location = 5) in vec3 aModel1;
layout (location = 6) in vec2 aSize;
layout (location = 7) in int aColor;
layout (location = 8) in int aStrokeColor;
// Stroke width, unit, antialiasing, textured (0 or 1)
layout (location = 9) in vec4 aStroke;
layout (location = 10) in vec4 aTexturePosition;
// Bounding box of the shape (shapeParams0)
layout (location = 11) in vec4 aBounds;

#include "utils/stroke2DVertex.glsl"

void main() {
    model = mat4(aModel0.x, aModel1.x, 0.0, 0.0,
                 aModel0.y, aModel1.y, 0.0, 0.0,
                 0.0, 0.0, 1.0, 0.0,
                 aModel0.z, aModel1.z, 0.0, 1.0);
    shapeSize = aSize;
    shapeReference = vec2(0.0);
    color = aColor;
    strokeColor = aStrokeColor;
    strokeWidth = aStroke.x;
    // Always centered on the line (not read by stroke2DF.glsl)
    strokeAlignment = 0.5;
    strokeUnit = aStroke.y;
    antialiasing = aStroke.z;
    shapeMargin = 0.0;
    shapeTextured = int(aStroke.w);
    shapeTexturePosition = aTexturePosition;
    shapeParams0 = aBounds;
    shapeParams1 = vec4(0.0);

    strokeVertex(aBase, aExtrusion, aCoverage, aPaint);
}
