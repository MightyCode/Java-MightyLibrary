package MightyLibrary.mightylib.graphics;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.utils.LoadingElement;

import java.util.ArrayList;

public abstract class GLElement implements LoadingElement {
    private String name;
    private boolean loaded;
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

    int getNumberReference() {
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

    void loadByResource(int remainingMilliseconds) {
        System.out.println("Loading gl (" + this.getClass().getSimpleName() + ") " + this.getName());
        for (DataType type : resourceDependencies) {
            if (!type.isLoaded()){
                return;
            }
        }

        for (GLElement element : glElementDependencies){
            if (!element.isLoaded()){
                return;
            }
        }

        loaded = load(remainingMilliseconds);
    }

    public abstract void unload(int remainingMilliseconds);

    void unloadByResource(int remainingMilliseconds) {
        System.out.println("Gl Elements Unloading " + this.getClass().getSimpleName() + " = " + this.getName());
        if (reference != 0)
            return;

        unload(remainingMilliseconds);
        loaded = false;
    }
}
