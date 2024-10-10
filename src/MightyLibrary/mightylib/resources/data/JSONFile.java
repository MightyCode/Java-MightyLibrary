package MightyLibrary.mightylib.resources.data;

import MightyLibrary.mightylib.resources.SingleSourceDataType;
import org.json.JSONObject;

public class JSONFile extends SingleSourceDataType {
    protected JSONObject object;
    public JSONFile(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);
    }


    public JSONFile (TYPE_SET_UP typeSetUp, String dataName, String path){
        super(typeSetUp, dataName, path);
    }

    public void setJsonObject(JSONObject object){
        this.object = object;
    }

    @Override
    protected boolean internLoad(){
        return object != null && !object.isEmpty();
    }

    public JSONObject getObject() {
        return object;
    }

    @Override
    protected void internUnload() {
        object = null;
    }
}
