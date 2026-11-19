// Vertex shader of the vector shapes drawn on a quad (VectorShape2DRenderer): each shape has its fragment shader
layout (location = 0) in vec2 aPos;

#include "utils/shape2DVertex.glsl"

void main() {
    shapeVertex(aPos);
}
