in vec2 texCoords;

out vec4 FragColor;

#include "utils/colorSpace.glsl"

uniform sampler2D ourTexture;
void main()
{
    FragColor = textureIn(texture(ourTexture, texCoords));
}