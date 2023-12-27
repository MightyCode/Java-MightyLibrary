package MightyLibrary.mightylib.algorithms.graph;

import java.util.*;

/**
 * Class that can only contain itself
 * @param <T> T is a child class of ListNode
 */
public abstract class ListNode<T extends ListNode<?>>{
    private static long ID = 0;
    private final long objectId;

    private final long id;
    private final ArrayList<Long> ids;
    private final SortedMap<Long, T> nodes;

    public ListNode(long id){
        this.id = id;
        objectId = ID++;

        nodes = new TreeMap<>();
        ids = new ArrayList<>();
    }

    public long getObjectId(){
        return objectId;
    }

    public long getId(){
        return id;
    }


    public void clear(){
        nodes.clear();
    }

    public int size(){
        return nodes.size();
    }

    public boolean add(T node){
        if (ids.add(node.getId())) {
            nodes.put(node.getId(), node);
            return true;
        }

        return false;
    }

    public void addAll(Collection<T> nodes){
        for (T node : nodes)
            add(node);
    }

    public T get(int i){
        return nodes.get(ids.get(i));
    }

    public boolean contains(T n){
        return nodes.containsKey(n.getId());
    }

    public boolean containsId(long id){
        return nodes.containsKey(id);
    }

    public int indexOf(long id){
        return ids.indexOf(id);
    }

    public int indexOf(T node){
        return ids.indexOf(node.getId());
    }

    public T getById(long id){ return nodes.get(id); }

    public boolean removeById(long id) {
        nodes.remove(id);
        return ids.remove(id);
    }

    public boolean remove(T node) {
        nodes.remove(node.getId());
        return ids.remove(node.getId());
    }

    public Collection<T> getNodes(){
        return nodes.values();
    }

    public Collection<Long> getIds(){ return ids; }
}
