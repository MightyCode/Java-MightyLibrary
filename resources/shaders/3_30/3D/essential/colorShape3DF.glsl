#version 330 core
out vec4 FragColor;

uniform int color;

vec4 decodeColor(int colorPacked) {
    float r = float((colorPacked >> 24) & 0xFF) / 255.0;
    float g = float((colorPacked >> 16) & 0xFF) / 255.0;
    float b = float((colorPacked >> 8) & 0xFF) / 255.0;
    float a = float(colorPacked & 0xFF) / 255.0;

    return vec4(r, g, b, a);
}

void main()
{
    FragColor = decodeColor(color);
}