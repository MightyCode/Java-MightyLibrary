#version 330 core
out vec4 FragColor;

uniform int color;

#include "colorsManipulation"

void main()
{
    FragColor = decodeColor(color);
}