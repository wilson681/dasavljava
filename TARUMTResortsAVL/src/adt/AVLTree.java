package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

// AVLTree - self balancing BST, implements SearchTreeInterface
// used for module 2 (vip allocation)
// tree itself doesnt decide order, thats all compareTo() on whatever data is stored (eg Booking)
// so higher priority = smaller compareTo value, means first item from getInorderIterator()
// is already the highest priority one, dont need a separate getMin()
// rebalance runs after every add/remove to keep height around log n
public class AVLTree<T extends Comparable<? super T>> implements SearchTreeInterface<T> {

    private Node<T> root;

    public AVLTree() {
        root = null;
    }

    @Override
    public T getRootData() {
        if (root == null) {
            return null;
        }
        return root.data;
    }

    @Override
    public int getHeight() {
        return heightOf(root);
    }

    @Override
    public int getNumberOfNodes() {
        // recalculates by recursion every time, no separate counter field
        // tree isn't huge so O(n) each call is fine
        return countNodes(root);
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public void clear() {
        root = null;
    }

    @Override
    public boolean add(T newEntry) {
        // addToSubtree might swap out the root because of rotation, so reassign root here
        root = addToSubtree(root, newEntry);
        return true;
    }

    @Override
    public T remove(T anEntry) {
        // check it actually exists first, need to know whether to return something to caller
        T found = getEntry(anEntry);
        if (found == null) {
            return null;
        }
        root = removeFromSubtree(root, anEntry);
        return found;
    }

    @Override
    public T getEntry(T anEntry) {
        // plain BST search, smaller goes left, bigger goes right
        Node<T> current = root;
        while (current != null) {
            int comparison = anEntry.compareTo(current.data);
            if (comparison == 0) {
                return current.data;
            } else if (comparison < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return null;
    }

    @Override
    public boolean contains(T anEntry) {
        return getEntry(anEntry) != null;
    }

    @Override
    public Iterator<T> getInorderIterator() {
        // count nodes first, allocate array of that size, then recurse once to fill it
        T[] entries = createArray(countNodes(root));
        fillInorder(root, entries, new int[]{0});
        return new ArrayIterator(entries);
    }

    @Override
    public Iterator<T> getPreorderIterator() {
        T[] entries = createArray(countNodes(root));
        fillPreorder(root, entries, new int[]{0});
        return new ArrayIterator(entries);
    }

    @Override
    public Iterator<T> getPostorderIterator() {
        T[] entries = createArray(countNodes(root));
        fillPostorder(root, entries, new int[]{0});
        return new ArrayIterator(entries);
    }

    // private helpers below

    // inserts newEntry into the subtree rooted at node, rebalances after, returns
    // whatever node should sit at this position now
    private Node<T> addToSubtree(Node<T> node, T newEntry) {
        if (node == null) {
            return new Node<>(newEntry);
        }
        if (newEntry.compareTo(node.data) < 0) {
            node.left = addToSubtree(node.left, newEntry);
        } else {
            // different Bookings shouldnt compareTo() equal since Booking.compareTo() tiebreaks
            // on bookingId. if it does come out 0 somehow, treat as same rank, goes right
            node.right = addToSubtree(node.right, newEntry);
        }
        return rebalance(node);
    }

    // standard BST delete, 3 cases: no children, one child, two children
    private Node<T> removeFromSubtree(Node<T> node, T anEntry) {
        if (node == null) {
            return null;
        }
        int comparison = anEntry.compareTo(node.data);
        if (comparison < 0) {
            node.left = removeFromSubtree(node.left, anEntry);
        } else if (comparison > 0) {
            node.right = removeFromSubtree(node.right, anEntry);
        } else {
            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }
            // both sides have children - take the smallest node from the right subtree
            // (successor) to replace node's data, then delete that duplicate successor
            Node<T> successor = findMin(node.right);
            node.data = successor.data;
            node.right = removeFromSubtree(node.right, successor.data);
        }
        return rebalance(node);
    }

    // walk left from node until there's nowhere left to go, that's the smallest in this subtree
    private Node<T> findMin(Node<T> node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    // checks node's balance factor, rotates to fix if unbalanced
    // balance = left height - right height
    // balance > 1 means left heavy, balance < -1 means right heavy
    private Node<T> rebalance(Node<T> node) {
        updateHeight(node);
        int balance = balanceFactor(node);

        if (balance > 1) {
            // LR case - left rotate the left child first to turn it into LL, then right rotate here
            if (balanceFactor(node.left) < 0) {
                node.left = rotateLeft(node.left);
            }
            return rotateRight(node);
        }
        if (balance < -1) {
            // RL case - right rotate the right child first to turn it into RR, then left rotate here
            if (balanceFactor(node.right) > 0) {
                node.right = rotateRight(node.right);
            }
            return rotateLeft(node);
        }
        return node;
    }

    // right rotate: node's left child becomes the new root, node becomes its right child
    private Node<T> rotateRight(Node<T> node) {
        Node<T> newRoot = node.left;
        node.left = newRoot.right;
        newRoot.right = node;
        // need node's height updated first so newRoot's height comes out right (node is its child now)
        updateHeight(node);
        updateHeight(newRoot);
        return newRoot;
    }

    // left rotate, mirror of rotateRight
    private Node<T> rotateLeft(Node<T> node) {
        Node<T> newRoot = node.right;
        node.right = newRoot.left;
        newRoot.left = node;
        updateHeight(node);
        updateHeight(newRoot);
        return newRoot;
    }

    private int heightOf(Node<T> node) {
        if (node == null) {
            return 0;
        }
        return node.height;
    }

    private void updateHeight(Node<T> node) {
        node.height = 1 + Math.max(heightOf(node.left), heightOf(node.right));
    }

    private int balanceFactor(Node<T> node) {
        return heightOf(node.left) - heightOf(node.right);
    }

    private int countNodes(Node<T> node) {
        if (node == null) {
            return 0;
        }
        return 1 + countNodes(node.left) + countNodes(node.right);
    }

    // java wont let you new T[] directly, new a Comparable[] and cast instead
    @SuppressWarnings("unchecked")
    private T[] createArray(int size) {
        return (T[]) new Comparable[size];
    }

    // inorder: left -> self -> right
    // position uses a length-1 array as a mutable counter (method params cant be used as out params)
    private void fillInorder(Node<T> node, T[] array, int[] position) {
        if (node == null) {
            return;
        }
        fillInorder(node.left, array, position);
        array[position[0]] = node.data;
        position[0]++;
        fillInorder(node.right, array, position);
    }

    // preorder: self -> left -> right
    private void fillPreorder(Node<T> node, T[] array, int[] position) {
        if (node == null) {
            return;
        }
        array[position[0]] = node.data;
        position[0]++;
        fillPreorder(node.left, array, position);
        fillPreorder(node.right, array, position);
    }

    // postorder: left -> right -> self
    private void fillPostorder(Node<T> node, T[] array, int[] position) {
        if (node == null) {
            return;
        }
        fillPostorder(node.left, array, position);
        fillPostorder(node.right, array, position);
        array[position[0]] = node.data;
        position[0]++;
    }

    // tree node, keeps its own height so balance factor doesnt need recalculating from scratch each time
    private class Node<E> {
        private E data;
        private Node<E> left;
        private Node<E> right;
        private int height;

        private Node(E data) {
            this.data = data;
            this.height = 1;
        }
    }

    // dumps traversal result into a plain array, wraps it as an Iterator
    private class ArrayIterator implements Iterator<T> {
        private T[] items;
        private int currentIndex;

        private ArrayIterator(T[] items) {
            this.items = items;
            this.currentIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < items.length;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return items[currentIndex++];
        }
    }
}
