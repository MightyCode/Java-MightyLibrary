#version 330

in vec2 texCoords;

out vec4 FragColor;

uniform sampler2D ourTexture;
uniform sampler2D displacementMap;
uniform float time;

void main()
{
    vec2 uv = texCoords;
    float displacement = texture(displacementMap, uv).r; // Get the displacement value from the displacement map

    // Use the displacement value to distort the texture coordinates
    uv.y += (displacement - 0.5) * 0.1;
    uv.x += (displacement - 0.5) * 0.05;

    // Add some waves to the water surface by using sin() function
    float wave1 = sin(uv.y * 20.0 + time * 2.0);
    float wave2 = sin(uv.x * 10.0 - time * 1.0);
    float wave3 = sin((uv.x + uv.y) * 5.0 + time * 0.5);

    // Combine the waves and the water texture to get the final color
    vec4 waterColor = texture(ourTexture, uv + vec2(wave1, wave2) * 0.01 + vec2(wave3, wave1) * 0.005);
    FragColor = waterColor;
}