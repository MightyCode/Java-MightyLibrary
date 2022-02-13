package MightyLibrary.mightylib.util.math;

public class KeyTreeNode<K, V> {
    private final K key;
    public final V value;

    private final KeyTreeNode<K, V> parent;

    public KeyTreeNode(KeyTreeNode<K, V> parent, K key, V value){
        this.parent = parent;
        this.key = key;
        this.value = value;
    }

    public K getKey(){
        return key;
    }

    public V getValue(){
        return value;
    }

    public KeyTreeNode<K, V> parent(){
        return parent;
    }
}
