package MightyLibrary.mightylib.inputs;

import java.util.HashMap;

public class CharInputEntry {
    public static final int CAPSLOCK = 0;
    public static final int ALT = CAPSLOCK + 1;
    public static final int FINAL_ID = ALT;

    public HashMap<Integer, Character> entries;

    private final int keyId;
    private final char representation;


    public CharInputEntry(int keyId, char representation){
        this.keyId = keyId;
        this.representation = representation;

        entries = new HashMap<>();
    }

    public CharInputEntry addModifier(int action, char modified){
        entries.put(action, modified);

        return this;
    }

    public char getModification(int action){
        if (entries.containsKey(action))
            return entries.get(action);

        return (char) -1;
    }

    public char getModificationOrDefault(int action){
        if (entries.containsKey(action))
            return entries.get(action);

        return representation;
    }

    public char getChar(){
        return representation;
    }

    public boolean isKeyId(int keyId){
        return this.keyId == keyId;
    }
}
