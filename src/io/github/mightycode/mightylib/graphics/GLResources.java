package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.IGLElementSink;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TexturePlaceholders;
import io.github.mightycode.mightylib.resources.texture.TextureUploader;
import org.json.JSONObject;

import static org.lwjgl.opengl.GL11.glFlush;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Shared GL elements (textures...), sent to the graphic card by the main loop with the time left in each frame.
 * Asking for an element never blocks: it is queued and loaded in the next frames.
 * An element wanted now (urgent reference: renderer, loaded batch) is loaded before those only preloaded
 * (preloaded batch), and only the urgent ones keep a loading screen waiting.
 */
public class GLResources implements IGLElementSink {
    private static final long NANO_IN_MILLISECOND = 1000000L;

    private final long maxNanosPerFrame;

    // The CPU data these elements are made from (null in the tests without data)
    private final Resources cpuResources;
    private final TexturePlaceholders placeholders = new TexturePlaceholders();
    private Shader fallbackShader;

    // Elements waiting to be loaded, in order; an element is in one of them (moved in O(1) when its urgency changes)
    private final LinkedHashSet<GLElement> urgentToLoad = new LinkedHashSet<>();
    private final LinkedHashSet<GLElement> preloadToLoad = new LinkedHashSet<>();
    // Elements whose first loading failed: out of the queues until one of their data is reloaded (dataReloaded)
    private final LinkedHashSet<GLElement> failedElements = new LinkedHashSet<>();
    // Loaded elements to send again, their data having changed (hot reload)
    private final Queue<GLElement> elementToReload = new ArrayDeque<>();

    private final HashMap<Class<? extends GLElement>, HashMap<String, GLElement>> resources;

    // maxMillisecondsPerFrame: cap of the time spent per frame, -1 to only use the time left in the frame
    public GLResources(Resources cpuResources, int maxMillisecondsPerFrame) {
        this.cpuResources = cpuResources;
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

        // Nothing left to send: the pixel buffers are not kept
        if (urgentToLoad.isEmpty() && preloadToLoad.isEmpty() && elementToReload.isEmpty())
            TextureUploader.releaseBuffers();

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
        while (!registered.isLoaded() && !registered.hasFailed()) {
            if (!registered.loadByResource(Integer.MAX_VALUE))
                throw new IllegalStateException(type.getSimpleName() + " " + name
                        + " can't be loaded now: its dependencies are not loaded");
        }

        if (registered.hasFailed())
            failedElements.add(registered);

        return registered;
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

    // Textures bound while a texture is not loaded
    public TexturePlaceholders placeholders() {
        return placeholders;
    }

    // ---- IGLElementSink: the GL elements held by the batches ----

    /*
     * The elements of the fallback data, shown in place of those that failed or are not loaded yet. The texture is
     * sent in the next frames, the program is linked now: a shape always has a linked program to draw with.
     */
    @Override
    public void fallbackDataLoaded() {
        addElementOrReturnIfPresent(Texture.class, new Texture(cpuResources, Resources.FALLBACK_NAME), Resources.FALLBACK_NAME);

        ShaderData shaderData = cpuResources.getResourceOrFallback(ShaderData.class, Resources.FALLBACK_NAME);
        fallbackShader = addElementAndLoadNow(Shader.class, (Shader) shaderData.createGLElement(), Resources.FALLBACK_NAME);
        if (!fallbackShader.isLoaded())
            throw new IllegalStateException("The fallback shader can't be linked");
    }

    // Program used in place of those that failed or are not linked yet; null before the fallback data are loaded
    public Shader fallbackShader() {
        return fallbackShader;
    }

    @Override
    public GLElement acquire(DataType data, boolean urgent) {
        Class<? extends GLElement> type = data.glElementType();
        if (type == null)
            return null;

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

            resources.get(element.registeredType).remove(element.getName());
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

        urgentToLoad.clear();
        preloadToLoad.clear();
        elementToReload.clear();
        failedElements.clear();
        TextureUploader.releaseBuffers();
        placeholders.delete();
    }
}
