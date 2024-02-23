package org.example.queue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

// Trying to implement a lock-free queue
public class ThreadSafeQueue<T> implements MyQueue<T> {
    // TODO: can this just exist in the constructor after creating the reference, or will it get gc?
    private State state;
    private final AtomicReference<State> stateReference;

    public ThreadSafeQueue(int size) {
        state = new State(size);
        stateReference = new AtomicReference<>(state);
    }

    public T next() throws IllegalStateException {
        boolean success = false;
        T value = null;

        while (!success) {
            State currentState = stateReference.get();
            if (currentState.startingIndex >= currentState.nextEmptyIndex) {
                throw new IllegalStateException("queue is empty");
            }

            value = currentState.queue[currentState.startingIndex];

            State newState = new State(currentState.queue, currentState.startingIndex++, currentState.nextEmptyIndex);
            success = stateReference.compareAndSet(currentState, newState);
        }

        return value;
    }

    public T peek() {
        State currentState = stateReference.get();
        System.out.println(Arrays.toString(currentState.queue));
        return currentState.queue[currentState.startingIndex];
    }

    public void add(T value) throws IllegalStateException {
        boolean success = false;

        while (!success) {
            State currentState = stateReference.get();
            State newState = new State(currentState.queue.length);

            int si = currentState.startingIndex;
            int nei = currentState.nextEmptyIndex;

            if (currentState.startingIndex != 0 && currentState.nextEmptyIndex + 1 >= currentState.queue.length) {
                T[] compactedQueue = compact(currentState.startingIndex, currentState.nextEmptyIndex, currentState.queue);
                si = 0;
                nei = newState.queue.length - currentState.startingIndex;
            }

            if (nei >= currentState.queue.length) {
                throw new IllegalStateException("queue is full");
            }

            newState.startingIndex = si;
            newState.nextEmptyIndex = nei;
            newState.queue[nei] = value;
            newState.nextEmptyIndex++;

            success = stateReference.compareAndSet(currentState, newState);
        }
    }

    private T[] compact(int si, int nei, T[] queueCopy) {
        T[] compactedQueue = (T[]) new Object[queueCopy.length];

        for (int i = 0; si + i < queueCopy.length; i++) {
            compactedQueue[i] = queueCopy[si + i];
        }

        return compactedQueue;
    }

    class State {
        public T[] queue;
        public int startingIndex;
        public int nextEmptyIndex;

        State(int newQueueSize) {
            queue = (T[]) new Object[newQueueSize];
            startingIndex = 0;
            nextEmptyIndex = 0;
        }

        State(T[] queue, int startingIndex, int nextEmptyIndex) {
            this.queue = queue;
            this.startingIndex = startingIndex;
            this.nextEmptyIndex = nextEmptyIndex;
        }
    }
}
