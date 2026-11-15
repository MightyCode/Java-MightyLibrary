package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.IGLElementSink;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.lwjgl.opengl.GL11.glFlush;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * Shared GL elements (textures...), sent to the graphic card by the main loop with the time left in each frame.
 * Asking for an element never blocks: it is queued and loaded in the next frames.
 * An element wanted now (urgent reference: renderer, loaded batch) is loaded before those only preloaded
 * (preloaded batch), and only the urgent ones keep a loading screen waiting.
 */
public class GLResources implements IGLElementSink {
    private static final long NANO_IN_MILLISECOND = 1000000L;

    private final long maxNanosPerFrame;

    // State shared by the elements of a type, by its class (see shared)
    private final Map<Class<? extends IGLSharedState>, IGLSharedState> sharedStates = new LinkedHashMap<>();
    // The fallback data with a GL element, by the type of their loader (see fallback)
    private final Map<Class<? extends DataType>, DataType> fallbackData = new LinkedHashMap<>();
    // Their elements, kept for the whole program; out of the table by name: two fallbacks with elements of the same
    // type and name do not collide
    private final Map<DataType, GLElement> fallbackElements = new IdentityHashMap<>();

    // Elements waiting to be loaded, in order; an element is in one of them (moved in O(1) when its urgency changes)
    private final LinkedHashSet<GLElement> urgentToLoad = new LinkedHashSet<>();
    private final LinkedHashSet<GLElement> preloadToLoad = new LinkedHashSet<>();
    // Elements whose first loading failed: out of the queues until one of their data is reloaded (dataReloaded)
    private final LinkedHashSet<GLElement> failedElements = new LinkedHashSet<>();
    // Loaded elements to send again, their data having changed (hot reload)
    private final Queue<GLElement> elementToReload = new ArrayDeque<>();

    private final HashMap<Class<? extends GLElement>, HashMap<String, GLElement>> resources;

    // maxMillisecondsPerFrame: cap of the time spent per frame, -1 to only use the time left in the frame
    public GLResources(int maxMillisecondsPerFrame) {
        maxNanosPerFrame = maxMillisecondsPerFrame < 0 ? Long.MAX_VALUE : maxMillisecondsPerFrame * NANO_IN_MILLISECOND;

        resources = new HashMap<>();
    }

    /**
     * Load the waiting elements until the deadline (System.nanoTime) is reached, the urgent ones first.
     * At least one step is done even if the deadline is already passed, so the loading always progresses:
     * an element loaded in several steps (big texture) must keep each step short.
     */
    public void process(long deadlineNanos) {
        long startTime = System.nanoTime();
        long deadline = Math.min(deadlineNanos, startTime + Math.min(maxNanosPerFrame, Long.MAX_VALUE - startTime));
        boolean stepDone = loadFrom(urgentToLoad, deadline, false);
        stepDone = loadFrom(preloadToLoad, deadline, stepDone);

        // Then the elements sent again (reloaded data), shown with their previous version meanwhile
        int reloadsToTry = elementToReload.size();
        while (reloadsToTry > 0 && (!stepDone || System.nanoTime() < deadline)) {
            --reloadsToTry;
            GLElement element = elementToReload.poll();
            // A step can release other elements (a model replacing its textures): the queue may be shorter than counted
            if (element == null)
                break;

            long remainingNanos = Math.max(0, deadline - System.nanoTime());
            if (!element.reloadByResource((int) (remainingNanos / NANO_IN_MILLISECOND)))
                elementToReload.add(element);
            stepDone = true;
        }

        // Nothing left to send: what is only needed while sending is not kept (pixel buffers)
        if (urgentToLoad.isEmpty() && preloadToLoad.isEmpty() && elementToReload.isEmpty()) {
            for (IGLSharedState state : sharedStates.values())
                state.loadingIdle();
        }

        // What was sent here is shared with the other windows: their contexts only see it once the commands are issued
        if (stepDone && GLContext.hasSeveralWindows())
            glFlush();
    }

