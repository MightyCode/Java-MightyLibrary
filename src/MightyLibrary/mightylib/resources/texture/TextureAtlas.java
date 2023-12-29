package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.Texture;
import org.joml.Vector2i;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextureAtlas extends Texture {
    // Ordered list by texture size
    private final List<Texture> textures;
    // Get the texture by name
    private final Map<String, Texture> texturesMap;

    // Get the position of the texture in the atlas
    private final Map<String, Vector2i> texturesPosition;;
    private BufferedImage atlas;

    public TextureAtlas(String name) {
        super(name, null);
        textures = new ArrayList<>();
        texturesMap = new HashMap<>();
        texturesPosition = new HashMap<>();
    }

    public void addTexture(String name) {
        addTexture(Resources.getInstance().getResource(Texture.class, name));
    }

    public void addTexture(Texture texture) {
        textures.add(texture);
        texturesMap.put(texture.getDataName(), texture);
        texturesPosition.put(texture.getDataName(), new Vector2i());

        // Sort the list by texture size (width * height)
        textures.sort((o1, o2) -> {
            int size1 = o1.getWidth() * o1.getHeight();
            int size2 = o2.getWidth() * o2.getHeight();

            if (size1 > size2) return -1;
            else if (size1 < size2) return 1;
            else return 0;
        });

        createAtlas();
    }

    private void createAtlas() {
        placeTextures();
        constructTexture();

        setAspectTexture(textures.get(0).getQualityType());
        TextureParameters.applyParameters(this);
    }

    private void placeTextures() {
        int atlasWidth = 1024;
        int currentX = 0;
        int currentY = 0;
        int maxHeightInRow = 0;

        for (Texture texture : textures) {
            int textureWidth = texture.getWidth();
            int textureHeight = texture.getHeight();

            // If the texture exceeds the remaining width, move to the next row
            if (currentX + textureWidth > atlasWidth) {
                currentX = 0;
                currentY += maxHeightInRow;
                maxHeightInRow = 0;
            }

            // Set the position of the texture in the atlas
            texturesPosition.get(texture.getDataName()).set(currentX, currentY);

            // Update the maximum height in the current row
            if (textureHeight > maxHeightInRow) {
                maxHeightInRow = textureHeight;
            }

            // Update the X coordinate for the next texture
            currentX += textureWidth;
        }

        width = atlasWidth;
        height = currentY + maxHeightInRow;
    }

    public void constructTexture(){
        super.unload();
        // fill an array with black pixels
        atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = atlas.createGraphics();

        for (Texture texture : textures) {
            Vector2i position = texturesPosition.get(texture.getDataName());

            g2d.drawImage(texture.loadBufferedImage(), position.x, position.y, null);
        }

        createImage(atlas);
    }

    @Override
    public BufferedImage loadBufferedImage(){
        return atlas;
    }

    public void clear(){
        unload();

        textures.clear();
        texturesMap.clear();
        texturesPosition.clear();
    }

    public Vector2i getTexturePosition(String name) {
        return texturesPosition.get(name);
    }
}
