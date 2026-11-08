package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.Resources;
import org.joml.Vector2i;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextureDataAtlas extends TextureData {
    // Ordered list by texture size
    private List<TextureData> textures;

    // Get the texture by name
    private Map<String, TextureData> texturesMap;

    // Get the position of the texture in the atlas
    private Map<String, Vector2i> texturesPosition;
    private BufferedImage atlas;

    public TextureDataAtlas(String name, String path) {
        this(TYPE_SET_UP.THREAD_CONTEXT, name, path);
    }

    public TextureDataAtlas(TYPE_SET_UP typeSetUp, String name, String path) {
        super(typeSetUp, name, path);
        textures = new ArrayList<>();
        texturesMap = new HashMap<>();
        texturesPosition = new HashMap<>();
    }

    public TextureDataAtlas(String name) {
        this(name, null);
    }

    // Read again from its file, or built again from the same textures (atlas built by the code)
    @Override
    protected DataType createReloadCopy() {
        if (getClass() != TextureDataAtlas.class)
            return null;

        TextureDataAtlas copy = new TextureDataAtlas(getTypeSetUp(), dataName, path);
        copy.copySettingsFrom(this);
        if (!validPath()) {
            for (TextureData texture : textures) {
                copy.addDependency(texture);
                copy.addTexture(texture);
            }
            copy.setPreloaded();
        }

        return copy;
    }

    // Atlas built and owned by another data (TileMap), outside Resources: takes the content of the new atlas
    public void replaceContentWith(TextureDataAtlas newer) {
        swapContent(newer);
        markContentChanged();
    }

    // Atlas built by its owner outside Resources (no pipeline): placed and drawn now
    public boolean build() {
        setPreloaded();
        load();
        return isLoaded();
    }

    @Override
    protected void swapContent(DataType copy) {
        super.swapContent(copy);
        TextureDataAtlas other = (TextureDataAtlas) copy;

        List<TextureData> otherTextures = other.textures;
        other.textures = textures;
        textures = otherTextures;

        Map<String, TextureData> otherMap = other.texturesMap;
        other.texturesMap = texturesMap;
        texturesMap = otherMap;

        Map<String, Vector2i> otherPositions = other.texturesPosition;
        other.texturesPosition = texturesPosition;
        texturesPosition = otherPositions;

        BufferedImage otherAtlas = other.atlas;
        other.atlas = atlas;
        atlas = otherAtlas;
    }

    public void addTexture(String name) {
        TextureData texture = resourcesOrCurrent().getResource(TextureData.class, name);

        addDependency(texture);
        addTexture(texture);
    }

    public void addTexture(TextureData texture) {
        textures.add(texture);
        texturesMap.put(texture.getDataName(), texture);
        texturesPosition.put(texture.getDataName(), new Vector2i());

        if (isLoaded())
            createAtlas();
    }

    @Override
    protected boolean internLoad() {
        return createAtlas();
    }

    // A failed texture is replaced by the fallback (its sizes and pixels)
    private TextureData usable(TextureData texture) {
        return resourcesOrCurrent().usable(TextureData.class, texture);
    }

    private boolean createAtlas() {
        // Biggest first; sizes are known once the textures are loaded
        textures.sort((o1, o2) -> {
            TextureData u1 = usable(o1), u2 = usable(o2);
            return Integer.compare(u2.getWidth() * u2.getHeight(), u1.getWidth() * u1.getHeight());
        });

        placeTextures();
        defaultAspectTexture = textures.get(0).defaultAspectTexture;
        defaultTextureType = textures.get(0).defaultTextureType;

        return constructTexture();
    }

    private void placeTextures() {
        int atlasWidth = 1024;
        int currentX = 0;
        int currentY = 0;
        int maxHeightInRow = 0;

        for (TextureData texture : textures) {
            int textureWidth = usable(texture).getWidth();
            int textureHeight = usable(texture).getHeight();

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

    public boolean constructTexture(){
        // fill an array with black pixels
        atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (TextureData texture : textures) {
            Vector2i position = texturesPosition.get(texture.getDataName());

            // From the loaded pixels: no second read of the file, and the fallback has no file
            TextureData source = usable(texture);
            int[] pixels = new int[source.getWidth() * source.getHeight()];
            for (int i = 0; i < pixels.length; ++i)
                pixels[i] = source.getPixel(i);
            atlas.setRGB(position.x, position.y, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());
        }

        setBufferedImage(atlas);

        return super.internLoad();
    }

    @Override
    public BufferedImage loadBufferedImage(){
        return atlas;
    }

    public Vector2i getTexturePosition(String name) {
        return texturesPosition.get(name);
    }

    public void clear() {
        unload();

        textures.clear();
        texturesMap.clear();
        texturesPosition.clear();
    }
}
