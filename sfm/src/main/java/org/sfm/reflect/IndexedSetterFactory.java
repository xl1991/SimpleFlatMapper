package org.sfm.reflect;


public interface IndexedSetterFactory<T, A> {
    <P> IndexedSetter<T, P> getIndexedSetter(A arg);
}
