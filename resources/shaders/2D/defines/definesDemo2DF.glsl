out vec4 FragColor;

uniform int color;

#include "utils/colorsManipulation.glsl"

// Read by two entries of shaders.json (ShaderDefines2D): TINT is given by the entry definesDemoTinted2D only,
// DEMO_INVERT is a global define (key D of the scene, or the command shaders define DEMO_INVERT)
void main() {
    vec4 base = decodeColor(color);
    // Stripes, the same for every entry
    float stripe = step(0.5, fract((gl_FragCoord.x + gl_FragCoord.y) / 40.0));
    vec3 rgb = mix(base.rgb, base.rgb * 0.6, stripe);

#ifdef TINT
    rgb *= TINT;
#endif

#ifdef DEMO_INVERT
    rgb = vec3(1.0) - rgb;
#endif

    FragColor = vec4(rgb, base.a);
}
