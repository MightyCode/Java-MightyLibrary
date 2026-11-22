// Geometric shapes (PolylineRenderer, ArrowRenderer, PolygonRenderer): triangles made by Stroker and fills, extruded
// by utils/stroke2DVertex.glsl
layout (location = 0) in vec2 aBase;
layout (location = 2) in vec4 aExtrusion;
layout (location = 3) in vec4 aCoverage;
layout (location = 4) in vec2 aPaint;

#include "utils/stroke2DVertex.glsl"

void main() {
    strokeVertex(aBase, aExtrusion, aCoverage, aPaint);
}
