package MightyLibrary.mightylib.resources.models;

import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.sound.SoundData;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjModelLoader extends ResourceLoader {
    @Override
    public Class<? extends DataType> getType() {
        return Model.class;
    }

    @Override
    public String getResourceNameType() {
        return "Model";
    }

    @Override
    public void create(Map<String, DataType> data) {
        exploreResourcesFile(data, Resources.FOLDER);
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new Model(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".obj"))
            return getFileName(path);

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof Model))
            return;

        Model model = (Model) dataType;
        String file = FileMethods.readFileAsString(model.path());
        String[] lines = file.split("\n");

        for (String line : lines) {
            String[] parsedLine = line.split(" ");

            if (line.startsWith("v ")){
                model.addVertexPosition(new Vector3f(Float.parseFloat(parsedLine[1]),
                        Float.parseFloat(parsedLine[2]), Float.parseFloat(parsedLine[3])));
            } else if (line.startsWith("vt")){
                // Invert the y coordinate because the texture is flipped
                model.addTextureCoordinate(new Vector2f(Float.parseFloat(parsedLine[1]),
                       1 - Float.parseFloat(parsedLine[2])));
            } else if (line.startsWith("vn")){
                model.addNormalVector(new Vector3f(Float.parseFloat(parsedLine[1]),
                        Float.parseFloat(parsedLine[2]), Float.parseFloat(parsedLine[3])));

            } else if (line.startsWith("f ")) {
                ModelTriangle modelTriangle = getModelTriangle(parsedLine);

                model.addFace(modelTriangle);
            }
        }
    }

    private static ModelTriangle getModelTriangle(String[] parsedLine) {
        ModelTriangle modelTriangle = new ModelTriangle(new String[]{Model.VERTEX_POSITION, Model.TEXTURE_COORDINATE, Model.NORMAL_VECTOR});

        for (int i = 0; i < 3; ++i) {
            // + 1 because the first element is the "f" character
            String[] vertexData = parsedLine[i + 1].split("/");

            modelTriangle.setVertexInfo(Model.VERTEX_POSITION, i, Integer.parseInt(vertexData[0]) - 1);
            modelTriangle.setVertexInfo(Model.TEXTURE_COORDINATE, i, Integer.parseInt(vertexData[1]) - 1);
            modelTriangle.setVertexInfo(Model.NORMAL_VECTOR, i, Integer.parseInt(vertexData[2]) - 1);
        }

        return modelTriangle;
    }
}
