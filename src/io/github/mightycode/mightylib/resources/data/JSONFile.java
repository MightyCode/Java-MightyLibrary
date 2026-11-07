package io.github.mightycode.mightylib.resources.data;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;
import org.json.JSONObject;

public class JSONFile extends SingleSourceDataType {
    protected JSONObject object;
    public JSONFile(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);
    }


    public JSONFile (TYPE_SET_UP typeSetUp, String dataName, String path){
        super(typeSetUp, dataName, path);
    }

    @Override
    protected DataType createReloadCopy() {
        return getClass() == JSONFile.class ? new JSONFile(getTypeSetUp(), dataName, path) : null;
    }

    @Override
    protected void swapContent(DataType copy) {
        JSONFile other = (JSONFile) copy;
        JSONObject otherObject = other.object;
        other.object = object;
        object = otherObject;
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
