package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.utils.LoadingElement;

import java.util.ArrayList;

public abstract class GLElement implements LoadingElement {
    private String name;
    private boolean loaded;
    private boolean loadStarted;
    // The first loading threw a ResourceException: waits for one of its data to be reloaded (GLResources.dataReloaded)
    private boolean failed;

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    // The GLResources holding this element, set when it registers it: an element has a single owner
    private volatile GLResources owner;

    // The type it is registered under in its GLResources (a subtype is registered under its base type: FallbackShader)
    Class<? extends GLElement> registeredType;

    final void attachTo(GLResources glResources) {
        if (owner == glResources)
            return;

        if (owner != null)
            throw new IllegalStateException(getClass().getSimpleName() + " " + name
                    + " is already managed by another GLResources");

        owner = glResources;
    }

    // The GLResources holding this element; fails while it is not registered
    protected final GLResources glResources() {
        GLResources current = owner;
        if (current == null)
            throw new IllegalStateException(getClass().getSimpleName() + " " + name
                    + " is not registered in a GLResources");

        return current;
    }

    private int reference;
    // References wanting the element now (renderers, loaded batches), the others only preload it
    private int urgentReference;

    protected ArrayList<DataType> resourceDependencies;
    protected ArrayList<GLElement> glElementDependencies;

    public GLElement(){
        reference = 0;
        resourceDependencies = new ArrayList<>();
        glElementDependencies = new ArrayList<>();
    }

    // The element also waits for this data (and is blocked by its failure)
    public final void addResourceDependency(DataType data) {
        resourceDependencies.add(data);
    }

    void addReference(boolean urgent) {
        ++reference;
        if (urgent)
            ++urgentReference;
    }

    void removeReference(boolean urgent) {
        --reference;
        if (urgent)
            --urgentReference;
    }

    void clearReferences() {
        reference = 0;
        urgentReference = 0;
    }

    boolean isReferenced(){
        return reference != 0;
    }

    boolean isUrgent() {
        return urgentReference != 0;
    }

    public int getNumberReference() {
        return reference;
    }

    public int getNumberUrgentReference() {
        return urgentReference;
    }

    /**
     * Load the element, or a step of it (false until done). An expected failure (the data is wrong, not the code:
     * a shader that does not compile) throws a ResourceException, after deleting what the element created:
     * the element is then failed, its users get the fallback until one of its data is reloaded.
     */
    public abstract boolean load(int remainingMilliseconds);

    @Override
    public boolean isLoaded(){
        return loaded;
    }

    // Its first loading failed (ResourceException): its users get the fallback until one of its data is reloaded
    public boolean hasFailed() {
        return failed;
    }

    // One of its data was reloaded: it is tried again
    void clearFailure() {
        failed = false;
        loadStarted = false;
    }

    // True when this element was made from the data (it is tried again when the data is reloaded)
    boolean dependsOn(DataType data) {
        return resourceDependencies.contains(data);
    }

    /**
     * Load the element, or continue its loading when it is done in several steps (load returns false until finished).
     * remainingMilliseconds is the time left in the frame, it can be 0: do at least one short step anyway.
     * @return true if a loading step was done, false if the dependencies are not loaded yet
     */
    boolean loadByResource(int remainingMilliseconds) {
        for (DataType type : resourceDependencies) {
            if (!type.isLoaded()){
                return false;
            }
        }

        for (GLElement element : glElementDependencies){
            if (!element.isLoaded()){
                return false;
            }
        }

        // Only printed once the dependencies are ready and once per element, even if loaded in several steps
        if (!loadStarted) {
            System.out.println("Loading gl (" + this.getClass().getSimpleName() + ") " + this.getName());
            loadStarted = true;
        }

        try {
            loaded = load(remainingMilliseconds);
        } catch (ResourceException e) {
            loaded = false;
            failed = true;
            System.err.println("--" + getClass().getSimpleName() + " " + name + " failed: " + e.getMessage()
                    + ", its fallback is used");
        }
        return true;
    }

    // This element or a data needed by it failed: it waits for a reload, its users get the fallback meanwhile
    boolean blockedByFailure() {
        if (failed)
            return true;

        for (DataType type : resourceDependencies) {
            if (type.hasFailed())
                return true;
        }

        return false;
    }

    // ---- Sent again (its data was reloaded) while it stays usable ----

    private boolean reloadQueued;

    // Queue the element to be sent again by GLResources (once, until it is done)
    protected final void requestReload() {
        if (!loaded || reloadQueued)
            return;

        reloadQueued = true;
        glResources().queueReload(this);
    }

    /**
     * Send the element again, in steps like load, while the current version stays in use.
     * An expected failure throws a ResourceException, after deleting what the new version created:
     * the element keeps its current version (it is not failed).
     * @return true once done (by default nothing to do)
     */
    protected boolean reload(int remainingMilliseconds) {
        return true;
    }

    boolean reloadByResource(int remainingMilliseconds) {
        boolean done;
        try {
            done = !loaded || reload(remainingMilliseconds);
        } catch (ResourceException e) {
            done = true;
            System.err.println("--" + getClass().getSimpleName() + " " + name + " could not be reloaded: "
                    + e.getMessage() + ", the previous version is kept");
        }

        if (done)
            reloadQueued = false;

        return done;
    }

    public abstract void unload(int remainingMilliseconds);

    void unloadByResource(int remainingMilliseconds) {
        System.out.println("Gl Elements Unloading " + this.getClass().getSimpleName() + " = " + this.getName());
        if (reference != 0)
            return;

        unload(remainingMilliseconds);
        loaded = false;
        loadStarted = false;
        failed = false;
        reloadQueued = false;
    }
}
