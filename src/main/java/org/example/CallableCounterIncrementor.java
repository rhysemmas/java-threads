package org.example;

import org.example.counter.Counter;
import org.example.counter.UnsynchronisedCounter;

import java.util.concurrent.Callable;

public class CallableCounterIncrementor implements Callable<Integer> {
    private final Counter counter;
    private final int valueToIncrementTo;

    CallableCounterIncrementor(int valueToIncrementTo) {
        this.counter = new UnsynchronisedCounter();
        this.valueToIncrementTo = valueToIncrementTo;
    }

    public Integer call() {
        int i = 0;
        while (i < valueToIncrementTo) {
            counter.increment(1);
            i++;
        }

        return i;
    }
}
