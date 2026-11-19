// PointsRenderer: shapeData0.y = 0 for round points, 1 for square ones
#include "utils/shape2D.glsl"

void main() {
    float d = shapeData0.y > 0.5 ? sdRoundedBox(shapeLocal, shapeHalfSize, vec4(0.0))
            : sdCircle(shapeLocal, shapeHalfSize.x);
    FragColor = shapeColor(d);
}
