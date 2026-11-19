out vec4 FragColor;

#include "utils/colorSpace.glsl"

in vec2 texCoord;

uniform sampler2D ourTexture;

void main() {
    FragColor = textureIn(texture(ourTexture, texCoord));
}
