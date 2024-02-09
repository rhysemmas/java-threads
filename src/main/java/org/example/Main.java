package org.example;

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
        while (true) {
            System.out.println("Starting test with id: " + counter);
            // Assumes spawning 2 threads
            ThreadSpawner spawner = new ThreadSpawner(this.desiredCounterValue / 2);
            int value = spawner.run();
            if (value != this.desiredCounterValue) {
                System.out.println("Unexpected value: " + value);
                break;
            } else {
                System.out.println("Got expected value of: " + value);
            }
            counter++;
        }
    }
}

class ThreadSpawner {
    private final int threadCounterValue;

    ThreadSpawner(int threadCounterValue) {
        this.threadCounterValue = threadCounterValue;
    }
    public int run() {
        return this.testQueuedExecutor();
        //return this.testSynchronisedCounter();
    }

    private int testQueuedExecutor() {
        Counter counter = new UnsynchronisedCounter();
        Executor executor = new QueuedExecutor();

        RunnableCounterIncrementor c1 = new RunnableCounterIncrementor(counter, this.threadCounterValue);
        RunnableCounterIncrementor c2 = new RunnableCounterIncrementor(counter, this.threadCounterValue);

        executor.execute(c1);
        executor.execute(c2);
        executor.shutdown();

        //executor.execute(c2); - successfully throws

        return counter.getValue();
    }

    // Using an unsynchronised counter will basically never work, as the threads interleave
    private int testSynchronisedCounter() {
        Counter counter = new SynchronisedCounter();

        RunnableCounterIncrementor c1 = new RunnableCounterIncrementor(counter, this.threadCounterValue);
        RunnableCounterIncrementor c2 = new RunnableCounterIncrementor(counter, this.threadCounterValue);

        Thread t1 = new Thread(c1);
        Thread t2 = new Thread(c2);

        t1.start();
        t2.start();

        while (t1.getState() != Thread.State.TERMINATED || t2.getState() != Thread.State.TERMINATED) {
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException e) {
//                System.out.println("got interrupted exception: " + e);
//            }
        }

        return counter.getValue();
    }
}