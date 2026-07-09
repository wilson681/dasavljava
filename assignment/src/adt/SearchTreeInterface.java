package adt;

public interface SearchTreeInterface<K extends Comparable<K>, V> {
    boolean insert(K key, V value);

    V search(K key);

    boolean delete(K key);

    boolean contains(K key);

    void traverseInOrder(Visitor<K, V> visitor);

    int size();

    boolean isEmpty();

    void clear();
}
