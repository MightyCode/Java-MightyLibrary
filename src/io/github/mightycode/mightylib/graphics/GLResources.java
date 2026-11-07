package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.resources.texture.Texture;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Shared GL elements (textures...), sent to the graphic card by the main loop with the time left in each frame.
 * Asking for an element never blocks: it is queued and loaded in the next frames.
 */
public class GLResources {
    private static final long NANO_IN_MILLISECOND = 1000000L;

    private static GLResources singletonInstance = null;

    // maxMillisecondsPerFrame: cap of the time spent per frame, -1 to only use the time left in the frame
    public static GLResources createInstance(int maxMillisecondsPerFrame) {
        if (singletonInstance == null) {
            singletonInstance = new GLResources(maxMillisecondsPerFrame);
        }

        return singletonInstance;
    }

    public static GLResources getInstance(){
        if (singletonInstance == null){
            throw new RuntimeException("No GL resource instance created!");
        }

        return singletonInstance;
    }

    private final long maxNanosPerFrame;

    private final Queue<GLElement> elementToLoad;
    // Loaded elements to send again, their data having changed (hot reload)
    private final Queue<GLElement> elementToReload = new ArrayDeque<>();

    private final HashMap<Class<? extends GLElement>, HashMap<String, GLElement>> resources;

    private GLResources(int maxMillisecondsPerFrame) {
        maxNanosPerFrame = maxMillisecondsPerFrame < 0 ? Long.MAX_VALUE : maxMillisecondsPerFrame * NANO_IN_MILLISECOND;

        elementToLoad = new ArrayDeque<>();

        resources = new HashMap<>();
    }

    /**
     * Load the waiting elements until the deadline (System.nanoTime) is reached.
     * At least one step is done even if the deadline is already passed, so the loading always progresses:
     * an element loaded in several steps (big texture) must keep each step short.
     */
    public void process(long deadlineNanos) {
        long startTime = System.nanoTime();
        long deadline = Math.min(deadlineNanos, startTime + Math.min(maxNanosPerFrame, Long.MAX_VALUE - startTime));
        boolean stepDone = false;

        // Each waiting element is tried at most once per call: unfinished ones go back in the queue
        int elementsToTry = elementToLoad.size();
        while (elementsToTry > 0 && (!stepDone || System.nanoTime() < deadline)) {
            --elementsToTry;
            GLElement element = elementToLoad.poll();

            if (element == null || element.isLoaded())
                continue;

            long remainingNanos = Math.max(0, deadline - System.nanoTime());
            if (element.loadByResource((int) (remainingNanos / NANO_IN_MILLISECOND)))
                stepDone = true;

            if (!element.isLoaded())
                elementToLoad.add(element);
        }

        // Then the elements sent again (reloaded data), shown with their previous version meanwhile
        int reloadsToTry = elementToReload.size();
        while (reloadsToTry > 0 && (!stepDone || System.nanoTime() < deadline)) {
            --reloadsToTry;
            GLElement element = elementToReload.poll();

            long remainingNanos = Math.max(0, deadline - System.nanoTime());
            if (!element.reloadByResource((int) (remainingNanos / NANO_IN_MILLISECOND)))
                elementToReload.add(element);
            stepDone = true;
        }
    }

    // Called by GLElement.requestReload
    void queueReload(GLElement element) {
        elementToReload.add(element);
    }

    // True while elements wait to be loaded, except those waiting for a failed data (they use the fallback)
    public boolean hasElementsToLoad() {
        for (GLElement element : elementToLoad) {
            if (!element.blockedByFailure())
                return true;
        }

        return false;
    }

    /**
     * Return the element already registered with this name, or register the given one and queue it:
     * it is loaded by process() in the next frames, never during this call.
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
        elementToLoad.add(T);

        return T;
    }

    // Names of the elements waiting to be loaded, to find the one blocking a loading screen
    public String describeWaitingElements() {
        StringBuilder names = new StringBuilder();
        for (GLElement element : elementToLoad) {
            if (names.length() > 0)
                names.append(", ");
            names.append(element.getClass().getSimpleName()).append(' ').append(element.getName());
        }

        return names.toString();
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
            elementToReload.remove(element);

            resources.get(element.getClass()).remove(element.getName());
            element.unloadByResource(150);
        }
    }

    // Registered elements by type and name with their number of references, to track leaks
    public JSONObject describe() {
        JSONObject json = new JSONObject();

        for (Map.Entry<Class<? extends GLElement>, HashMap<String, GLElement>> typeEntry : resources.entrySet()) {
            JSONObject elements = new JSONObject();
            for (Map.Entry<String, GLElement> element : typeEntry.getValue().entrySet())
                elements.put(element.getKey(), element.getValue().getNumberReference());

            json.put(typeEntry.getKey().getSimpleName(), elements);
        }

        json.put("waitingToLoad", elementToLoad.size());

        return json;
    }

    public void unload() {
        for (HashMap<String, GLElement> map : resources.values()){
            for (GLElement element : map.values()){
                element.unloadByResource(150);
            }
        }

        elementToReload.clear();
        Texture.DeletePlaceholder();
    }
}
