package io.github.mightycode.mightylib.utils.json;

import org.json.JSONObject;

public abstract class JSONObjectUtil {
    public static String convertJsonObjectToStringValue(JSONObject object, String field){
        Object obj = object.get(field);
        if (obj instanceof String)
            return (String)obj;
        else if (obj instanceof Number || obj instanceof Boolean)
            return obj.toString();

        return "null";
    }
}
