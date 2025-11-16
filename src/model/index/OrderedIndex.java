package model.index;

import java.io.Serializable;
import java.util.Map;

public interface OrderedIndex<K extends Comparable<K>, V> extends Serializable {

    V get(K key);
    void put(K key, V value);
    V remove(K key);
    boolean containsKey(K key);
    int size();
    default boolean isEmpty() {
        return size() == 0;
    }

    Iterable<Map.Entry<K,V>> entries();
    Iterable<Map.Entry<K,V>> range(K from, boolean fromInc, K to, boolean toInc);
}
