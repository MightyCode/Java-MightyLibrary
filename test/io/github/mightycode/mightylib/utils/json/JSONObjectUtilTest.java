package io.github.mightycode.mightylib.utils.json;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JSONObjectUtilTest {
    private static String text(String json, String field) {
        return JSONObjectUtil.convertJsonObjectToStringValue(new JSONObject(json), field);
    }

    @Test
    void stringsAreReturnedAsTheyAre() {
        assertEquals("hello", text("{\"a\":\"hello\"}", "a"));
        assertEquals("", text("{\"a\":\"\"}", "a"));
    }

    @Test
    void numbersAndBooleansAreWrittenAsText() {
        assertEquals("42", text("{\"a\":42}", "a"));
        assertEquals("-7", text("{\"a\":-7}", "a"));
        assertEquals("true", text("{\"a\":true}", "a"));
        assertEquals("false", text("{\"a\":false}", "a"));
    }

    @Test
    void decimalNumbersOfAFile() {
        assertEquals("2.5", text("{\"a\":2.5}", "a"));
    }

    @Test
    void floatsAndDoublesPutByCode() {
        JSONObject object = new JSONObject();
        object.put("float", 1.5f);
        object.put("double", 0.25);
        assertEquals("1.5", JSONObjectUtil.convertJsonObjectToStringValue(object, "float"));
        assertEquals("0.25", JSONObjectUtil.convertJsonObjectToStringValue(object, "double"));
    }

    @Test
    void otherTypesGiveTheWordNull() {
        assertEquals("null", text("{\"a\":null}", "a"));
        assertEquals("null", text("{\"a\":[1,2]}", "a"));
        assertEquals("null", text("{\"a\":{\"b\":1}}", "a"));
    }

    @Test
    void aMissingFieldIsAnError() {
        assertThrows(JSONException.class, () -> text("{\"a\":1}", "b"));
    }

    @Test
    void bigIntegers() {
        assertEquals("3000000000", text("{\"a\":3000000000}", "a"));
    }
}
