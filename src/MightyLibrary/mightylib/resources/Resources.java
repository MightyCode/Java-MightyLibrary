package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.text.FontLoader;
import MightyLibrary.mightylib.resources.animation.AnimationDataLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
            loader.create(map);
            resources.put(loader.getType(), map);
        }

        initialized = true;
    }


    public static Class<?> getClassFromName(String name){
        for (ResourceLoader resourceLoader : singletonInstance.Loaders){
            if (resourceLoader.getResourceNameType().equals(name))
                return resourceLoader.getType();
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

        for (ResourceLoader loader : Loaders){
            incorrectlyLoad += load(loader.getType());
        }

        firstLoad = true;
        return incorrectlyLoad;
    }


    private int load(Class<?> typeOfResource){
        int incorrectlyLoad = 0;

        for (DataType dataType : resources.get(typeOfResource).values()){
            if (!dataType.load(Objects.requireNonNull(getLoader(typeOfResource))))
                ++incorrectlyLoad;
        }

        return incorrectlyLoad;
    }

    private ResourceLoader getLoader (Class<?> typeOfResource){
        for (ResourceLoader loader : Loaders){
            if (typeOfResource.equals(loader.getType()))
                return loader;
        }

        return null;
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
            if (!dataType.reload(Objects.requireNonNull(getLoader(typeOfResource)))) ++incorrectlyReload;
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
