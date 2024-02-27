package org.example.queue;

import java.util.StringJoiner;

public class UnsynchronisedQueue<T> implements MyQueue<T> {
    private T[] itemQueue;
    private int startingIndex;
    private int nextEmptyIndex;

    public UnsynchronisedQueue(int size) {
        // TODO: should probably just use an ArrayList of T
        itemQueue = (T[]) new Object[size];
        startingIndex = 0;
        nextEmptyIndex = 0;
    }

    // Return the next item and remove it from the Queue
    public T next() throws IllegalStateException {
        if (startingIndex >= nextEmptyIndex) {
            throw new IllegalStateException("queue is empty");
        }

        T value = itemQueue[startingIndex];
        startingIndex++;
        return value;
    }

    // Check what is the next item in the queue
    public T peek() {
        if (startingIndex >= itemQueue.length) {
            return null;
        }
        
        return itemQueue[startingIndex];
    }

    // Add a new item to the Queue
    public void add(T value) throws IllegalStateException {
        // If we have previously pulled things off the queue, and we are about to run out of space,
        // reclaim some space from the front
        if (startingIndex != 0 && nextEmptyIndex >= itemQueue.length) {
            compact();
        }

        if (nextEmptyIndex >= itemQueue.length) {
            throw new IllegalStateException("queue is full");
        }

        itemQueue[nextEmptyIndex] = value;
        nextEmptyIndex++;
    }

    private void compact() {
        T[] compactedQueue = (T[]) new Object[itemQueue.length];

        for (int i = 0; startingIndex + i < itemQueue.length; i++) {
            compactedQueue[i] = itemQueue[startingIndex + i];
        }

        itemQueue = compactedQueue;

        //        i,   e
        // [0, 1, 2, 3]
        // [2, 3, null, null]


        //        si    nei       si    nei
        // [null, 1, 2]    ->    [1, 2, null]
        // (length = 3) - (startingIndex = 1) = (nextEmptyIndex = 2)
        nextEmptyIndex = compactedQueue.length - startingIndex;

        startingIndex = 0;
    }

    // TODO: remove, for debugging to print out the entire queue
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",");
        for (int i = 0; i < itemQueue.length; i++) {
            var item = itemQueue[i];
            if (item != null) {
                joiner.add(itemQueue[i].toString());
            }
        }
        joiner.add("starting index: " + startingIndex);
        joiner.add("next empty index: " + nextEmptyIndex);
        return joiner.toString();
    }
}
