package model.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

public class BPlusTree<K extends Comparable<K>, V> implements OrderedIndex<K, V>, Serializable {

    private static final int DEFAULT_ORDER = 32;
    private final int order;

    private abstract static class Node<K extends Comparable<K>, V> implements Serializable {
        final ArrayList<K> keys = new ArrayList<>();
        abstract boolean isLeaf();
    }

    private static final class LeafNode<K extends Comparable<K>, V> extends Node<K, V> {

        final ArrayList<V> values = new ArrayList<>();
        LeafNode<K, V> next;

        @Override
        boolean isLeaf() {
            return true;
        }
    }

    private static final class InternalNode<K extends Comparable<K>, V> extends Node<K, V> {

        final ArrayList<Node<K, V>> children = new ArrayList<>();

        @Override
        boolean isLeaf() {
            return false;
        }
    }

    private Node<K, V> root = new LeafNode<>();
    private int size = 0;

    public BPlusTree() {
        this(DEFAULT_ORDER);
    }

    public BPlusTree(int order) {
        if (order < 4) {
            throw new IllegalArgumentException("[ERROR] order >= 4");
        }
        this.order = order;
    }

    @Override
    public V get(K key) {
        LeafNode<K, V> leaf = findLeaf(root, key);
        int idx = Collections.binarySearch(leaf.keys, key);

        if (idx >= 0) {
            return leaf.values.get(idx);
        }

        return null;
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public void put(K key, V value) {
        SplitResult<K, V> split = insert(root, key, value);
        if (split != null) {
            InternalNode<K, V> newRoot = new InternalNode<>();
            newRoot.keys.add(split.pivot);
            newRoot.children.add(split.left);
            newRoot.children.add(split.right);
            root = newRoot;
        }
    }

    @Override
    public V remove(K key) {
        return deleteNoRebalance(root, key);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Iterable<Entry<K, V>> entries() {
        LeafNode<K,V> l = leftmostLeaf(root);
        return () -> new Iterator<>() {
            LeafNode<K,V> curLeaf = l;
            int i = 0;
            @Override public boolean hasNext() {
                while (curLeaf != null) {
                    if (i < curLeaf.keys.size()) return true;
                    curLeaf = curLeaf.next; i = 0;
                }
                return false;
            }
            @Override public Map.Entry<K,V> next() {
                if (!hasNext()) throw new NoSuchElementException();
                Map.Entry<K,V> e = Map.entry(curLeaf.keys.get(i), curLeaf.values.get(i));
                i++;
                return e;
            }
        };
    }

    @Override
    public Iterable<Entry<K, V>> range(K from, boolean fromInc, K to, boolean toInc) {
        if (from.compareTo(to) > 0) return List.<Map.Entry<K,V>>of();
        LeafNode<K,V> leaf = findLeaf(root, from);
        int i = lowerBound(leaf.keys, from);
        return () -> new Iterator<>() {
            LeafNode<K,V> curLeaf = leaf;
            int idx = adjustStart(i, from, fromInc);

            @Override public boolean hasNext() {
                while (curLeaf != null) {
                    if (idx < curLeaf.keys.size()) {
                        K k = curLeaf.keys.get(idx);
                        int cmp = k.compareTo(to);
                        if (cmp < 0) return true;
                        if (cmp == 0) return toInc;
                        return false;
                    }
                    curLeaf = curLeaf.next; idx = 0;
                }
                return false;
            }
            @Override public Map.Entry<K,V> next() {
                if (!hasNext()) throw new NoSuchElementException();
                Map.Entry<K,V> e = Map.entry(curLeaf.keys.get(idx), curLeaf.values.get(idx));
                idx++;
                return e;
            }
        };
    }

    private int adjustStart(int i, K from, boolean fromInc) {
        if (!fromInc) {
            return (i < 0) ? 0 : i + ((i < rootMaxKeys() && getKeyAt(i).compareTo(from) == 0) ? 1 : 0);
        }
        return Math.max(i, 0);
    }

    private LeafNode<K, V> findLeaf(Node<K, V> n, K key) {
        Node<K, V> cur = n;
        while (!cur.isLeaf()) {
            InternalNode<K, V> internalNode = (InternalNode<K, V>) cur;
            int idx = childIndex(internalNode.keys, key);
            cur = internalNode.children.get(idx);
        }

        return (LeafNode<K, V>) cur;
    }

    private int childIndex(List<K> keys, K key) {
        int pos = Collections.binarySearch(keys, key);

        if (pos >= 0) {
            return pos + 1;
        }

        return -(pos + 1);
    }

    private static final class SplitResult<K extends Comparable<K>, V> {
        final K pivot;
        final Node<K, V> left;
        final Node<K, V> right;

        public SplitResult(K pivot, Node<K, V> left, Node<K, V> right) {
            this.pivot = pivot;
            this.left = left;
            this.right = right;
        }
    }

    private SplitResult<K, V> insert(Node<K, V> node, K key, V value) {
        if (node.isLeaf()) {
            LeafNode<K,V> leaf = (LeafNode<K, V>) node;
            int pos = Collections.binarySearch(leaf.keys, key);
            if (pos >= 0) {
                leaf.values.set(pos, value);
                return null;
            } else {
                int ip = -(pos + 1);
                leaf.keys.add(ip, key);
                leaf.values.add(ip, value);
                size++;
                if (leaf.keys.size() > order) {
                    return splitLeaf(leaf);
                }
                return null;
            }
        } else {
            InternalNode<K,V> in = (InternalNode<K,V>) node;
            int childIdx = childIndex(in.keys, key);
            SplitResult<K,V> childSplit = insert(in.children.get(childIdx), key, value);
            if (childSplit == null) return null;

            in.keys.add(childIdx, childSplit.pivot);
            in.children.set(childIdx, childSplit.left);
            in.children.add(childIdx + 1, childSplit.right);

            if (in.keys.size() > order) {
                return splitInternal(in);
            }
            return null;
        }
    }

    private SplitResult<K,V> splitLeaf(LeafNode<K,V> leaf) {
        int mid = (leaf.keys.size() + 1) / 2;
        LeafNode<K,V> right = new LeafNode<>();
        // move right half
        while (leaf.keys.size() > mid) {
            right.keys.add(leaf.keys.remove(mid));
            right.values.add(leaf.values.remove(mid));
        }
        // link
        right.next = leaf.next;
        leaf.next = right;

        K pivot = right.keys.get(0); // 오른쪽 첫 키가 승격됨
        return new SplitResult<>(pivot, leaf, right);
    }

    private SplitResult<K,V> splitInternal(InternalNode<K,V> in) {
        int mid = in.keys.size() / 2;
        K pivot = in.keys.get(mid);

        InternalNode<K,V> right = new InternalNode<>();
        // keys: mid 이후 오른쪽으로 이동 (pivot 제외)
        // children: mid+1 이후 이동
        // left는 in 자체를 사용
        ArrayList<K> rKeys = new ArrayList<>(in.keys.subList(mid + 1, in.keys.size()));
        ArrayList<Node<K,V>> rChildren = new ArrayList<>(in.children.subList(mid + 1, in.children.size()));

        // 왼쪽(in) 자르기
        in.keys.subList(mid, in.keys.size()).clear(); // pivot 포함해서 지움 → left keys는 [0..mid-1]
        in.children.subList(mid + 1, in.children.size()).clear(); // left children는 [0..mid]

        right.keys.addAll(rKeys);
        right.children.addAll(rChildren);

        return new SplitResult<>(pivot, in, right);
    }

    private LeafNode<K,V> leftmostLeaf(Node<K,V> n) {
        Node<K,V> cur = n;
        while (!cur.isLeaf()) cur = ((InternalNode<K,V>) cur).children.get(0);
        return (LeafNode<K,V>) cur;
    }

    private int lowerBound(List<K> keys, K key) {
        int pos = Collections.binarySearch(keys, key);
        return (pos >= 0) ? pos : -(pos + 1);
    }

    // (데모) 리밸런싱 없는 삭제
    private V deleteNoRebalance(Node<K,V> n, K key) {
        if (n.isLeaf()) {
            LeafNode<K,V> leaf = (LeafNode<K,V>) n;
            int pos = Collections.binarySearch(leaf.keys, key);
            if (pos < 0) return null;
            V prev = leaf.values.remove(pos);
            leaf.keys.remove(pos);
            size--;
            return prev;
        } else {
            InternalNode<K,V> in = (InternalNode<K,V>) n;
            int idx = childIndex(in.keys, key);
            return deleteNoRebalance(in.children.get(idx), key);
        }
    }

    private int rootMaxKeys() {
        return (root.isLeaf()) ? ((LeafNode<K,V>) root).keys.size()
                : ((InternalNode<K,V>) root).keys.size();
    }
    private K getKeyAt(int i) {
        if (root.isLeaf()) return ((LeafNode<K,V>) root).keys.get(i);
        return ((InternalNode<K,V>) root).keys.get(i);
    }
}
