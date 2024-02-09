package org.example;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class QueuedExecutor implements Executor {
    private final Scheduler scheduler;
    private final Thread schedulerThread;

    QueuedExecutor() {
        this.scheduler = new Scheduler();
        this.schedulerThread = new Thread(scheduler);
        this.schedulerThread.start();
    }

    public void execute(Runnable job) throws IllegalStateException {
        try {
            this.scheduler.schedule(job);
        } catch (IllegalStateException ise) {
            throw new IllegalStateException("Error trying to schedule job: " + ise);
        }
    }

    public void shutdown() {
        while (this.schedulerThread.isAlive()) {
            try {
                this.schedulerThread.interrupt();
                this.schedulerThread.join();
            } catch (InterruptedException ie) {
                System.out.println("Executor unexpectedly interrupted while waiting for the scheduler thread to exit");
            }
        }
    }
}

class Scheduler implements Runnable {
    private final Queue<Runnable> jobQueue;
    private boolean shutdown;

    Scheduler() {
        this.jobQueue = new ArrayBlockingQueue<>(10);
    }

    public void run() {
        while (true) {
            if (Thread.interrupted()) {
                System.out.println("Scheduler thread has been interrupted, finishing remaining tasks");
                this.shutdown = true;
            }

            if (this.shutdown && this.jobQueue.isEmpty()) {
                return;
            }

            if (this.jobQueue.peek() != null) {
                    Runnable job = this.jobQueue.poll();
                    if (job != null) {
                        job.run();
                    }
                    // TODO: trying to run jobs on their own threads, rather than the scheduler's thread
//                    Thread jobThread = new Thread(this.jobQueue.poll());
//                    jobThread.start();
//                    try {
//                        jobThread.join();
//                    } catch (InterruptedException ie) {
//                        System.out.println("Job thread has been interrupted: " + ie);
//                    }
                }
            }
        }

    public void schedule(Runnable job) throws IllegalStateException {
        if (this.shutdown) {
            throw new IllegalStateException("Scheduler is shutting down, can't schedule new task");
        }

        try {
            this.jobQueue.add(job);
        } catch (IllegalStateException ise) {
            throw new IllegalStateException("Job queue is full, try again later: " + ise);
        }
    }
}