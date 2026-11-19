// TriangleRenderer: shapeData0 = first and second vertices, shapeData1.xy = third (frame of the shape)
#include "utils/shape2D.glsl"

void main() {
    FragColor = shapeColor(sdTriangle(shapeLocal, shapeData0.xy, shapeData0.zw, shapeData1.xy));
}
