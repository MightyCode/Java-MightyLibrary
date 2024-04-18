#version 330 core
out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D ourTexture;
uniform sampler2D displacementMap;
uniform float time;

void main() {
    vec2 dispGrey = texture(displacementMap, vec2(TexCoord.x + time, TexCoord.y + mod(time + 0.5f, 1.0f))).xy;
    FragColor = texture(ourTexture, vec2(TexCoord.x  + ((dispGrey.x - 0.5f) * 0.2f), TexCoord.y + ((dispGrey.y - 0.5f) * 0.2f)));
   //FragColor = FragColor * vec4(0.9f, 0.5f, 0.5f, 1.0f);
}

