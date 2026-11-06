package MightyLibrary.mightylib.utils.enginecommand;

import org.json.JSONObject;

/**
 * Keep only some fields of a command answer, to make it shorter.
 */
public final class JsonSelect {
    private JsonSelect() {}

    /**
     * Fields given by dotted paths ("glObjects.Texture"), kept at the same place; unknown paths are ignored.
     */
    public static JSONObject Select(JSONObject source, String... paths) {
        JSONObject result = new JSONObject();

        for (String path : paths) {
            String[] keys = path.split("\\.");
            JSONObject from = source;
            JSONObject to = result;

            for (int i = 0; i < keys.length && from != null; ++i) {
                if (!from.has(keys[i]))
                    break;

                if (i == keys.length - 1) {
                    to.put(keys[i], from.get(keys[i]));
                } else {
                    from = from.optJSONObject(keys[i]);
                    if (!to.has(keys[i]))
                        to.put(keys[i], new JSONObject());
                    to = to.getJSONObject(keys[i]);
                }
            }
        }

        return result;
    }
}
