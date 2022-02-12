package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.text.FontFace;
import MightyLibrary.mightylib.graphics.text.FontLoader;
import MightyLibrary.mightylib.graphics.texture.AnimationData;
import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.sounds.Sound;

import java.util.HashMap;

public class Resources {
    private static Resources singletonInstance = null;
    public static Resources getInstance(){
        if (singletonInstance == null){
            singletonInstance = new Resources();
            singletonInstance.init();
        }

        return singletonInstance;
    }

    private final HashMap<Class<?>, HashMap<String, DataType>> resources;

    private Resources(){
        resources = new HashMap<>();
        resources.put(Texture.class, new HashMap<>());
        resources.put(AnimationData.class, new HashMap<>());
        resources.put(FontFace.class, new HashMap<>());
        resources.put(Sound.class, new HashMap<>());
    }

    private void init(){

        TextureLoader.load(resources.get(Texture.class));
        AnimationDataLoader.creates(resources.get(AnimationData.class));
        FontLoader.load(resources.get(FontFace.class));
        SoundLoader.creates(resources.get(Sound.class));
    }

    public static Class<?> getClassFromName(String name){
        if (name.equalsIgnoreCase("animation"))
            return AnimationData.class;
        else if (name.equalsIgnoreCase("texture"))
            return Texture.class;
        else if (name.equalsIgnoreCase("font"))
            return FontFace.class;
        else if (name.equalsIgnoreCase("sound"))
            return Sound.class;

        return Object.class;
    }

    public static Class<?> getClassFromType(EDataType type){
        switch (type){
            case AnimationData:
                return AnimationData.class;
            case Texture:
                return Texture.class;
            case Font:
                return FontFace.class;
            case Sound:
                return Sound.class;
        }

        return Object.class;
    }


    public <T> T getResource(Class<T> type, String name){
        return type.cast(resources.get(type).get(name));
    }


    public int load(){
        System.out.println("--Load Resources");
        int incorrectlyLoad = 0;

        for (Class<?> c : resources.keySet()){
            incorrectlyLoad += load(c);
        }

        return incorrectlyLoad;
    }


    public int load(Class<?> typeOfResource){
        int incorrectlyLoad = 0;

        for (DataType dataType : resources.get(typeOfResource).values()){
            if (!dataType.load()) ++incorrectlyLoad;
        }

        return incorrectlyLoad;
    }


    public int reload(){
        int incorrectlyReload = 0;

        for (Class<?> c : resources.keySet()){
            incorrectlyReload += reload(c);
        }

        return incorrectlyReload;
    }


    public int reload(Class<?> typeOfResource){
        int incorrectlyReload = 0;

        for (DataType dataType : resources.get(typeOfResource).values()){
            if (!dataType.reload()) ++incorrectlyReload;
        }

        return incorrectlyReload;
    }


    public int unload(){
        System.out.println("--Unload Resources");
        int incorrectlyUnload = 0;

        for (Class<?> c : resources.keySet()){
            incorrectlyUnload += unload(c);
        }

        return incorrectlyUnload;
    }


    public int unload(Class<?> typeOfResource){
        int incorrectlyUnload = 0;

        for (DataType dataType : resources.get(typeOfResource).values()){
            if (!dataType.unload()) ++incorrectlyUnload;
        }

        return incorrectlyUnload;
    }
}