    // Each waiting element of the set is tried at most once: unfinished ones go back at its end
    private boolean loadFrom(LinkedHashSet<GLElement> waiting, long deadline, boolean stepDone) {
        int elementsToTry = waiting.size();
        while (elementsToTry > 0 && !waiting.isEmpty() && (!stepDone || System.nanoTime() < deadline)) {
            --elementsToTry;
            Iterator<GLElement> first = waiting.iterator();
            GLElement element = first.next();
            first.remove();

            if (element.isLoaded())
                continue;

            long remainingNanos = Math.max(0, deadline - System.nanoTime());
            if (element.loadByResource((int) (remainingNanos / NANO_IN_MILLISECOND)))
                stepDone = true;

            // Out of the queues: tried again only when one of its data is reloaded, never in a loop
            if (element.hasFailed()) {
                if (element.isReferenced())
                    failedElements.add(element);
                continue;
            }

            // The step may have released it, or changed its urgency (another element released)
            if (!element.isLoaded() && element.isReferenced())
                waitingSetOf(element).add(element);
        }

        return stepDone;
    }

    private LinkedHashSet<GLElement> waitingSetOf(GLElement element) {
        return element.isUrgent() ? urgentToLoad : preloadToLoad;
    }

    // Called by GLElement.requestReload
    void queueReload(GLElement element) {
        elementToReload.add(element);
    }

    /**
     * Register the element (urgent reference) and load it now, during this call: for the small elements needed
     * before anything is drawn (fallbacks). Its dependencies must already be loaded.
     */
    public <T extends GLElement> T addElementAndLoadNow(Class<T> type, T element, String name) {
        T registered = addElementOrReturnIfPresent(type, element, name);
        if (registered.isLoaded())
            return registered;

        waitingSetOf(registered).remove(registered);
        loadNow(registered);
        return registered;
    }

    // Loaded during the call: a failed element waits for a reload of its data
    private void loadNow(GLElement element) {
        while (!element.isLoaded() && !element.hasFailed()) {
            if (!element.loadByResource(Integer.MAX_VALUE))
                throw new IllegalStateException(element.getClass().getSimpleName() + " " + element.getName()
                        + " can't be loaded now: its dependencies are not loaded");
        }

        if (element.hasFailed())
            failedElements.add(element);
    }

    // True while urgent elements wait to be loaded, except those waiting for a failed data (they use the fallback)
    public boolean hasElementsToLoad() {
        for (GLElement element : urgentToLoad) {
            if (!element.blockedByFailure())
                return true;
        }

        return false;
    }

    /**
     * Return the element already registered with this name, or register the given one and queue it:
     * it is loaded by process() in the next frames, never during this call.
     * Either way an urgent reference is added: call releaseResource once the element isn't used anymore.
     */
    public <T extends GLElement> T addElementOrReturnIfPresent(Class<T> type, T T, String name) {
        return addElementOrReturnIfPresent(type, T, name, true);
    }

    // urgent false: only preloaded, loaded after the urgent elements and not waited by a loading screen
    public <T extends GLElement> T addElementOrReturnIfPresent(Class<T> type, T T, String name, boolean urgent) {
        if (T.isLoaded())
            throw new RuntimeException("Element is already loaded!");

        T element = getResource(type, name);
        if (element != null) {
            addReference(element, urgent);
            return element;
        }

        T.attachTo(this);
        T.registeredType = type;
        resources.computeIfAbsent(type, key -> new HashMap<>()).put(name, T);
        T.setName(name);
        T.addReference(urgent);
        waitingSetOf(T).add(T);

        return T;
    }

    private void addReference(GLElement element, boolean urgent) {
        boolean wasUrgent = element.isUrgent();
        element.addReference(urgent);
        if (!wasUrgent && element.isUrgent() && preloadToLoad.remove(element))
            urgentToLoad.add(element);
    }

