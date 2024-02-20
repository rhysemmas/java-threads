package org.example;

import org.example.counter.Counter;

public class RunnableCounterIncrementor implements Runnable {
    private final Counter counter;
    private final int valueToIncrementTo;

    public RunnableCounterIncrementor(Counter counter, int valueToIncrementTo) {
        this.counter = counter;
        this.valueToIncrementTo = valueToIncrementTo;
    }

    public void run() {
        int i = 0;
        while (i < valueToIncrementTo) {
//            System.out.println("thread with id '" + Thread.currentThread().getName() + "' sees counter with value: " + counter.getValue());
//            System.out.println("thread with id '" + Thread.currentThread().getName() + "' trying to increment counter");
            counter.increment(1);
//            System.out.println("thread with id '" + Thread.currentThread().getName() + "' sees counter with value: " + counter.getValue());
            i++;
        }
    }
}
