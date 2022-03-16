package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.text.FontFace;
import MightyLibrary.mightylib.graphics.text.FontLoader;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.resources.animation.AnimationDataLoader;
import MightyLibrary.mightylib.sounds.SoundData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Resources {
    private static Resources singletonInstance = null;
    public static Resources getInstance(){
        if (singletonInstance == null){
            singletonInstance = new Resources();
        }

        return singletonInstance;
    }

    public final List<ResourceLoader> Loaders;

    private final HashMap<Class<?>, HashMap<String, DataType>> resources;

    private boolean initialized;
    private boolean firstLoad;

    private Resources(){
        resources = new HashMap<>();
        Loaders = new ArrayList<>();

        Loaders.add(new TextureLoader());
        Loaders.add(new AnimationDataLoader());
        Loaders.add(new FontLoader());
        Loaders.add(new SoundLoader());

        initialized = false;
        firstLoad = false;
    }

    public void init(){
        if (initialized)
            return;

        for (ResourceLoader loader : Loaders){
            HashMap<String, DataType> map = new HashMap<>();
            loader.load(map);
            resources.put(loader.getType(), map);
        }

        initialized = true;
    }


    public static Class<?> getClassFromName(String name){
        if (name.equalsIgnoreCase("animation"))
            return AnimationData.class;
        else if (name.equalsIgnoreCase("texture"))
            return Texture.class;
        else if (name.equalsIgnoreCase("font"))
            return FontFace.class;
        else if (name.equalsIgnoreCase("sound"))
            return SoundData.class;

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
                return SoundData.class;
        }

        return Object.class;
    }


    public <T> T getResource(Class<T> type, String name){
        return type.cast(resources.get(type).get(name));
    }


    public int load(){
        if (firstLoad)
            return -1;

        System.out.println("--Load Resources");
        int incorrectlyLoad = 0;

        for (Class<?> c : resources.keySet()){
            incorrectlyLoad += load(c);
        }

        firstLoad = true;
        return incorrectlyLoad;
    }


    private int load(Class<?> typeOfResource){
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
