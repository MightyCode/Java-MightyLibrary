#version 330

in vec2 texCoords;

out vec4 FragColor;

uniform int color;

uniform sampler2DRect ourTexture;

vec4 decodeColor(int colorPacked) {
    float r = float((colorPacked >> 24) & 0xFF) / 255.0;
    float g = float((colorPacked >> 16) & 0xFF) / 255.0;
    float b = float((colorPacked >> 8) & 0xFF) / 255.0;
    float a = float(colorPacked & 0xFF) / 255.0;

    return vec4(r, g, b, a);
}


void main()
{
    FragColor = texture(ourTexture, texCoords) * decodeColor(color);

    //FragColor = vec4(0, 0, 0, 1);
}