// Batch program of PointsRenderer, one instance per point (BatchFormats.POINT): the values of points2DV.glsl are
// attributes of each instance (the center of the point instead of the size and reference point of a shape)
#define SHAPE_BATCH

// Corner of the quad of the point, -1 or 1 on each axis
layout (location = 0) in vec2 aCorner;
layout (location = 1) in vec3 aModel0;
layout (location = 2) in vec3 aModel1;
// Center of the point, in the frame of the renderer
layout (location = 3) in vec2 aCenter;
layout (location = 4) in int aColor;
layout (location = 5) in int aStrokeColor;
layout (location = 6) in vec4 aStroke;
layout (location = 7) in vec2 aMarginTextured;
layout (location = 8) in vec4 aTexturePosition;
layout (location = 9) in vec4 aParams0;
layout (location = 10) in vec4 aParams1;

#include "utils/shape2DInstance.glsl"

void main() {
    shapeReadInstance();
    shapePointVertex(aCenter, aCorner, shapeParams0.x);
}
