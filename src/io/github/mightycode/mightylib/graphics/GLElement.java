package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.utils.LoadingElement;

import java.util.ArrayList;

public abstract class GLElement implements LoadingElement {
    private String name;
    private boolean loaded;
    private boolean loadStarted;
    private boolean preLoaded;

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    private int reference;

    protected ArrayList<DataType> resourceDependencies;
    protected ArrayList<GLElement> glElementDependencies;

    public GLElement(){
        reference = 0;
        resourceDependencies = new ArrayList<>();
        glElementDependencies = new ArrayList<>();
    }

    void addReference() {
        //System.out.println("Adding reference to " + this.getClass().getSimpleName() + " = " + this.getName());
        ++reference;
    }

    void removeReference() {
        //System.out.println("Removing reference to " + this.getClass().getSimpleName() + " = " + this.getName());
        --reference;
    }

    boolean isReferenced(){
        return reference != 0;
    }

    public int getNumberReference() {
        return reference;
    }

    public abstract boolean load(int remainingMilliseconds);

    @Override
    public boolean isPreLoaded(){
        return preLoaded;
    }

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

    public abstract void unload(int remainingMilliseconds);

    void unloadByResource(int remainingMilliseconds) {
        System.out.println("Gl Elements Unloading " + this.getClass().getSimpleName() + " = " + this.getName());
        if (reference != 0)
            return;

        unload(remainingMilliseconds);
        loaded = false;
        loadStarted = false;
    }
}
