package MightyLibrary.mightylib.graphics.lightning.materials;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import org.joml.Vector3f;

public class Material extends BasicMaterial {
    public float Shininess;

    public Material(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular, final float shininess){
        super(ambient, diffuse, specular);

        this.Shininess = shininess;
    }

    @Override
    public Material clone(){
        return new Material(Ambient, Diffuse, Specular, Shininess);
    }

    @Override
    public void addToRenderer(Renderer renderer, String structureName) {
        super.addToRenderer(renderer, structureName);

        renderer.addShaderValue(structureName + ".shininess", Float.class, Shininess);
    }

    @Override
    public void updateRenderer(Renderer renderer, String structureName) {
        super.updateRenderer(renderer, structureName);

        renderer.updateShaderValue(structureName + ".shininess", Shininess);
    }

    public static Material Emerald(){
        return new Material(
            new Vector3f(0.0215f, 0.1745f, 0.0215f),
            new Vector3f(0.07568f, 0.61424f, 0.07568f),
            new Vector3f(0.633f, 0.727811f, 0.633f),
            0.6f);
    }

    public static Material Jade(){
        return new Material(
                new Vector3f(0.135f, 0.2225f, 0.1575f),
                new Vector3f(0.54f, 0.89f, 0.63f),
                new Vector3f(0.316228f, 0.316228f, 0.316228f),
                0.1f);
    }

    public static Material Obsidian(){
        return new Material(
                new Vector3f(0.05375f, 0.05f, 0.06625f),
                new Vector3f(0.18275f, 0.17f, 0.22525f),
                new Vector3f(0.332741f, 0.328634f, 0.346435f),
                0.3f);
    }

    public static Material Pearl(){
        return new Material(
                new Vector3f(0.25f, 0.20725f, 0.20725f),
                new Vector3f(1f, 0.829f, 0.829f),
                new Vector3f(0.296648f, 0.296648f, 0.296648f),
                0.088f);
    }

    public static Material Ruby(){
        return new Material(
                new Vector3f(0.1745f, 0.01175f, 0.01175f),
                new Vector3f(0.61424f, 0.04136f, 0.04136f),
                new Vector3f(0.727811f, 0.626959f, 0.626959f),
                0.6f);
    }

    public static Material Turquoise(){
        return new Material(
                new Vector3f(0.1f, 0.18725f, 0.1745f),
                new Vector3f(0.396f,0.74151f, 0.69102f),
                new Vector3f(0.297254f, 0.30829f, 0.306678f),
                0.1f);
    }

    public static Material Brass(){
        return new Material(
                new Vector3f(0.329412f, 0.223529f, 0.027451f),
                new Vector3f(0.780392f,0.568627f, 0.113725f),
                new Vector3f(0.992157f, 0.941176f, 0.807843f),
                0.21794872f);
    }

    public static Material Bronze(){
        return new Material(
                new Vector3f(0.2125f, 0.1275f, 0.054f),
                new Vector3f(0.714f,0.4284f, 0.18144f),
                new Vector3f(0.393548f, 0.271906f, 0.166721f),
                0.2f);
    }

    public static Material Chrome(){
        return new Material(
                new Vector3f(0.25f),
                new Vector3f(0.4f),
                new Vector3f(0.774597f),
                0.6f);
    }

    public static Material Copper(){
        return new Material(
                new Vector3f(0.19125f, 0.0735f, 0.0225f),
                new Vector3f(0.7038f, 0.27048f, 0.0828f),
                new Vector3f(0.256777f, 0.137622f, 0.086014f),
                0.1f);
    }

    public static Material Gold(){
        return new Material(
                new Vector3f(0.24725f,	0.1995f, 0.0745f),
                new Vector3f(0.75164f, 0.60648f, 0.22648f),
                new Vector3f(0.628281f, 0.555802f, 0.366065f),
                0.4f);
    }

    public static Material Silver(){
        return new Material(
                new Vector3f(0.19225f),
                new Vector3f(0.50754f),
                new Vector3f(0.508273f),
                0.4f);
    }

    public static final float PLASTIC_SHININESS = 0.25f;

    public static Material BlackPlastic(){
        return new Material(
                new Vector3f(0.0f),
                new Vector3f(0.01f),
                new Vector3f(0.5f),
                PLASTIC_SHININESS);
    }

    public static Material CyanPlastic(){
        return new Material(
                new Vector3f(0.0f, 0.1f, 0.06f),
                new Vector3f(0.0f,	0.50980392f,	0.50980392f),
                new Vector3f(0.50196078f),
                PLASTIC_SHININESS);
    }

    public static Material GreenPlastic(){
        return new Material(
                new Vector3f(0),
                new Vector3f(0.1f, 0.35f,	0.1f),
                new Vector3f(0.45f,	0.55f, 0.45f),
                PLASTIC_SHININESS);
    }

    public static Material RedPlastic(){
        return new Material(
                new Vector3f(0),
                new Vector3f(0.5f, 0,	0),
                new Vector3f(0.7f,0.6f,0.6f),
                PLASTIC_SHININESS);
    }

    public static Material WhitePlastic(){
        return new Material(
                new Vector3f(0),
                new Vector3f(0.55f),
                new Vector3f(0.7f),
                0.25f);
    }

    public static Material YellowPlastic(){
        return new Material(
                new Vector3f(0),
                new Vector3f(0.5f, 0.5f, 0),
                new Vector3f(0.60f, 0.60f, 0.50f),
                PLASTIC_SHININESS);
    }

    public static final float RUBBER_SHININESS = .078125f;

    public static Material BlackRubber(){
        return new Material(
                new Vector3f(0.02f),
                new Vector3f(0.1f),
                new Vector3f(0.4f),
                RUBBER_SHININESS);
    }

    public static Material CyanRubber(){
        return new Material(
                new Vector3f(0.0f, 0.05f, 0.05f),
                new Vector3f(0.4f,0.5f,0.5f),
                new Vector3f(0.04f,	0.7f,	0.7f),
                RUBBER_SHININESS);
    }

    public static Material GreenRubber(){
        return new Material(
                new Vector3f(0.0f, 0.05f, 0.0f),
                new Vector3f(0.4f,0.5f,0.4f),
                new Vector3f(0.04f,	0.7f,	0.04f),
                RUBBER_SHININESS);
    }

    public static Material RedRubber(){
        return new Material(
                new Vector3f(0.05f, 0.0f, 0.0f),
                new Vector3f(0.5f,0.4f,0.4f),
                new Vector3f(0.7f,	0.04f,	0.04f),
                RUBBER_SHININESS);
    }

    public static Material WhiteRubber(){
        return new Material(
                new Vector3f(0.05f),
                new Vector3f(0.5f),
                new Vector3f(0.7f),
                RUBBER_SHININESS);
    }

    public static Material YellowRubber(){
        return new Material(
                new Vector3f(0.05f, 0.05f, 0.0f),
                new Vector3f(0.5f, 0.5f, 0.4f),
                new Vector3f(0.7f, 0.7f, 0.04f),
                RUBBER_SHININESS);
    }


    /*
white rubber	0.05	0.05	0.05	0.5	0.5	0.5	0.7	0.7	0.7	.078125
yellow rubber	0.05	0.05	0.0	0.5	0.5	0.4	0.7	0.7	0.04	.078125
     */
}
