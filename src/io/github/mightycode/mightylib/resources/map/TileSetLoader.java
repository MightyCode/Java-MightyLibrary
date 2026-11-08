package io.github.mightycode.mightylib.resources.map;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.TextFormat;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.animation.AnimationData;
import org.joml.Vector2i;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class TileSetLoader extends ResourceLoader {

    @Override
    public Class<? extends DataType> getType() {
        return TileSet.class;
    }

    @Override
    public String getResourceNameType() {
        return "TileSet";
    }

    @Override
    public void create(Map<String, DataType> data){
        exploreResourcesFile(data, Resources.FOLDER + "tileset");
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new TileSet(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".tileset"))
            return getFileName(path);

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof TileSet))
            return;

        TileSet tileset = (TileSet) dataType;

        String path = tileset.path();
        String data = FileMethods.readFileAsString(path);
        String[] parts = data.split("\n");

        String texture = null;
        Vector2i tileSize = null;

        boolean parameterFlip = false;
        boolean parameterRotation = false;

        int lineIndex = 0;
        while (lineIndex < parts.length) {
            String line = parts[lineIndex].trim();
            String[] lineParts = line.split(" ");
            int lineNumber = lineIndex + 1;

            if (lineParts[0].equals("texture")) {
                texture = TextFormat.word(lineParts, 1, path, lineNumber, "texture name");
            } else if (lineParts[0].equals("tilesize")) {
                tileSize = new Vector2i(
                        TextFormat.integer(lineParts, 1, path, lineNumber, "tile width"),
                        TextFormat.integer(lineParts, 2, path, lineNumber, "tile height"));
            } else if (lineParts[0].equals("animation")) {
                int numberAnimation = TextFormat.integer(lineParts, 1, path, lineNumber, "number of animations");

                for (int i = 0; i < numberAnimation; ++i){
                    lineIndex += 1;
                    if (lineIndex >= parts.length)
                        throw TextFormat.error(path, parts.length, "expected " + numberAnimation
                                + " animations, found " + i);
                    lineNumber = lineIndex + 1;
                    lineParts = parts[lineIndex].trim().split(" ");

                    // Each information is tileId, then pairs of (tileId, duration)
                    if ((lineParts.length - 1) % 2 != 0)
                        throw TextFormat.error(path, lineNumber, "an animation is a tile id then (tile id, duration) pairs");

                    TileSet.TileAnimation tileAnimation = new TileSet.TileAnimation(
                            TextFormat.integer(lineParts, 0, path, lineNumber, "animated tile id"),
                            (lineParts.length - 1) / 2
                    );

                    for (int j = 1; j < lineParts.length; j += 2){
                        tileAnimation.ids[j / 2] = TextFormat.integer(lineParts, j, path, lineNumber, "frame tile id");
                        tileAnimation.times[j / 2] = TextFormat.decimal(lineParts, j + 1, path, lineNumber, "frame duration");
                    }

                    tileset.addAnimation(tileAnimation.refId, tileAnimation);
                }
            } else if (lineParts[0].equals("parameter")){
                TextFormat.word(lineParts, 2, path, lineNumber, "rotation and flip flags");
                parameterRotation = Boolean.parseBoolean(lineParts[1]);
                parameterFlip = Boolean.parseBoolean(lineParts[2]);
            }

            ++lineIndex;
        }

        if (texture == null || tileSize == null)
            return;

        tileset.setTileSize(texture, tileSize);

        tileset.setTileParameters(parameterRotation, parameterFlip);
    }
}
