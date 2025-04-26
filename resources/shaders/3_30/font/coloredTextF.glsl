#version 330

in vec2 texCoords;

out vec4 FragColor;

uniform int color;

uniform sampler2DRect ourTexture;

#include "colorsManipulation"

void main()
{
    FragColor = texture(ourTexture, texCoords) * decodeColor(color);

    //FragColor = vec4(0, 0, 0, 1);
}