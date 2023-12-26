package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.text.FontLoader;
import MightyLibrary.mightylib.resources.animation.AnimationDataLoader;
import MightyLibrary.mightylib.resources.data.JsonLoader;
import MightyLibrary.mightylib.resources.texture.IconLoader;
import MightyLibrary.mightylib.resources.texture.TextureLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Resources {
    public static String FOLDER = "resources/";

    public static abstract class LoadingMethod{}
    public static class AllResourcesMethod extends LoadingMethod{}
    public static class BatchResourcesMethod extends LoadingMethod{
        private final String firstBatch;
        public BatchResourcesMethod(String firstBatch){
            this.firstBatch = firstBatch;
        }
    }

    private static Resources singletonInstance = null;

    public static Resources createInstance(LoadingMethod loadingMethod){
        if (singletonInstance == null) {
            singletonInstance = new Resources(loadingMethod);
        }

        return singletonInstance;
    }

    public static Resources getInstance(){
        if (singletonInstance == null){
            singletonInstance = new Resources(new AllResourcesMethod());
        }

        return singletonInstance;
    }

    public final List<ResourceLoader> Loaders;

    private final HashMap<Class<?>, HashMap<String, DataType>> resources;
    private final LoadingMethod loadingMethod;
    private boolean initialized;
    private boolean firstLoad;

    private Resources(LoadingMethod loadingMethod) {
        this.loadingMethod = loadingMethod;
        resources = new HashMap<>();
        Loaders = new ArrayList<>();

        // Mandatory loaders
        Loaders.add(new IconLoader());
        Loaders.add(new TextureLoader());
        Loaders.add(new AnimationDataLoader());
        Loaders.add(new FontLoader());
        JsonLoader jsonLoader = new JsonLoader();
        Loaders.add(jsonLoader);

        Loaders.add(new BatchLoader(jsonLoader));

        initialized = false;
        firstLoad = false;
    }


    public void init(){
        if (initialized)
            return;

        // Init all loaders
        for (ResourceLoader loader : Loaders){
            HashMap<String, DataType> map = new HashMap<>();
            loader.create(map);
            resources.put(loader.getType(), map);
        }

        initialized = true;
    }

    public void createAndInit(Class<?> resourceType, String resourceName, String resourcePath){
        for (ResourceLoader resourceLoader : singletonInstance.Loaders) {
            if (resourceType == resourceLoader.getType()) {
                resourceLoader.createAndLoad(resources.get(resourceType), resourceName, resourcePath);
                break;
            }
        }
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

    public boolean isExistingResource(Class<?> type, String name){
        if (!resources.containsKey(type))
            return false;

        return resources.get(type).containsKey(name);
    }

    public int loadBatch(String batchName){
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't load batch: " + batchName + " when loading method is not BatchResources");
            return -1;
        }

        System.out.println("--Load batch: " + batchName);

        int incorrectlyLoad = 0;

        BatchResources batchResources = getResource(BatchResources.class, batchName);
        JSONObject content = batchResources.getObject();
        for (String resourceTypeName : content.keySet()) {
            Class<?> type = getClassFromName(resourceTypeName);
            ResourceLoader loader = getLoader(type);
            JSONObject resourceType = content.getJSONObject(resourceTypeName);

            JSONArray resourceFiles = resourceType.getJSONArray("files");
            JSONArray regularExpressions = resourceType.getJSONArray("regex");

            for (int i = 0; i < resourceFiles.length(); ++i) {
                String resourceName = resourceFiles.getString(i);

                DataType dataType = this.resources.get(type).get(resourceName);

                if (!dataType.isReferenced())
                    incorrectlyLoad += loadData(dataType, loader);

                dataType.addReference(batchName);
            }

            for (int i = 0; i < regularExpressions.length(); ++i) {
                String regularExpression = regularExpressions.getString(i);
                try {
                    Pattern pattern = Pattern.compile(regularExpression);

                    for (String resourceName : this.resources.get(type).keySet()) {
                        Matcher matcher = pattern.matcher(resourceName);
                        if (matcher.matches()) {
                            DataType dataType = this.resources.get(type).get(resourceName);

                            if (!dataType.isReferenced())
                                incorrectlyLoad += loadData(dataType, loader);

                            dataType.addReference(batchName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Can't compile regular expression: " + regularExpression);
                }
            }
        }

        return incorrectlyLoad;
    }

    public int load(){
        if (firstLoad)
            return -1;

        if (loadingMethod instanceof BatchResourcesMethod){
            // Load the batch resources first
            for (ResourceLoader loader : Loaders){
                if (loader.getType() == BatchResources.class){
                    loadAllOfType(loader.getType());
                    break;
                }
            }

            firstLoad = true;
            return loadBatch(((BatchResourcesMethod) loadingMethod).firstBatch);
        }

        System.out.println("--Load Resources");
        int incorrectlyLoad = 0;

        for (ResourceLoader loader : Loaders){
            incorrectlyLoad += loadAllOfType(loader.getType());
        }

        firstLoad = true;
        return incorrectlyLoad;
    }

    private int loadData(DataType dataType, ResourceLoader loader){
        if (dataType.isCorrectlyLoaded())
            return 0;

        dataType.load(loader);

        if (!dataType.isCorrectlyLoaded())
            return 1;

        return 0;
    }

    private int loadAllOfType(Class<?> typeOfResource){
        int incorrectlyLoad = 0;

        ResourceLoader loader = getLoader(typeOfResource);

        for (DataType dataType : resources.get(typeOfResource).values()){
            incorrectlyLoad += loadData(dataType, loader);
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
            dataType.reload(Objects.requireNonNull(getLoader(typeOfResource)));

            if (!dataType.isCorrectlyLoaded())
                ++incorrectlyReload;
        }

        return incorrectlyReload;
    }


    public int unload(){
        System.out.println("--Unload Resources");
        int incorrectlyUnload = 0;

        for (Class<?> c : resources.keySet()){
            incorrectlyUnload += unloadAllOfType(c);
        }

        return incorrectlyUnload;
    }

    private int unloadData(DataType dataType){
        if (!dataType.isCorrectlyLoaded())
            return 0;

        dataType.unload();
        if (!dataType.isCorrectlyLoaded())
            return 1;

        return 0;
    }


    public int unloadAllOfType(Class<?> typeOfResource){
        int incorrectlyUnload = 0;

        for (DataType dataType : resources.get(typeOfResource).values())
            unloadData(dataType);

        return incorrectlyUnload;
    }

    public int unloadBatch(String batchName){
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't unload batch: " + batchName + " when loading method is not BatchResources");
            return -1;
        }

        System.out.println("--Unload batch: " + batchName);
        int incorrectlyUnload = 0;

        BatchResources batchResources = getResource(BatchResources.class, batchName);
        JSONObject content = batchResources.getObject();
        for (String resourceTypeName : content.keySet()) {
            Class<?> type = getClassFromName(resourceTypeName);
            ResourceLoader loader = getLoader(type);
            JSONObject resourceType = content.getJSONObject(resourceTypeName);

            JSONArray resourceFiles = resourceType.getJSONArray("files");
            JSONArray regularExpressions = resourceType.getJSONArray("regex");

            for (int i = 0; i < resourceFiles.length(); ++i) {
                String resourceName = resourceFiles.getString(i);

                DataType dataType = this.resources.get(type).get(resourceName);
                dataType.removeReference(batchName);

                if (!dataType.isReferenced())
                    incorrectlyUnload += unloadData(dataType);
            }

            for (int i = 0; i < regularExpressions.length(); ++i) {
                String regularExpression = regularExpressions.getString(i);
                try {
                    Pattern pattern = Pattern.compile(regularExpression);

                    for (String resourceName : this.resources.get(type).keySet()) {
                        Matcher matcher = pattern.matcher(resourceName);
                        if (matcher.matches()) {
                            DataType dataType = this.resources.get(type).get(resourceName);
                            dataType.removeReference(batchName);

                            if (!dataType.isReferenced())
                                incorrectlyUnload += unloadData(dataType);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Can't compile regular expression: " + regularExpression);
                }
            }
        }

        return incorrectlyUnload;
    }
}
