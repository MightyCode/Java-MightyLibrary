in vec2 texCoords;
flat in vec4 texRegion;

out vec4 FragColor;

#include "utils/colorSpace.glsl"
#include "utils/textureRegion.glsl"

uniform sampler2D ourTexture;
void main()
{
    FragColor = textureIn(texture(ourTexture, regionTexCoords(ourTexture, texCoords, texRegion)));
}
