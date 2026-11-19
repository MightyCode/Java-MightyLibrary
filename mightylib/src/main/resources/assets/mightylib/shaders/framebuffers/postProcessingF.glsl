in vec2 texCoords;

out vec4 FragColor;

// The framebuffer of the view, sRGB: decoded to linear by the card when it is read
uniform sampler2D ourTexture;

// Settings of the picture (PictureSettings), 1 = unchanged
uniform float mlGamma;
uniform float mlBrightness;
uniform float mlContrast;

#include "utils/colorSpace.glsl"

// Written to the window without GL_FRAMEBUFFER_SRGB: encoded here, whatever the default framebuffer supports, then
// the settings are applied to the encoded values
void main() {
    vec3 color = toSRGB(texture(ourTexture, texCoords).rgb);
    color = (color - 0.5) * mlContrast + 0.5;
    color = color * mlBrightness;
    color = pow(clamp(color, 0.0, 1.0), vec3(1.0 / mlGamma));
    FragColor = vec4(color, 1.0);
}
