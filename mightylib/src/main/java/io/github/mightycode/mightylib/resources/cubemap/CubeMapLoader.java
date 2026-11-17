package io.github.mightycode.mightylib.resources.cubemap;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.source.SourceEntry;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.NoSuchFileException;
import java.util.Map;

/**
 * The cubemaps: one per folder of resources/cubemaps, named after it, its faces the files right, left, top, bottom,
 * front and back (.png), found without description. A missing or bad face makes the cubemap fail, naming the face.
 */
public final class CubeMapLoader extends ResourceLoader {
    public static final String FOLDER = "cubemaps";

    @Override
    public Class<? extends DataType> getType() {
        return CubeMapData.class;
    }

    @Override
    public String getResourceNameType() {
        return "CubeMapData";
    }

    @Override
    public void create(Map<String, DataType> data) {
        // The folders of every source: a face missing in the highest source is read in a lower one
        for (SourceEntry folder : resources().sources().list(FOLDER)) {
            if (folder.isFolder())
                data.put(folder.name(), new CubeMapData(folder.name(), folder.path() + "/"));
        }
    }

    @Override
    public DataType createFallback() {
        return new FallbackCubeMapData();
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
    }

    @Override
    public String filterFile(String path) {
        return null;
    }

    @Override
    protected void initWithFile(DataType dataType) {
        CubeMapData cubeMap = (CubeMapData) dataType;
        cubeMap.freeDecoded();
        try {
            for (int i = 0; i < cubeMap.sourceCount(); ++i) {
                cubeMap.checkCancelled();
                readFace(cubeMap, i);
                cubeMap.markSourceRead(i);
            }
        } catch (RuntimeException e) {
            cubeMap.freeDecoded();
            throw e;
        }
    }

    private void readFace(CubeMapData cubeMap, int index) {
        String path = cubeMap.sourcePath(index);
        ByteBuffer file = readFile(path);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = STBImage.stbi_load_from_memory(file, width, height, channels, 4);
            if (pixels == null)
                throw new ResourceException("can't decode the face " + path + " (" + STBImage.stbi_failure_reason() + ")");

            int size = width.get(0);
            int expected = cubeMap.decodedSize();
            if (size != height.get(0) || (expected != 0 && size != expected)) {
                STBImage.stbi_image_free(pixels);
                throw new ResourceException("the face " + path + " is " + size + "x" + height.get(0)
                        + (expected != 0 ? ", the others are " + expected + "x" + expected : ", not square"));
            }

            cubeMap.setDecodedFace(index, pixels, size);
        } finally {
            MemoryUtil.memFree(file);
        }
    }

    // A native buffer, freed by the caller
    private ByteBuffer readFile(String path) {
        try {
            ByteBuffer buffer = resources().sources().readNative(path);
            if (!buffer.hasRemaining()) {
                MemoryUtil.memFree(buffer);
                throw new ResourceException("can't read the face " + path + " (size 0)");
            }

            return buffer;
        } catch (NoSuchFileException e) {
            throw new ResourceException("missing face " + path);
        } catch (IOException e) {
            throw new ResourceException("can't read the face " + path, e);
        }
    }
}
