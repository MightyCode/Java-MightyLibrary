package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.texture.AnimationData;
import MightyLibrary.mightylib.graphics.texture.Texture;

import java.util.HashMap;

public class Resources {
    private static Resources singletonInstance = null;
    public static Resources getInstance(){
        if (singletonInstance == null) singletonInstance = new Resources();

        return singletonInstance;
    }

    private final HashMap<Class<?>, HashMap<String, DataType>> resources;

    private Resources(){
        resources = new HashMap<>();
        resources.put(Texture.class, new HashMap<>());
        resources.put(AnimationData.class, new HashMap<>());

        TextureLoader.load(resources.get(Texture.class));
        AnimationDataLoader.creates(resources.get(AnimationData.class));
    }


    public Class<?> getClassFromType(EDataType type){
        switch (type){
            case AnimationData:
                return AnimationData.class;
            case Texture:
                return Texture.class;
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
