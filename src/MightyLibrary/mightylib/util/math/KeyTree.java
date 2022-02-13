package MightyLibrary.mightylib.util.math;

import java.util.ArrayList;

public class KeyTree<K, V>  {
    public final K noRoot;
    private final ArrayList<K> keys;
    private final ArrayList<KeyTreeNode<K, V>> values;

    public KeyTree(){
        noRoot = null;
        keys = new ArrayList<>();
        values = new ArrayList<>();
    }

    public KeyTreeNode<K, V> addNewNode(K predecessor, K currentKey, V value){
        if (keys.contains(currentKey))
            return null;

        KeyTreeNode<K, V> parent = null;
        for (KeyTreeNode<K, V> node : values){
            if (node.getKey().equals(predecessor)){
                parent = node;
                break;
            }
        }

        KeyTreeNode<K, V> result = null;

        if (predecessor == noRoot){
            result = new KeyTreeNode<>(null, currentKey, value);
            values.add(result);
            keys.add(currentKey);
        } else if (parent != null){
            result = new KeyTreeNode<>(null, currentKey, value);
            values.add(result);
            keys.add(currentKey);
        }

        return result;
    }

    public KeyTreeNode<K, V> getNode(K key){
        for (KeyTreeNode<K, V> node : values){
            if (node.getKey().equals(key))
                return node;
        }

        return null;
    }
}
