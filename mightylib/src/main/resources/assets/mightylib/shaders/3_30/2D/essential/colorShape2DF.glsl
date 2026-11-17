#version 330 core
out vec4 FragColor;

uniform int color;

#include "utils/colorsManipulation.glsl"

void main() {
    FragColor = decodeColor(color);
}
