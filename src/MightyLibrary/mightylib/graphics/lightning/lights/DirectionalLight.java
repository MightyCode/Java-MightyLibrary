package MightyLibrary.mightylib.graphics.lightning.lights;

import MightyLibrary.mightylib.graphics.lightning.materials.BasicMaterial;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import org.joml.Vector3f;

public class DirectionalLight extends BasicMaterial {
    private Vector3f direction;

    public DirectionalLight(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular){
        super(ambient, diffuse, specular);
    }

    public DirectionalLight(final BasicMaterial mat){
        super(mat.Ambient, mat.Diffuse, mat.Specular);
    }

    public DirectionalLight setDirection(Vector3f direction){
        this.direction = direction;

        return this;
    }

    public Vector3f getDirection(){
        return direction;
    }

    @Override
    public DirectionalLight clone(){
        return
                new DirectionalLight(Ambient, Diffuse, Specular).setDirection(direction);
    }

    @Override
    public void addToRenderer(Renderer renderer, String structureName) {
        super.addToRenderer(renderer, structureName);

        renderer.addShaderValue(structureName + ".direction", Vector3f.class, direction);
    }

    @Override
    public void updateRenderer(Renderer renderer, String structureName) {
        super.updateRenderer(renderer, structureName);

        renderer.updateShaderValue(structureName + ".direction", direction);
    }
}
