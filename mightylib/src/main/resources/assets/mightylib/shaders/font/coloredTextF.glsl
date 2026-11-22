in vec2 texCoords;

out vec4 FragColor;

uniform int color;

uniform sampler2D ourTexture;

#include "utils/colorsManipulation.glsl"

void main()
{
    FragColor = texture(ourTexture, texCoords) * decodeColor(color);
}