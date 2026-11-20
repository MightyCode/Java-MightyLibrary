package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.DataTypeTestAccess;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.LoaderTestAccess;
import io.github.mightycode.mightylib.resources.ResourceException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Field "colorSpace" of textures.json: the encoding of a texture, read with the list (no GL)
class TextureColorSpaceTest {
    @TempDir
    Path folder;

    private TextureDataLoader loader;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(folder.resolve("textures"));
        loader = LoaderTestAccess.attached(new TextureDataLoader(), folder);
    }

    private Map<String, DataType> list(String textures) throws IOException {
        Files.write(folder.resolve("textures/textures.json"),
                ("{\"textures\":" + textures + "}").getBytes(StandardCharsets.UTF_8));
        Map<String, DataType> data = new HashMap<>();
        loader.create(data);
        return data;
    }

    private static TextureData texture(Map<String, DataType> data, String name) {
        return (TextureData) data.get(name);
    }

    @Test
    void srgbByDefaultAndLinearWhenWritten() throws IOException {
        Map<String, DataType> data = list("{"
                + "\"picture\":{\"file\":\"picture.png\",\"qualityType\":\"pixelart\"},"
                + "\"color\":{\"file\":\"color.png\",\"qualityType\":\"pixelart\",\"colorSpace\":\"srgb\"},"
                + "\"normals\":{\"file\":\"normals.png\",\"qualityType\":\"realistic\",\"colorSpace\":\"LINEAR\"}}");

        assertEquals(ETextureColorSpace.SRGB, texture(data, "picture").getColorSpace());
        assertEquals(ETextureColorSpace.SRGB, texture(data, "color").getColorSpace());
        assertEquals(ETextureColorSpace.LINEAR, texture(data, "normals").getColorSpace());
    }

    // The list is still read: only this texture fails, with one line naming the value
    @Test
    void unknownValueFailsTheTextureOnly() throws IOException {
        Map<String, DataType> data = list("{"
                + "\"wrong\":{\"file\":\"wrong.png\",\"qualityType\":\"pixelart\",\"colorSpace\":\"gamma\"},"
                + "\"right\":{\"file\":\"right.png\",\"qualityType\":\"pixelart\"}}");

        // Plus the white texture the loader always declares
        assertEquals(3, data.size());
        assertTrue(data.containsKey(EngineResources.WHITE_TEXTURE));
        ResourceException error = assertThrows(ResourceException.class, () -> loader.initWithFile(texture(data, "wrong")));
        assertTrue(error.getMessage().contains("gamma"), error.getMessage());
        assertEquals("gamma", details(texture(data, "wrong")).getString("rejectedColorSpace"));
    }

    // An image of the folder not listed: a color picture
    @Test
    void discoveredImageIsSrgb() throws IOException {
        Files.write(folder.resolve("textures/textures.json"), "{\"textures\":{}}".getBytes(StandardCharsets.UTF_8));
        Files.write(folder.resolve("textures/dropped.png"), new byte[]{1});
        Map<String, DataType> data = new HashMap<>();
        loader.discover(data);

        assertEquals(ETextureColorSpace.SRGB, texture(data, "dropped").getColorSpace());
    }

    @Test
    void describedByTheCommandResources() {
        TextureData data = new TextureData("test", "test.png");
        assertEquals("srgb", details(data).getString("colorSpace"));
        data.setColorSpace(ETextureColorSpace.LINEAR);
        assertEquals("linear", details(data).getString("colorSpace"));
        assertFalse(details(data).has("rejectedColorSpace"));
    }

    // A reload copy keeps the encoding given by the list or the code
    @Test
    void reloadCopyKeepsTheEncoding() {
        TextureData data = new TextureData("test", "test.png");
        data.setColorSpace(ETextureColorSpace.LINEAR);
        TextureData copy = (TextureData) DataTypeTestAccess.reloadCopy(data);

        assertEquals(ETextureColorSpace.LINEAR, copy.getColorSpace());
    }

    // A plain color of a data map is another texture than the same color of a picture
    @Test
    void plainColorsOfBothEncodingsAreDistinct() {
        ColorTextureData picture = new ColorTextureData(0xFF808080);
        ColorTextureData map = new ColorTextureData(0xFF808080, ETextureColorSpace.LINEAR);

        assertEquals(ETextureColorSpace.SRGB, picture.getColorSpace());
        assertEquals(ETextureColorSpace.LINEAR, map.getColorSpace());
        assertNotEquals(picture.getDataName(), map.getDataName());
        assertEquals(ColorTextureData.NameOf(0xFF808080), picture.getDataName());
    }

    @Test
    void parse() {
        assertEquals(ETextureColorSpace.SRGB, ETextureColorSpace.parse(" sRGB "));
        assertEquals(ETextureColorSpace.LINEAR, ETextureColorSpace.parse("linear"));
        assertNull(ETextureColorSpace.parse("gamma"));
        assertNull(ETextureColorSpace.parse(null));
    }

    private static JSONObject details(TextureData data) {
        JSONObject details = new JSONObject();
        DataTypeTestAccess.describeDetails(data, details);
        return details;
    }
}
