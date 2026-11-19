// PointsRenderer: one quad per point (4 vertices sharing its center); shapeParams0.x = diameter of the points
layout (location = 0) in vec2 aCenter;
layout (location = 2) in vec2 aCorner;

#include "utils/shape2DVertex.glsl"

void main() {
    shapePointVertex(aCenter, aCorner, shapeParams0.x);
}
