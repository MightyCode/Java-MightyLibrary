// The grid alone (out of the batch): its body is gridColor() of utils/shapeKinds2D.glsl
#include "utils/shapeKinds2D.glsl"

void main() {
    FragColor = gridColor(shapeFragmentPixel(), shapeFillColor());
}
