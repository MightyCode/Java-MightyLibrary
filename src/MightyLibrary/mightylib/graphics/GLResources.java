package MightyLibrary.mightylib.graphics;

import java.util.ArrayDeque;
import java.util.HashMap;
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

        elementToLoad = new ArrayDeque<>();
        elementToUnload = new ArrayDeque<>();

        resources = new HashMap<>();
    }

    public void process() {
        long startTime = System.currentTimeMillis();

        // Each waiting element is tried at most once per call: those whose dependencies aren't loaded yet go back in the queue
        int elementsToTry = elementToLoad.size();
        while (elementsToTry > 0 && System.currentTimeMillis() - startTime < millisecondAllowedToProcessPerFrame) {
            --elementsToTry;
            GLElement element = elementToLoad.poll();

            if (element == null || element.isLoaded())
                continue;

            long remainingMilliseconds = millisecondAllowedToProcessPerFrame - (System.currentTimeMillis() - startTime);
            element.loadByResource((int) remainingMilliseconds);

            if (!element.isLoaded())
                elementToLoad.add(element);
        }

        while (!elementToUnload.isEmpty() && System.currentTimeMillis() - startTime < millisecondAllowedToProcessPerFrame) {
            GLElement element = elementToUnload.poll();

            if (element != null) {
                long remainingMilliseconds = millisecondAllowedToProcessPerFrame - (System.currentTimeMillis() - startTime);
                element.unloadByResource((int) remainingMilliseconds);
            }
        }
    }

    /**
     * Return the element already registered with this name, or register and load the given one.
     * Either way a reference is added: call releaseResource once the element isn't used anymore.
     */
    public <T extends GLElement> T addElementOrReturnIfPresent(Class<T> type, T T, String name) {
        if (T.isLoaded())
            throw new RuntimeException("Element is already loaded!");

        T element = getResource(type, name);
        if (element != null) {
            element.addReference();
            return element;
        }

        resources.putIfAbsent(type, new HashMap<>());
        resources.get(type).put(name, T);
        T.setName(name);
        T.addReference();
        T.loadByResource(150);

        // Its dependencies aren't loaded yet: retried by process() until they are
        if (!T.isLoaded())
            elementToLoad.add(T);

        return T;
    }

    /**
     * Same as addElementOrReturnIfPresent, but the element is loaded later by process().
     */
    public <T extends GLElement> T addElementToLoadOrReturnIfPresent(Class<T> type, T t, String name) {
        T element = getResource(type, name);
        if (element != null) {
            element.addReference();
            return element;
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

    // Lookup only: no reference is added, use addElementOrReturnIfPresent to share an element
    public <T extends GLElement> T getResource(Class<T> type, String name) {
        if (!resources.containsKey(type)) {
            return null;
        }

        GLElement element = resources.get(type).get(name);
        if (element == null) {
            return null;
        }

        return type.cast(element);
    }

    public void releaseResource(GLElement element) {
        element.removeReference();

        if (!element.isReferenced()) {
            // Not needed anymore: it must not be loaded later
            elementToLoad.remove(element);

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
