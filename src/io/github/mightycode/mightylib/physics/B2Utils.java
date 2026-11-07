package io.github.mightycode.mightylib.physics;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;

public class B2Utils {

    private static Vector2f b2ToEngineVector = new Vector2f(1);
    public static Vector2f getB2ToEngineVector(){ return new Vector2f(b2ToEngineVector);}

    public static void setB2ToEngineVector(float newB2ToEngineValue){
        b2ToEngineVector.set(newB2ToEngineValue);
    }

    public static Vector2f b2ToEngine(Vec2 vector) {
        return new Vector2f(vector.x * b2ToEngineVector.x, -vector.y * b2ToEngineVector.y);
    }

    public static Vec2 engineToB2(Vector2f vector) {
        return new Vec2(vector.x / b2ToEngineVector.x, -vector.y / b2ToEngineVector.y);
    }

    public static <T> T getContactObject(Contact contact, T obj) {
        return getContactObject(contact, obj, null);
    }

    public static <T> T getContactObject(Contact contact, T obj, String fixtureName) {
        Object objA  = contact.getFixtureA().getBody().getUserData();
        String nameA = (String)contact.getFixtureA().getUserData();
        Object objB  = contact.getFixtureB().getBody().getUserData();
        String nameB = (String)contact.getFixtureB().getUserData();

        if ( objA.equals(obj) && (fixtureName == null || nameA.equals(fixtureName))) {
            return obj;
        } else if ( objB.equals(obj) && (fixtureName == null || nameB.equals(fixtureName))) {
            return obj;
        } else {
            return null;
        }
    }

    public static <T> T getContactObject(Contact contact, Class<T> type) {
        return getContactObject(contact, type, null);
    }

    public static <T> T getContactObject(Contact contact, Class<T> type, String fixtureName) {
        Object objA  = contact.getFixtureA().getBody().getUserData();
        String nameA = (String)contact.getFixtureA().getUserData();
        Object objB  = contact.getFixtureB().getBody().getUserData();
        String nameB = (String)contact.getFixtureB().getUserData();

        if (objA.getClass().equals(type) && (fixtureName == null || nameA.equals(fixtureName))) {
            return (T)objA;
        } else if (objB.getClass().equals(type) && (fixtureName == null || nameB.equals(fixtureName))) {
            return (T)objB;
        } else {
            return null;
        }
    }
}
