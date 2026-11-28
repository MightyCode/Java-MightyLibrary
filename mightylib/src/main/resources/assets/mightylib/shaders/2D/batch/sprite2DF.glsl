// Texture x color of the vertex: a colored draw uses the white texture of the engine (textureIn(white) = white),
// a textured one the white color, so both give what texture2D and colorShape2D give
in vec2 texCoords;
flat in vec4 texRegion;
in vec4 vertexColor;

out vec4 FragColor;

#include "utils/colorSpace.glsl"
#include "utils/textureRegion.glsl"

uniform sampler2D ourTexture;

void main()
{
    FragColor = textureIn(texture(ourTexture, regionTexCoords(ourTexture, texCoords, texRegion)))
            * colorIn(vertexColor);
}
