// The shape alone (out of the batch): its body is arcDistance() of utils/shapeKinds2D.glsl
#include "utils/shapeKinds2D.glsl"

void main() {
    FragColor = shapeColor(arcDistance());
}
