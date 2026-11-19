in vec3 texCoords;

out vec4 FragColor;

#include "utils/colorSpace.glsl"

uniform samplerCube skybox;

void main()
{
    FragColor = textureIn(texture(skybox, texCoords));
}
