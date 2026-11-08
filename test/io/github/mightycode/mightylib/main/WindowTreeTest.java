package io.github.mightycode.mightylib.main;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WindowTreeTest {
    private WindowTree<String> tree;
    private WindowTree.Node<String> main;

    @BeforeEach
    void setUp() {
        tree = new WindowTree<>();
        main = tree.addRoot("main");
    }

    private static List<String> names(List<WindowTree.Node<String>> nodes) {
        List<String> names = new ArrayList<>();
        for (WindowTree.Node<String> node : nodes)
            names.add(node.value);

        return names;
    }

    @Test
    void thereIsOnlyOneMainWindow() {
        assertThrows(IllegalStateException.class, () -> tree.addRoot("other"));
    }

    @Test
    void aWindowNeedsAnOpenParent() {
        WindowTree.Node<String> dialog = tree.add("dialog", main, false, false, null);
        tree.remove(dialog);

        assertThrows(IllegalArgumentException.class, () -> tree.add("child", dialog, false, false, null));
        assertThrows(IllegalArgumentException.class, () -> tree.add("child", null, false, false, null));
    }

    @Test
    void closingAWindowClosesItsChildrenFirst() {
        WindowTree.Node<String> tools = tree.add("tools", main, false, false, null);
        WindowTree.Node<String> palette = tree.add("palette", tools, false, false, null);
        tree.add("tooltip", palette, false, false, null);
        tree.add("other", main, false, false, null);

        assertEquals(java.util.Arrays.asList("tooltip", "palette", "tools"), names(tree.closeOrder(tools)));
    }

    @Test
    void closingTheMainWindowClosesEverythingLastItself() {
        tree.add("a", main, false, false, null);
        tree.add("b", main, false, false, null);

        List<String> order = names(tree.closeOrder(main));
        assertEquals("main", order.get(order.size() - 1));
        assertEquals(3, order.size());
    }

    @Test
    void removeForgetsTheWindowAndItsDescendants() {
        WindowTree.Node<String> tools = tree.add("tools", main, false, false, null);
        tree.add("palette", tools, false, false, null);
        tree.add("other", main, false, false, null);

        tree.remove(tools);

        assertEquals(java.util.Arrays.asList("main", "other"), names(tree.all()));
        assertEquals(1, main.children().size());
        assertNull(tree.find("palette"));
    }

    @Test
    void aModalWindowBlocksItsParentAndItsAncestorsOnly() {
        WindowTree.Node<String> tools = tree.add("tools", main, false, false, null);
        WindowTree.Node<String> other = tree.add("other", main, false, false, null);
        WindowTree.Node<String> dialog = tree.add("dialog", tools, true, false, null);

        assertTrue(tree.isBlocked(tools));
        assertTrue(tree.isBlocked(main));
        assertFalse(tree.isBlocked(dialog));
        assertFalse(tree.isBlocked(other));
    }

    @Test
    void aWindowIsFreeAgainWhenItsModalChildCloses() {
        WindowTree.Node<String> first = tree.add("first", main, true, false, null);
        WindowTree.Node<String> second = tree.add("second", main, true, false, null);
        assertTrue(tree.isBlocked(main));

        tree.remove(first);
        assertTrue(tree.isBlocked(main));

        tree.remove(second);
        assertFalse(tree.isBlocked(main));
    }

    @Test
    void aNonModalChildDoesNotBlock() {
        tree.add("tools", main, false, false, null);

        assertFalse(tree.isBlocked(main));
    }

    @Test
    void aSceneClosesTheWindowsItOpenedUnlessTheyArePersistent() {
        Object sceneA = new Object();
        Object sceneB = new Object();
        tree.add("a1", main, false, false, sceneA);
        tree.add("a2", main, true, false, sceneA);
        tree.add("kept", main, false, true, sceneA);
        tree.add("b1", main, false, false, sceneB);

        assertEquals(java.util.Arrays.asList("a1", "a2"), names(tree.openedBy(sceneA)));
        assertEquals(java.util.Collections.singletonList("b1"), names(tree.openedBy(sceneB)));
    }

    @Test
    void theMainWindowBelongsToNoScene() {
        assertTrue(tree.openedBy(null).isEmpty());
        assertSame(main, tree.root());
    }
}
