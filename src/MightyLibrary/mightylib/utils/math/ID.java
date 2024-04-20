package MightyLibrary.mightylib.utils.math;

public class ID {
    public int id;

    public ID(int id){
        this.id = id;
    }

    public ID increment(){
        ++id;
        return this;
    }

    public ID decrement(){
        --id;
        return this;
    }
}
