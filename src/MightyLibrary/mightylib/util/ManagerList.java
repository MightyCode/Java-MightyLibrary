package MightyLibrary.mightylib.util;

import java.util.ArrayList;

public class ManagerList<E>{
    private ArrayList<ObjectId> objects;

    public ManagerList(){
        objects = new ArrayList<>();
    }


    public E get(Id id){
        return get(id.id);
    }


    public E get(int id){
        return (E)objects.get(id);
    }


    public Id getIdFromString(String key){
        for (ObjectId object : objects){
            if(object.getName().equals(key)){
                return object.getId();
            }
        }
        return new Id(-1);
    }


    public Id add(ObjectId object){
        object.setId(objects.size());
        objects.add(object);

        return object.getId();
    }


    public String toString(){
        StringBuilder result = new StringBuilder("ManagerList:\n");

        for (ObjectId object : objects) {
            result.append(object.getName()).append(", with id ").append(object.getId().id).append("\n");
        }

        return result.toString();
    }


    public int size(){
        return  objects.size();
    }

    public void clear(){
        objects.clear();
    }
}
