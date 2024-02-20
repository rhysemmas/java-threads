package org.example.counter;

public class SynchronisedCounter implements Counter {
    private int count;

    public synchronized int getValue() {
        return this.count;
    }

    public synchronized void increment(int i) {
        this.count += i;
    }

    public synchronized void decrement(int i) {
        this.count -= i;
    }
}
