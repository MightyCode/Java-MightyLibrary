in vec2 texCoords;

out vec4 FragColor;

// The framebuffer of the view, sRGB: decoded to linear by the card when it is read
uniform sampler2D ourTexture;

#include "utils/colorSpace.glsl"

// Written to the window without GL_FRAMEBUFFER_SRGB: encoded here, whatever the default framebuffer supports
void main() {
    vec3 color = toSRGB(texture(ourTexture, texCoords).rgb);
    FragColor = vec4(color, 1.0);
}
