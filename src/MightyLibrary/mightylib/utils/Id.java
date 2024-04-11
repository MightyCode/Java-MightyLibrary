package MightyLibrary.mightylib.utils;

public class Id {
    public int id;

    public Id(int id){
        this.id = id;
    }

    public Id increment(){
        ++id;
        return this;
    }

    public Id decrement(){
        --id;
        return this;
    }
}
