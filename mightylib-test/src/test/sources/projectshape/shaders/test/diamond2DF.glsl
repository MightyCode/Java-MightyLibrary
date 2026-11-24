// A shape of a project (VectorShapeBatchGLTest): a diamond in the size of the shape, by its distance (scaled to the
// smaller half size: not exact, enough for a test)
#include "utils/shape2D.glsl"

void main() {
    vec2 q = abs(shapeLocal) / max(shapeHalfSize, vec2(1e-6));
    FragColor = shapeColor((q.x + q.y - 1.0) * 0.7 * min(shapeHalfSize.x, shapeHalfSize.y));
}
