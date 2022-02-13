package MightyLibrary.mightylib.sounds;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import static org.lwjgl.openal.AL10.*;

public class SoundData extends DataType {

    private final static String PATH = "resources/sounds/";

    private int bufferId;

    public SoundData(String dataName, String path) {
        super(EDataType.Sound, dataName, path);

        bufferId = -1;
    }

    @Override
    public boolean load() {
        this.bufferId = alGenBuffers();

        int lastIndex = path.lastIndexOf('.');

        if (path.indexOf("ogg", lastIndex) != -1)
            return SoundDataType.loadOgg(PATH + path, bufferId);

        return true;
    }


    public int getBufferId(){
        return bufferId;
    }

    @Override
    public boolean unload() {
        alDeleteBuffers(this.bufferId);
        return false;
    }
}
