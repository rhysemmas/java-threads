package org.example;

import org.example.counter.Counter;
import org.example.counter.LockFreeConcurrentCounter;
import org.example.counter.SynchronisedCounter;
import org.example.counter.UnsynchronisedCounter;
import org.example.executor.MyExecutor;
import org.example.executor.QueuedExecutor;
import org.example.executor.QueuingThreadPool;
import org.example.executor.ThreadPoolExecutor;

import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) {
        int desiredCounterValue = 10000;
        StressTester stressor = new StressTester(desiredCounterValue);
        stressor.stress();
    }
}

class StressTester {
    private final int desiredCounterValue;

    StressTester(int desiredCounterValue) {
        this.desiredCounterValue = desiredCounterValue;
    }

    public void stress() {
        int counter = 0;

        // All tests currently assume that there will ultimately be 2 threads doing the 'work'
        ThreadSpawner spawner = new ThreadSpawner(this.desiredCounterValue / 2);

        while (true) {
            System.out.println("Test/Starting test with id: " + counter);
            int value = spawner.run();
            if (value != this.desiredCounterValue) {
                System.out.println("Test/Unexpected value: " + value);
                break;
            } else {
                System.out.println("Test/Got expected value of: " + value);
            }
            counter++;
        }
    }
}

class ThreadSpawner {
    private final int threadCounterValue;
    private final QueuingThreadPool<Integer> threadPool;

    ThreadSpawner(int threadCounterValue) {
        this.threadCounterValue = threadCounterValue;
        this.threadPool = new QueuingThreadPool<>();
    }

    public int run() {
        return this.testThreadPoolExecutor(this.threadPool);
    }

    private ThreadPoolExecutor<Integer> setupThreadPool() {
        return new QueuingThreadPool<>();
    }

    private int testThreadPoolExecutor(ThreadPoolExecutor<Integer> executor) {
        CallableCounterIncrementor c1 = new CallableCounterIncrementor(this.threadCounterValue);
        CallableCounterIncrementor c2 = new CallableCounterIncrementor(this.threadCounterValue);

        ThreadPoolExecutor.MyFuture<Integer> c1Future = executor.submit(c1);
        ThreadPoolExecutor.MyFuture<Integer> c2Future = executor.submit(c2);

        // After calling cancel() the coordinator will try not to schedule the job on a thread, but it is not
        // guaranteed. Even if the job does get scheduled, though, get() will return null.
        c1Future.cancel();

        try {
            Integer c1Value = c1Future.get();
            Integer c2Value = c2Future.get();

            if (c1Value == null || c2Value == null) {
                System.out.println("Test/got null value");
                return 0;
            }

            return c1Value + c2Value;
        } catch (ExecutionException ee) {
            System.out.println("Test/got unexpected execution exception: " + ee);
            return 0;
        }
    }

    ////////////////////////////
    // Other test snippets below
    ////////////////////////////
    //
    // Using an unsynchronised counter will basically never work, as the threads interleave
    private int testSynchronisedCounter() {
        Counter counter = new SynchronisedCounter();
        this.simpleSpawnTwoThreads(counter);
        return counter.getValue();
    }

    private int testLockFreeConcurrentCounter() {
        Counter counter = new LockFreeConcurrentCounter();
        this.simpleSpawnTwoThreads(counter);
        return counter.getValue();
    }

    private int testQueuedExecutor() {
        Counter counter = new UnsynchronisedCounter();
        MyExecutor executor = new QueuedExecutor();

        RunnableCounterIncrementor c1 = new RunnableCounterIncrementor(counter, this.threadCounterValue);
        RunnableCounterIncrementor c2 = new RunnableCounterIncrementor(counter, this.threadCounterValue);

        executor.execute(c1);
        executor.execute(c2);
        executor.shutdown();

        //executor.execute(c2); - successfully throws

        return counter.getValue();
    }

    private void simpleSpawnTwoThreads(Counter counter) {
        RunnableCounterIncrementor c1 = new RunnableCounterIncrementor(counter, this.threadCounterValue);
        RunnableCounterIncrementor c2 = new RunnableCounterIncrementor(counter, this.threadCounterValue);

        Thread t1 = new Thread(c1);
        Thread t2 = new Thread(c2);

        t1.start();
        t2.start();

        while (t1.getState() != Thread.State.TERMINATED || t2.getState() != Thread.State.TERMINATED) {
        }
    }
}