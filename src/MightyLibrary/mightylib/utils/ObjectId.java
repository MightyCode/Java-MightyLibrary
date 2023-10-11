package MightyLibrary.mightylib.utils;

public class ObjectId {
    private Id id;
    private String name;

    public ObjectId(){
        this(0, "null");
    }

    public ObjectId(int id, String name){
        this.id = new Id(id);
        this.name = name;
    }


    public ObjectId(int id){
        this(id, "null");
    }


    public ObjectId(String name){
        this(0, name);
    }


    public void setId(int id){
        this.id.id = id;
    }

    public void setId(Id id){
        this.id.id = id.id;
    }


    public Id getId(){
        return id;
    }


    public void linkId(Id id){
        this.id = id;
    }


    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
}
