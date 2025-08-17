package MightyLibrary.mightylib.resources.models;

import java.util.HashMap;
import java.util.Map;

public class ModelTriangle {
    private final Map<String, Integer[]> verticesInfo;

    // List of info that will be saved by vertex id
    public ModelTriangle(String[] infoPerVertex) {
        verticesInfo = new HashMap<>();

        for (String info : infoPerVertex) {
            verticesInfo.put(info, new Integer[3]);
        }

        // Check if Model.TextureCoordinate is in the list
        if (!verticesInfo.containsKey(Model.TEXTURE_COORDINATE)) {
            throw new IllegalArgumentException("ModelTriangle must have a texture coordinate");
        }
    }

    public String[] getVerticesInfo() {
        return verticesInfo.keySet().toArray(new String[0]);
    }


    public void setVertexInfo(String vertexInfo, int vertexIndex, int value) {
        verticesInfo.get(vertexInfo)[vertexIndex] = value;

    }

    public ModelTriangle setAllVertexInfo(String vertexInfo, int value1, int value2, int value3) {
        Integer[] values = verticesInfo.get(vertexInfo);
        values[0] = value1;
        values[1] = value2;
        values[2] = value3;

        return this;
    }

    public HashMap<String, Integer> getRowValueOf(int vertexIndex) {
        HashMap<String, Integer> rowValues = new HashMap<>();

        for (Map.Entry<String, Integer[]> entry : verticesInfo.entrySet()) {
            rowValues.put(entry.getKey(), entry.getValue()[vertexIndex]);
        }

        return rowValues;
    }

    public boolean sameAs(ModelTriangle other) {
        for (Map.Entry<String, Integer[]> entry : verticesInfo.entrySet()) {
            Integer[] values = entry.getValue();
            Integer[] otherValues = other.verticesInfo.get(entry.getKey());

            if (values[0] != otherValues[0] || values[1] != otherValues[1] || values[2] != otherValues[2]) {
                return false;
            }
        }

        return true;
    }

    public boolean isValid() {
        for (Integer[] values : verticesInfo.values()) {
            if (values.length != 3) {
                return false;
            }

            if (values[0] == null || values[1] == null || values[2] == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer[]> entry : verticesInfo.entrySet()) {
            builder.append(entry.getKey()).append(" : ");
            for (int i = 0; i < 3; ++i) {
                builder.append(entry.getValue()[i]).append(" ");
            }
            builder.append("\n");
        }

        return builder.toString();
    }
}
