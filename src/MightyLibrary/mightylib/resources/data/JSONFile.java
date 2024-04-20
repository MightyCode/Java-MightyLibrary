package MightyLibrary.mightylib.resources.data;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.SingleSourceDataType;
import org.json.JSONObject;

public class JSONFile extends SingleSourceDataType {
    private JSONObject object;
    public JSONFile(String dataName, String path) {
        super(dataName, path);
    }

    void init(JSONObject object){
        this.object = object;

        correctlyLoaded = true;
    }

    public JSONObject getObject() {
        return object;
    }

    @Override
    public void unload() {
        correctlyLoaded = false;
    }
}
