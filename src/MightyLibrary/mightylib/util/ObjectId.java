package MightyLibrary.mightylib.util;

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

    public ObjectId setId(int id){
        this.id.id = id;
        return this;
    }

    public ObjectId setId(Id id){
        this.id.id = id.id;
        return this;
    }

    public Id getId(){
        return id;
    }

    public ObjectId linkId(Id id){
        this.id = id;
        return this;
    }


    public String getName(){
        return name;
    }

    public ObjectId setName(String name){
        this.name = name;
        return this;
    }
}
