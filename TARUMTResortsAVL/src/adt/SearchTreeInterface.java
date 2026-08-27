package adt;

import java.util.Iterator;

/**
 * SearchTreeInterface.java
 * ADT Search Tree - organizes entries by comparing them (Comparable), non-linear structure,
 * entries auto-sort themselves on insert, supports fast find/add/remove by comparison.
 *
 * @author Chong Kim Seng
 *
 * Source: read up on AVL tree implementations from
 * https://www.happycoders.eu/algorithms/avl-tree-java/ and
 * https://www.w3schools.com/dsa/dsa_data_avltrees.php before writing this
 *
 * note!!!: didnt name this AVLTreeInterface even tho thats the only impl right now,
 * keep it generic, dont want the name locked to one implementation
 *
 * @param <T> element type stored, must be comparable
 */
public interface SearchTreeInterface<T extends Comparable<? super T>> {

    /**
     * gets the data at the root, tree itself unchanged.
     * @return root data, null if tree is empty
     */
    public T getRootData();

    /**
     * gets the height of this tree.
     * @return tree height, 0 for empty tree
     */
    public int getHeight();

    /**
     * gets the current number of nodes in this tree.
     * @return node count
     */
    public int getNumberOfNodes();

    /**
     * checks if this tree is empty.
     * @return true if empty
     */
    public boolean isEmpty();

    /**
     * clears this tree, removes all nodes.
     */
    public void clear();

    /**
     * adds a new entry, tree auto-places it in the right spot.
     * @param newEntry entry to add
     * @return true if added
     */
    public boolean add(T newEntry);

    /**
     * removes the node equal to the given entry.
     * @param anEntry entry to remove (used for compare/equals)
     * @return the removed entry, null if not found
     */
    public T remove(T anEntry);

    /**
     * finds the node data equal to the given entry, tree itself unchanged.
     * @param anEntry entry to find (used for compare/equals)
     * @return the found entry, null if not found
     */
    public T getEntry(T anEntry);

    /**
     * checks if a node equal to the given entry exists in this tree.
     * @param anEntry entry to check
     * @return true if it exists
     */
    public boolean contains(T anEntry);

    /**
     * gets an iterator that walks this tree inorder (smallest to largest).
     * @return inorder iterator
     */
    public Iterator<T> getInorderIterator();

    /**
     * gets an iterator that walks this tree preorder.
     * @return preorder iterator
     */
    public Iterator<T> getPreorderIterator();

    /**
     * gets an iterator that walks this tree postorder.
     * @return postorder iterator
     */
    public Iterator<T> getPostorderIterator();
}
