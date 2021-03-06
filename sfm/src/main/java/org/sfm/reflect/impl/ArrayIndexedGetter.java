package org.sfm.reflect.impl;

import org.sfm.reflect.IndexedGetter;

import java.lang.reflect.Array;

public class ArrayIndexedGetter<P> implements IndexedGetter<Object,P> {
    @SuppressWarnings("unchecked")
    @Override
    public P get(Object target, int index) {
        return (P) Array.get(target, index);
    }
}
