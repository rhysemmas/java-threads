package org.example;

public class UnsynchronisedCounter implements Counter {
    private int count;

    public int getValue() {
        return this.count;
    }

    public void increment(int i) {
        this.count += i;
    }

    public void decrement(int i) {
        this.count -= i;
    }
}