    /**
     * The state shared by the elements of a type, created by the factory at the first call (main thread), one per
     * class, told when the loading is idle and deleted with GLResources.
     */
    public <T extends IGLSharedState> T shared(Class<T> type, Supplier<T> factory) {
        IGLSharedState state = sharedStates.get(type);
        if (state == null) {
            state = factory.get();
            if (!type.isInstance(state))
                throw new IllegalArgumentException("The factory of " + type.getSimpleName() + " made a "
                        + (state == null ? "null" : state.getClass().getSimpleName()));
            sharedStates.put(type, state);
        }

        return type.cast(state);
    }

    // ---- IGLElementSink: the GL elements held by the batches ----

    /*
     * The elements of the fallback data that have one, shown in place of those that failed or are not loaded yet,
     * referenced for the whole program. Each data says when its element is needed: sent in the next frames, or
     * loaded now when it must be ready before anything is drawn (the program of the renderers), its failure then
     * stopping the program since nothing could replace it.
     */
    @Override
    public void fallbackDataLoaded(Map<Class<? extends DataType>, DataType> fallbacks) {
        for (Map.Entry<Class<? extends DataType>, DataType> entry : fallbacks.entrySet()) {
            DataType data = entry.getValue();
            Class<? extends GLElement> type = data.glElementType();
            if (type == null || fallbackData.containsKey(entry.getKey()))
                continue;

            GLElement element = data.createGLElement();
            if (!type.isInstance(element))
                throw new IllegalStateException("The fallback " + data.getClass().getSimpleName() + " made a "
                        + (element == null ? "null" : element.getClass().getSimpleName()) + " instead of a "
                        + type.getSimpleName());

            element.attachTo(this);
            element.registeredType = type;
            element.setName(data.glElementName());
            element.addReference(true);
            fallbackData.put(entry.getKey(), data);
            fallbackElements.put(data, element);

            if (!data.glElementNeededBeforeDrawing()) {
                urgentToLoad.add(element);
                continue;
            }

            loadNow(element);
            if (!element.isLoaded())
                throw new IllegalStateException("The fallback " + type.getSimpleName() + " of "
                        + entry.getKey().getSimpleName() + " can't be loaded: it is needed before anything is drawn");
        }
    }

    /**
     * The element of the fallback data of this type, or of its closest parent type with a fallback element (same rule
     * as Resources.getFallback); null without one, or before the fallback data are loaded. Not loaded yet when its
     * data does not need it before drawing: check isLoaded.
     */
    public GLElement fallback(Class<? extends DataType> type) {
        for (Class<?> current = type; current != null && DataType.class.isAssignableFrom(current); current = current.getSuperclass()) {
            DataType data = fallbackData.get(current);
            if (data != null)
                return fallbackElements.get(data);
        }

        return null;
    }

    // Each fallback element: its data type (the type of the loader), element type, when it is loaded and its state
    public JSONArray describeFallbacks() {
        JSONArray list = new JSONArray();
        for (Map.Entry<Class<? extends DataType>, DataType> entry : fallbackData.entrySet()) {
            GLElement element = fallbackElements.get(entry.getValue());
            JSONObject json = new JSONObject();
            json.put("dataType", entry.getKey().getSimpleName());
            json.put("elementType", element.registeredType.getSimpleName());
            json.put("neededBeforeDrawing", entry.getValue().glElementNeededBeforeDrawing());
            json.put("state", element.isLoaded() ? "loaded" : element.hasFailed() ? "failed" : "waiting");
            list.put(json);
        }

        return list;
    }

    @Override
    public GLElement acquire(DataType data, boolean urgent) {
        Class<? extends GLElement> type = data.glElementType();
        if (type == null)
            return null;

        // A fallback data shares the element made by fallbackDataLoaded
        GLElement fallbackElement = fallbackElements.get(data);
        if (fallbackElement != null) {
            addReference(fallbackElement, urgent);
            return fallbackElement;
        }

        String name = data.glElementName();
        GLElement element = getResource(type, name);
        if (element != null) {
            addReference(element, urgent);
            return element;
        }

        return register(type, data.createGLElement(), name, urgent);
    }

