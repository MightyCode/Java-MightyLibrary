package MightyLibrary.mightylib.utils.json;

import org.json.JSONObject;

public abstract class JSONObjectUtil {
    public static String convertJsonObjectToStringValue(JSONObject object, String field){
        Object obj = object.get(field);
        if (obj instanceof String)
            return (String)obj;
        else if (obj instanceof Integer)
            return String.valueOf((int)obj);
        else if (obj instanceof Boolean)
            return String.valueOf((boolean) obj);
        else if (obj instanceof Float)
            return String.valueOf((float) obj);
        else if (obj instanceof Double)
            return String.valueOf((double) obj);

        return "null";
    }
}
