// The shape alone (out of the batch): its body is triangleDistance() of utils/shapeKinds2D.glsl
#include "utils/shapeKinds2D.glsl"

void main() {
    FragColor = shapeColor(triangleDistance());
}
