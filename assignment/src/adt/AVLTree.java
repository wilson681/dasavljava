package adt;

public class AVLTree<K extends Comparable<K>, V> implements SearchTreeInterface<K, V> {
    private int size;

    public AVLTree() {
        size = 0;
    }

    @Override
    public boolean insert(K key, V value) {
        throw todo();
    }

    @Override
    public V search(K key) {
        throw todo();
    }

    @Override
    public boolean delete(K key) {
        throw todo();
    }

    @Override
    public boolean contains(K key) {
        throw todo();
    }

    @Override
    public void traverseInOrder(Visitor<K, V> visitor) {
        throw todo();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        size = 0;
    }

    private UnsupportedOperationException todo() {
        return new UnsupportedOperationException("TODO: implement basic AVL logic here: node, height, rotations, insert, search, delete and traversal.");
    }
}
