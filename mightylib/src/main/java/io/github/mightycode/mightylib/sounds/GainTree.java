package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.resources.sound.SoundLoader;
import io.github.mightycode.mightylib.utils.math.KeyTree;
import io.github.mightycode.mightylib.utils.math.KeyTreeNode;
import org.json.JSONObject;

import java.util.List;

// The gains of the sound services: each node multiplies the gain of its sources and of its children
final class GainTree {
    private final KeyTree<String, Float> tree = new KeyTree<>();

    // Null content: no node, every sound keeps a gain of 1
    GainTree(JSONObject content) {
        if (content != null)
            SoundLoader.loadGainTree(content, tree);
    }

    List<KeyTreeNode<String, Float>> nodes() {
        return tree.nodes();
    }

    // Product of the values of the node and its ancestors (1 for an unknown node)
    float gain(String name) {
        float gain = 1.0f;
        KeyTreeNode<String, Float> node = tree.getNode(name);

        while (node != null) {
            if (node.getValue() != null)
                gain *= node.getValue();

            node = node.parent();
        }

        return gain;
    }

    // Clamped to [0, 1]; false if the node is unknown or keeps its value
    boolean change(String name, float value) {
        KeyTreeNode<String, Float> node = tree.getNode(name);

        float clamped = value > 1 ? 1 : value < 0 ? 0 : value;
        if (node == null || (node.getValue() != null && clamped == node.getValue()))
            return false;

        node.setValue(clamped);
        return true;
    }

    void print() {
        tree.printTree("List of gains");
    }
}
