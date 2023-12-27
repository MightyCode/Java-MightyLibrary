package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import org.joml.Vector2i;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class TileSetLoader extends ResourceLoader {

    @Override
    public Class<?> getType() {
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
    public void load(DataType dataType) {
        if (!(dataType instanceof TileSet))
            return;

        TileSet tileset = (TileSet) dataType;

        String data = FileMethods.readFileAsString(tileset.getPath());
        String[] parts = data.split("\n");

        String texture = null;
        Vector2i tileSize = null;

        boolean parameterFlip = false;
        boolean parameterRotation = false;

        int lineIndex = 0;
        while (lineIndex < parts.length) {
            String line = parts[lineIndex].trim();
            String[] lineParts = line.split(" ");

            if (lineParts[0].equals("texture")) {
                texture = lineParts[1];
            } else if (lineParts[0].equals("tilesize")) {
                tileSize = new Vector2i(Integer.parseInt(lineParts[1]), Integer.parseInt(lineParts[2]));
            } else if (lineParts[0].equals("animation")) {
                int numberAnimation = Integer.parseInt(lineParts[1]);

                for (int i = 0; i < numberAnimation; ++i){
                    lineIndex += 1;
                    lineParts = parts[lineIndex].trim().split(" ");

                    // Each information is tileId, duration
                    TileSet.TileAnimation tileAnimation = new TileSet.TileAnimation(
                            Integer.parseInt(lineParts[0]),
                            (lineParts.length - 1) / 2
                    );

                    for (int j = 1; j < lineParts.length; j += 2){
                        tileAnimation.ids[j / 2] = Integer.parseInt(lineParts[j]);
                        tileAnimation.times[j / 2] = Float.parseFloat(lineParts[j + 1]);
                    }

                    tileset.addAnimation(tileAnimation.refId, tileAnimation);
                }
            } else if (lineParts[0].equals("parameter")){
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

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        // Todo
    }
}
