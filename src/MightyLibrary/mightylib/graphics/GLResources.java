package MightyLibrary.mightylib.graphics;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;

public class GLResources {

    public static class GLResourceCreation {
        public int MILLISECONDS_ALLOWED_TO_PROCESS_PER_FRAME = 100;
    }

    private static GLResources singletonInstance = null;

    public static GLResources createInstance(GLResourceCreation info) {
        if (singletonInstance == null) {
            singletonInstance = new GLResources(info);
        }

        return singletonInstance;
    }

    public static GLResources getInstance(){
        if (singletonInstance == null){
            throw new RuntimeException("No GL resource instance created!");
        }

        return singletonInstance;
    }

    private final int millisecondAllowedToProcessPerFrame;

    private final Queue<GLElement> elementToLoad;
    private final Queue<GLElement> elementToUnload;

    private final HashMap<Class<? extends GLElement>, HashMap<String, GLElement>> resources;

    private GLResources(GLResourceCreation info) {
        millisecondAllowedToProcessPerFrame = info.MILLISECONDS_ALLOWED_TO_PROCESS_PER_FRAME;

        elementToLoad = new PriorityQueue<>();
        elementToUnload = new PriorityQueue<>();

        resources = new HashMap<>();
    }

    public void process() {
        long startTime = System.currentTimeMillis();

        while (!elementToLoad.isEmpty() && System.currentTimeMillis() - startTime < millisecondAllowedToProcessPerFrame) {
            GLElement element = elementToLoad.poll();
            if (element != null) {
                long remainingMilliseconds = millisecondAllowedToProcessPerFrame - (System.currentTimeMillis() - startTime);
                element.loadByResource((int) remainingMilliseconds);
            }
        }

        while (!elementToUnload.isEmpty() && System.currentTimeMillis() - startTime < millisecondAllowedToProcessPerFrame) {
            GLElement element = elementToUnload.poll();

            if (element != null) {
                long remainingMilliseconds = millisecondAllowedToProcessPerFrame - (System.currentTimeMillis() - startTime);
                element.unloadByResource((int) remainingMilliseconds);
            }
        }
    }

    public <T extends GLElement> T addElementOrReturnIfPresent(Class<T> type, T T, String name) {
        if (T.isLoaded())
            throw new RuntimeException("Element is already loaded!");

        GLElement element = getResource(type, name);
        if (element != null) {
            return type.cast(element);
        }

        resources.putIfAbsent(type, new HashMap<>());
        resources.get(type).put(name, T);
        T.setName(name);
        T.loadByResource(150);
        T.addReference();

        return T;
    }

    public <T extends GLElement> T addElementToLoadOrReturnIfPresent(Class<T> type, T t, String name) {
        GLElement element = resources.get(type).get(name);
        if (element != null) {
            return type.cast(element);
        }

        resources.putIfAbsent(type, new HashMap<>());
        resources.get(type).put(name, t);
        t.setName(name);
        t.addReference();

        if (!t.isLoaded()) {
            elementToLoad.add(t);
        }

        return t;
    }

    public void addElementToUnload(GLElement element) {
        //TODO

        if (element.isLoaded()) {
            elementToUnload.add(element);
        }
    }

    public <T extends GLElement> T getResource(Class<T> type, String name) {
        if (!resources.containsKey(type)) {
            return null;
        }

        GLElement element = resources.get(type).get(name);
        if (element == null) {
            return null;
        }

        element.addReference();
        return type.cast(element);
    }

    public void releaseResource(GLElement element) {
        element.removeReference();

        if (!element.isReferenced()) {
            resources.get(element.getClass()).remove(element.getName());
            element.unloadByResource(150);
        }
    }

    public void unload() {
        for (HashMap<String, GLElement> map : resources.values()){
            for (GLElement element : map.values()){
                element.unloadByResource(150);
            }
        }
    }
}
