package com.maxdemarzi.shortest;

import java.util.Set;

public interface Counter<T> extends Iterable<Counter.Count<T>> {
    void increment(T key);

    void decrement(T key);

    int getCount(T key);

    void remove(T key);

    Set<T> getKeys();

    interface Count<T> {
        T getKey();

        int getCount();
    }


}