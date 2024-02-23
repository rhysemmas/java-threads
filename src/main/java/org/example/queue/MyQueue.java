package org.example.queue;

public interface MyQueue<T> {
    T peek();

    T next() throws IllegalStateException;

    void add(T value) throws IllegalStateException;
}
