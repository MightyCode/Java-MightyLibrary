#version 330

in vec2 outTexCoords;

out vec4 FragColor;

uniform sampler2D samplerTex;
void main()
{
    FragColor = texture(samplerTex, outTexCoords);
}