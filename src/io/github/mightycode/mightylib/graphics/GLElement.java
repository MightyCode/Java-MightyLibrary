package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.utils.LoadingElement;

import java.util.ArrayList;

public abstract class GLElement implements LoadingElement {
    private String name;
    private boolean loaded;
    private boolean loadStarted;

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    // The GLResources holding this element, set when it registers it: an element has a single owner
    private volatile GLResources owner;

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

    public abstract boolean load(int remainingMilliseconds);

    @Override
    public boolean isLoaded(){
        return loaded;
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

        loaded = load(remainingMilliseconds);
        return true;
    }

    // A data needed by this element failed: it waits for a reload, its users get the fallback meanwhile
    boolean blockedByFailure() {
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
     * @return true once done (by default nothing to do)
     */
    protected boolean reload(int remainingMilliseconds) {
        return true;
    }

    boolean reloadByResource(int remainingMilliseconds) {
        boolean done = !loaded || reload(remainingMilliseconds);
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
        reloadQueued = false;
    }
}
