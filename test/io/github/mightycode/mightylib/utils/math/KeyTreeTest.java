package io.github.mightycode.mightylib.utils.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyTreeTest {
    private final KeyTree<String, Integer> tree = new KeyTree<>();

    @Test
    void aRootHasNoParent() {
        KeyTreeNode<String, Integer> root = tree.addNewNode(tree.NoRoot, "root", 1);

        assertNotNull(root);
        assertNull(root.parent());
        assertEquals("root", root.getKey());
        assertEquals(1, root.getValue());
        assertSame(root, tree.getNode("root"));
    }

    @Test
    void childrenKnowTheirParent() {
        KeyTreeNode<String, Integer> root = tree.addNewNode(null, "root", 1);
        KeyTreeNode<String, Integer> child = tree.addNewNode("root", "child", 2);
        KeyTreeNode<String, Integer> grandChild = tree.addNewNode("child", "grand", 3);

        assertSame(root, child.parent());
        assertSame(child, grandChild.parent());
        assertSame(root, grandChild.parent().parent());
    }

    @Test
    void severalRootsAreAllowed() {
        assertNotNull(tree.addNewNode(null, "a", 1));
        assertNotNull(tree.addNewNode(null, "b", 2));
        assertNull(tree.getNode("a").parent());
        assertNull(tree.getNode("b").parent());
    }

    @Test
    void anExistingKeyIsRefused() {
        tree.addNewNode(null, "root", 1);

        assertNull(tree.addNewNode(null, "root", 2));
        assertNull(tree.addNewNode("root", "root", 2));
        assertEquals(1, tree.getNode("root").getValue(), "the first value is kept");
    }

    @Test
    void anUnknownPredecessorIsRefusedAndNothingIsAdded() {
        assertNull(tree.addNewNode("nobody", "child", 1));
        assertNull(tree.getNode("child"));
    }

    @Test
    void unknownNodeIsNull() {
        assertNull(tree.getNode("missing"));
    }

    @Test
    void valueCanBeChanged() {
        KeyTreeNode<String, Integer> node = tree.addNewNode(null, "root", 1);
        node.setValue(9);
        assertEquals(9, tree.getNode("root").getValue());
    }

    @Test
    void nodesAreListedInTheOrderTheyWereAddedAndCannotBeChanged() {
        KeyTreeNode<String, Integer> root = tree.addNewNode(null, "root", 1);
        KeyTreeNode<String, Integer> child = tree.addNewNode("root", "child", 2);
        tree.addNewNode("nobody", "orphan", 3);

        assertEquals(java.util.List.of(root, child), tree.nodes());
        assertThrows(UnsupportedOperationException.class, () -> tree.nodes().clear());
    }

    @Test
    void printingDoesNotFail() {
        tree.addNewNode(null, "root", 1);
        tree.addNewNode("root", "child", 2);
        assertDoesNotThrow(() -> tree.printTree("test tree"));
    }
}
