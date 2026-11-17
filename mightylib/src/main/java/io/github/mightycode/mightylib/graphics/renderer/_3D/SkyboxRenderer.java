package io.github.mightycode.mightylib.graphics.renderer._3D;

import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.cubemap.CubeMapData;
import io.github.mightycode.mightylib.resources.cubemap.CubeMapTexture;

/**
 * The sky around the 3D camera: a cube seen from inside, textured by a cubemap (CubeMapData, listed by a batch of the
 * scene), drawn first without depth test nor depth write, so everything drawn after covers it. The program
 * EngineResources.SKYBOX keeps only the rotation of the view: the sky does not move with the camera. While the
 * cubemap is not loaded, its fallback (checkerboards) is shown.
 */
public final class SkyboxRenderer extends MeshRenderer {
    private static final String SAMPLER = "skybox";

    private final CubeMapTexture cubeMap;
    private boolean released;

    public SkyboxRenderer(String cubeMapName) {
        this(cubeMapName, EngineResources.SKYBOX);
    }

    public SkyboxRenderer(String cubeMapName, String shaderName) {
        super(shaderName, false);

        getMesh().addAllVbo(cubeVertices(), new int[]{3}, Mesh.STATIC_STORE);
        cubeMap = (CubeMapTexture) glResources().acquire(
                resources().getResourceOrFallback(CubeMapData.class, cubeMapName), true);

        addShaderValue(SAMPLER, Integer.class, 0);
        setDepthTest(false);
        setDepthWrite(false);
        setFaceCulling(false);
    }

    public CubeMapTexture getCubeMap() {
        return cubeMap;
    }

    @Override
    public void updateShader() {
        cubeMap.bind(0);
        super.updateShader();
    }

    @Override
    public void unload() {
        if (!released) {
            glResources().release(cubeMap, true);
            released = true;
        }

        super.unload();
    }

    // Two triangles per face of the cube from -1 to 1
    private static float[] cubeVertices() {
        return new float[]{
                -1,  1, -1,  -1, -1, -1,   1, -1, -1,   1, -1, -1,   1,  1, -1,  -1,  1, -1,
                -1, -1,  1,  -1, -1, -1,  -1,  1, -1,  -1,  1, -1,  -1,  1,  1,  -1, -1,  1,
                 1, -1, -1,   1, -1,  1,   1,  1,  1,   1,  1,  1,   1,  1, -1,   1, -1, -1,
                -1, -1,  1,  -1,  1,  1,   1,  1,  1,   1,  1,  1,   1, -1,  1,  -1, -1,  1,
                -1,  1, -1,   1,  1, -1,   1,  1,  1,   1,  1,  1,  -1,  1,  1,  -1,  1, -1,
                -1, -1, -1,  -1, -1,  1,   1, -1, -1,   1, -1, -1,  -1, -1,  1,   1, -1,  1
        };
    }
}
