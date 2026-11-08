package io.github.mightycode.mightylib.main;

import java.util.ArrayList;
import java.util.List;

/**
 * The windows of a context and how they depend on each other, without anything of GLFW: the main window is the
 * root, each other window has a parent. Closing a window closes its children first. A window waits (is blocked)
 * while a modal window is open below it. A window can belong to the scene that opened it, which closes it when it
 * is left, unless the window is persistent.
 */
final class WindowTree<T> {
    static final class Node<T> {
        final T value;
        final Node<T> parent;
        final boolean modal;
        final boolean persistent;
        // What opened the window (a scene): it closes the window when it ends. Null for the main window.
        final Object opener;
        private final List<Node<T>> children = new ArrayList<>();

        private Node(T value, Node<T> parent, boolean modal, boolean persistent, Object opener) {
            this.value = value;
            this.parent = parent;
            this.modal = modal;
            this.persistent = persistent;
            this.opener = opener;
        }

        List<Node<T>> children() {
            return new ArrayList<>(children);
        }
    }

    private Node<T> root;
    // In the order of opening, the root first
    private final List<Node<T>> all = new ArrayList<>();

    Node<T> addRoot(T value) {
        if (root != null)
            throw new IllegalStateException("The window tree already has its main window");

        root = new Node<>(value, null, false, true, null);
        all.add(root);
        return root;
    }

    Node<T> add(T value, Node<T> parent, boolean modal, boolean persistent, Object opener) {
        if (parent == null || !all.contains(parent))
            throw new IllegalArgumentException("The parent window is not open in this context");

        Node<T> node = new Node<>(value, parent, modal, persistent, opener);
        parent.children.add(node);
        all.add(node);
        return node;
    }

    Node<T> root() {
        return root;
    }

    boolean contains(Node<T> node) {
        return all.contains(node);
    }

    List<Node<T>> all() {
        return new ArrayList<>(all);
    }

    Node<T> find(T value) {
        for (Node<T> node : all) {
            if (node.value == value)
                return node;
        }

        return null;
    }

    /** The window and its descendants, the deepest first: the order in which they must be closed. */
    List<Node<T>> closeOrder(Node<T> node) {
        List<Node<T>> order = new ArrayList<>();
        collect(node, order);
        return order;
    }

    private void collect(Node<T> node, List<Node<T>> order) {
        for (Node<T> child : node.children())
            collect(child, order);

        order.add(node);
    }

    /** Forget the window and its descendants (already closed by the caller). */
    void remove(Node<T> node) {
        for (Node<T> closed : closeOrder(node)) {
            all.remove(closed);
            if (closed.parent != null)
                closed.parent.children.remove(closed);
        }
    }

    /** True when a modal window is open below this one (child, grandchild...). */
    boolean isBlocked(Node<T> node) {
        for (Node<T> child : node.children) {
            if (child.modal || isBlocked(child))
                return true;
        }

        return false;
    }

    /** The windows opened by this scene and not persistent, in the order of opening. */
    List<Node<T>> openedBy(Object opener) {
        List<Node<T>> opened = new ArrayList<>();
        for (Node<T> node : all) {
            if (node.opener == opener && !node.persistent)
                opened.add(node);
        }

        return opened;
    }
}