    // The failed elements made from this data are tried again with its new version
    @Override
    public void dataReloaded(DataType data) {
        Iterator<GLElement> iterator = failedElements.iterator();
        while (iterator.hasNext()) {
            GLElement element = iterator.next();
            if (!element.dependsOn(data))
                continue;

            iterator.remove();
            element.clearFailure();
            waitingSetOf(element).add(element);
        }
    }

    private <T extends GLElement> T register(Class<T> type, GLElement element, String name, boolean urgent) {
        return addElementOrReturnIfPresent(type, type.cast(element), name, urgent);
    }

    @Override
    public void release(GLElement element, boolean urgent) {
        releaseResource(element, urgent);
    }

    // Names of the elements waiting to be loaded, to find the one blocking a loading screen
    public String describeWaitingElements() {
        StringBuilder names = new StringBuilder();
        for (GLElement element : urgentToLoad) {
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

    // Release an urgent reference (the one added by addElementOrReturnIfPresent)
    public void releaseResource(GLElement element) {
        releaseResource(element, true);
    }

    public void releaseResource(GLElement element, boolean urgent) {
        // Released more than referenced (a user unloaded twice): would delete an element others still use
        if (urgent ? !element.isUrgent() : element.getNumberReference() == element.getNumberUrgentReference()) {
            System.err.println("--GL element released more than referenced: " + element.getClass().getSimpleName()
                    + " " + element.getName() + (urgent ? "" : " (preload)"));
            return;
        }

        element.removeReference(urgent);

        if (!element.isReferenced()) {
            // Not needed anymore: it must not be loaded later
            urgentToLoad.remove(element);
            preloadToLoad.remove(element);
            elementToReload.remove(element);
            failedElements.remove(element);

            // A fallback element is not in the table by name, where an element of the same name must stay
            Map<String, GLElement> ofType = resources.get(element.registeredType);
            if (ofType != null)
                ofType.remove(element.getName(), element);
            element.unloadByResource(150);
        } else if (!element.isUrgent() && urgentToLoad.remove(element)) {
            // Still preloaded, not wanted now anymore
            preloadToLoad.add(element);
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

        json.put("waitingToLoad", urgentToLoad.size());
        json.put("waitingToPreload", preloadToLoad.size());
        json.put("failed", namesFailed());

        return json;
    }

    // Elements whose first loading failed ("type name"), waiting for one of their data to be reloaded
    public List<String> namesFailed() {
        List<String> names = new ArrayList<>();
        for (GLElement element : failedElements)
            names.add(element.getClass().getSimpleName() + " " + element.getName());

        return names;
    }

    // Names of the elements waiting to be loaded: urgent ones, or only preloaded ones (tests and debug)
    public List<String> namesWaiting(boolean urgent) {
        List<String> names = new ArrayList<>();
        for (GLElement element : urgent ? urgentToLoad : preloadToLoad)
            names.add(element.getName());

        return names;
    }

    // At exit: everything is deleted, even the elements still referenced (batches loaded until the end)
    public void unload() {
        for (HashMap<String, GLElement> map : resources.values()){
            for (GLElement element : map.values()){
                if (!element.isLoaded())
                    continue;

                element.clearReferences();
                element.unloadByResource(150);
            }
        }

        for (GLElement element : fallbackElements.values()) {
            if (!element.isLoaded())
                continue;

            element.clearReferences();
            element.unloadByResource(150);
        }
        fallbackElements.clear();
        fallbackData.clear();

        urgentToLoad.clear();
        preloadToLoad.clear();
        elementToReload.clear();
        failedElements.clear();
        for (IGLSharedState state : sharedStates.values())
            state.delete();
        sharedStates.clear();
    }
}
