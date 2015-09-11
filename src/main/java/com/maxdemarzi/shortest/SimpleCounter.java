package com.maxdemarzi.shortest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

class SimpleCounter<T> implements Counter<T> {
    public static final LongAdder DEFAULT_VALUE = new LongAdder();

    private final Map<T, LongAdder> counts = new HashMap<>();

    @Override
    public void increment(T key) {
        counts.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    @Override
    public void decrement(T key) {
        counts.computeIfAbsent(key, k -> new LongAdder()).decrement();
    }

    @Override
    public int getCount(T key) {
        return counts.getOrDefault(key, DEFAULT_VALUE).intValue();
    }

    @Override
    public void remove(T key) {
        counts.remove(key);
    }

    @Override
    public Set<T> getKeys() {
        return counts.keySet();
    }

    @Override
    public Iterator<Count<T>> iterator() {
        return counts.entrySet().stream().map(e -> new SimpleCount<T>(e)).collect(Collectors.<Count<T>>toSet()).iterator();
    }

    private static class SimpleCount<E> implements Count<E> {
        private final Map.Entry<E, LongAdder> entry;

        SimpleCount(Map.Entry<E, LongAdder> entry) {
            this.entry = entry;
        }

        @Override
        public E getKey() {
            return entry.getKey();
        }

        @Override
        public int getCount() {
            return entry.getValue().intValue();
        }
    }
}