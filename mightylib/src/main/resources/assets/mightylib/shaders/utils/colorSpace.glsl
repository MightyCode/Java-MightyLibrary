// Color space of a draw. The textures of colors are stored in sRGB (the card decodes them to linear when they are
// read) and the framebuffer of a view is sRGB. mlLinearColors (sent by the Renderer for each draw):
// - true, linear draw: GL_FRAMEBUFFER_SRGB is enabled, the colors given in sRGB (uniforms, vertices) are decoded,
//   lighting and blending are done in linear, the card encodes the result;
// - false, gamma draw (a shader without the uniform too): what is read from a texture of colors is encoded again,
//   everything is computed and blended on the sRGB values, as written.
// Replaceable by a higher source (game, mod): keep the names of the functions.
uniform bool mlLinearColors;

// Exact sRGB curve
vec3 toLinear(vec3 color) {
    vec3 low = color / 12.92;
    vec3 high = pow((color + 0.055) / 1.055, vec3(2.4));
    return mix(high, low, vec3(lessThanEqual(color, vec3(0.04045))));
}

vec3 toSRGB(vec3 color) {
    color = max(color, vec3(0.0));
    vec3 low = color * 12.92;
    vec3 high = 1.055 * pow(color, vec3(1.0 / 2.4)) - 0.055;
    return mix(high, low, vec3(lessThanEqual(color, vec3(0.0031308))));
}

// A color given in sRGB (uniform, vertex, packed color), in the color space of the draw
vec4 colorIn(vec4 color) {
    return mlLinearColors ? vec4(toLinear(color.rgb), color.a) : color;
}

vec3 colorIn(vec3 color) {
    return mlLinearColors ? toLinear(color) : color;
}

// A value read from a texture of colors (decoded to linear by the card), in the color space of the draw.
// A data map (specular, normals, displacement, font coverage) uses neither of them.
vec4 textureIn(vec4 texel) {
    return mlLinearColors ? texel : vec4(toSRGB(texel.rgb), texel.a);
}
