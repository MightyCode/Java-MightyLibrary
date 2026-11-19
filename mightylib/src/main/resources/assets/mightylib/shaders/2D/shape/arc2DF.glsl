// ArcRenderer: shapeData0 = inner radius ratio (of the outer one), start angle, sweep (radians); outer radius: the
// largest circle in the size of the shape
#include "utils/shape2D.glsl"

void main() {
    float outer = min(shapeHalfSize.x, shapeHalfSize.y);
    FragColor = shapeColor(sdAnnularSector(shapeLocal, outer * clamp(shapeData0.x, 0.0, 1.0), outer,
            shapeData0.y, shapeData0.z));
}
