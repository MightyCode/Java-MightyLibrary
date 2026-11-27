// Region of a texture a draw is limited to: left, right, top, bottom in texture coordinates (always min, max, even
// when the draw is flipped). No limit when right <= left (the default value (0, 0, 0, 1) of an attribute not given).
// The coordinates are kept half a texel inside the region: the outer rows and columns of the region are read whole,
// never a texel of a neighbor region (frames of a sheet, tiles of a tileset that touch each other).
// Exact for GL_NEAREST and for GL_LINEAR without mipmaps (the edge is extended, like GL_CLAMP_TO_EDGE).
vec2 regionTexCoords(sampler2D textureToRead, vec2 uv, vec4 region)
{
    if (region.y <= region.x)
        return uv;

    vec2 halfTexel = 0.5f / vec2(textureSize(textureToRead, 0));
    vec2 regionMin = region.xz, regionMax = region.yw;
    // A region thinner than a texel keeps its center (never a clamp with a min above its max)
    vec2 center = 0.5f * (regionMin + regionMax);
    return clamp(uv, min(regionMin + halfTexel, center), max(regionMax - halfTexel, center));
}
