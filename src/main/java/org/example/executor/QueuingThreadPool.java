package org.example.executor;

import org.example.queue.MyQueue;
import org.example.queue.UnsynchronisedQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class QueuingThreadPool<V> implements ThreadPoolExecutor<V> {
    Channel<Job<V>> jobNotificationChannel;

    public QueuingThreadPool() {
        jobNotificationChannel = new Channel<>();
        Coordinator<V> coordinator = new Coordinator<>(jobNotificationChannel);
        Thread coordinatorThread = new Thread(coordinator);
        coordinatorThread.start();
    }

    public MyFuture<V> submit(Callable<V> submittedJob) {
        System.out.println("QueuingThreadPool/got job!");
        Channel<Status<V>> resultChannel = new Channel<>();
        Channel<ExecutionException> errorChannel = new Channel<>();
        Channel<Boolean> cancelChannel = new Channel<>();

        Job<V> job = new Job<>(submittedJob, resultChannel, errorChannel, cancelChannel);
        MyFuture<V> futureResult = new Result<>(job);

        System.out.println("QueuingThreadPool/sending notification of new job!");
        jobNotificationChannel.send(job);

        return futureResult;
    }

    record Job<V>(Callable<V> jobToRun,
                  Channel<Status<V>> resultChannel,
                  Channel<ExecutionException> errorChannel,
                  Channel<Boolean> cancelChannel) {
    }

    record Status<V>(V value, boolean success) {
    }

    static class Result<V> implements MyFuture<V> {
        private final Channel<Status<V>> resultChannel;
        private final Channel<ExecutionException> errorChannel;
        private final Channel<Boolean> cancelChannel;
        private boolean isCancelled;

        Result(Job<V> job) {
            this.resultChannel = job.resultChannel;
            this.errorChannel = job.errorChannel;
            this.cancelChannel = job.cancelChannel;
        }

        public V get() throws ExecutionException {
            if (isCancelled) {
                System.out.println("Result/is cancelled, returning null");
                return null;
            }

            Status<V> status = resultChannel.receive();
            // TODO: clean this up
            if (!status.success) {
                if (errorChannel.hasMessage()) {
                    throw new ExecutionException(errorChannel.receive());
                } else {
                    // TODO: what to do if unsuccessful without error?
                    System.out.println("Result/returning unexpected null");
                    return null;
                }
            }
            System.out.println("Result/returning value of '" + status.value + "' to caller");
            return status.value;
        }

        public boolean cancel() {
            // TODO: need to return false if we get some kind of exception while sending on the channel
            cancelChannel.send(true);
            isCancelled = true;
            return true;
        }
    }
}

class Channel<V> {
    private final BlockingQueue<V> data;

    Channel() {
        data = new ArrayBlockingQueue<>(1);
    }

    public void send(V value) {
        try {
            data.put(value);
        } catch (InterruptedException ie) {
            // TODO: handle exception
            System.out.println("channel got interrupted exception while putting: " + ie);
        }
    }

    public V receive() {
        V received = null;
        try {
            received = data.take();
        } catch (InterruptedException ie) {
            // TODO: handle exception
            System.out.println("channel got interrupted exception while receiving: " + ie);
        }
        return received;
    }

    public boolean hasMessage() {
        return !data.isEmpty();
    }
}

class Coordinator<V> implements Runnable {
    private final Channel<QueuingThreadPool.Job<V>> jobNotificationChannel;
    private final MyQueue<QueuingThreadPool.Job<V>> jobQueue;
    private final Map<Worker<V>, Work<V>> workers;
    private final MyQueue<Worker<V>> freeWorkers;
    private final Channel<Worker<V>> workersDoneChannel;

    Coordinator(Channel<QueuingThreadPool.Job<V>> jobNotificationChannel) {
        this.jobNotificationChannel = jobNotificationChannel;
        jobQueue = new UnsynchronisedQueue<>(10);

        workers = new HashMap<>();
        int numberOfWorkers = 2;
        freeWorkers = new UnsynchronisedQueue<>(numberOfWorkers);

        // There is one shared done channel which workers use to signal to the coordinator they are done
        workersDoneChannel = new Channel<>();

        for (int i = 0; i < numberOfWorkers; i++) {
            // Each worker has their own job channel, which the coordinator will submit jobs to
            Channel<QueuingThreadPool.Job<V>> newJobChannel = new Channel<>();
            Work<V> work = new Work<>(newJobChannel, workersDoneChannel);
            Worker<V> worker = new Worker<>(i + 1, work);

            try {
                workers.put(worker, work);
            } catch (IllegalArgumentException iae) {
                System.out.println("got exception adding worker to workers map: " + iae);
            }

            try {
                freeWorkers.add(worker);
            } catch (IllegalStateException ise) {
                System.out.println("got exception adding worker to free workers queue: " + ise);
            }
        }
    }

    // TODO: the idea was to give each worker its own job and done channel, I have opted to go for a shared done
    // channel to make handling concurrent done's a bit easier, but in the future it would be nice to try and have a
    // thread that watches each of the workers done channels
    record Work<V>(Channel<QueuingThreadPool.Job<V>> newJobChannel, Channel<Worker<V>> doneChannel) {
    }

    public void run() {
        for (Worker<V> worker : workers.keySet()) {
            Thread thread = new Thread(worker);
            thread.start();
            System.out.println("Started worker with id: " + worker.getId());
        }

        while (true) {
            try {
                checkForIdleWorkers();
            } catch (IllegalStateException ise) {
                System.out.println("got exception while checking for idle workers: " + ise);
            }

            try {
                receiveNewJob();
            } catch (IllegalStateException ise) {
                System.out.println("got exception while receiving new job: " + ise);
            }

            try {
                scheduleNewJob();
            } catch (IllegalStateException ise) {
                System.out.println("got exception while scheduling worker: " + ise);
            }
        }
    }

    // TODO: expand the coordinator to start another watchdog style thread that is responsible for keeping an eye on
    // when workers have completed and updating the freeWorkers queue - might need to synchronise or worry about other
    // methods of thread safety (could make more channels haha!)
    private void checkForIdleWorkers() throws IllegalStateException {
        if (workersDoneChannel.hasMessage()) {
            Worker<V> idleWorker = workersDoneChannel.receive();
            try {
                freeWorkers.add(idleWorker);
            } catch (IllegalStateException ise) {
                throw new IllegalStateException("got exception while adding to free workers queue: " + ise);
            }
        }
    }

    private void receiveNewJob() throws IllegalStateException {
        if (jobNotificationChannel.hasMessage()) {
            QueuingThreadPool.Job<V> submittedJob = jobNotificationChannel.receive();
            try {
                jobQueue.add(submittedJob);
            } catch (IllegalStateException ise) {
                throw new IllegalStateException("got exception while adding to job queue: " + ise);
            }
        }
    }

    private void scheduleNewJob() throws IllegalStateException {
        if (jobQueue.peek() == null || freeWorkers.peek() == null) {
            return;
        }

        QueuingThreadPool.Job<V> job;
        try {
            job = jobQueue.next();
        } catch (IllegalStateException ise) {
            throw new IllegalStateException("got exception while taking next from job queue: " + ise);
        }

        if (jobIsCancelled(job)) {
            System.out.println("job has been cancelled, will not schedule");
            return;
        }

        Worker<V> worker;
        try {
            worker = freeWorkers.next();
            Work<V> work = workers.get(worker);
            work.newJobChannel.send(job);
            System.out.println("Coordinator/scheduling job on worker id: " + worker.getId());
        } catch (IllegalStateException ise) {
            System.out.println("got exception while taking next from worker queue: " + ise);
        }
    }

    private boolean jobIsCancelled(QueuingThreadPool.Job<V> job) {
        if (job.cancelChannel().hasMessage()) {
            return job.cancelChannel().receive();
        }
        return false;
    }
}

class Worker<V> implements Runnable {
    private final int id;
    private final Channel<QueuingThreadPool.Job<V>> newJobChannel;
    private final Channel<Worker<V>> doneChannel;

    // TODO: have some kind of id to identify workers?

    Worker(int id, Coordinator.Work<V> work) {
        this.id = id;
        this.newJobChannel = work.newJobChannel();
        this.doneChannel = work.doneChannel();
    }

    public int getId() {
        return id;
    }

    public void run() {
        // TODO: investigate using a switch statement here instead
        while (true) {
            // Will block until a new job becomes available (could signal shutdown with a dummy job)
            QueuingThreadPool.Job<V> currentJob = newJobChannel.receive();

            System.out.println("Worker/starting job on id: " + id);

            V result;
            try {
                result = currentJob.jobToRun().call();
                QueuingThreadPool.Status<V> status = new QueuingThreadPool.Status<>(result, true);
                System.out.println("Worker/sending result from id: " + id);
                currentJob.resultChannel().send(status);
            } catch (Exception e) {
                ExecutionException executionException = new ExecutionException(e);
                System.out.println("Worker/sending error from id: " + id);
                currentJob.errorChannel().send(executionException);
                QueuingThreadPool.Status<V> status = new QueuingThreadPool.Status<>(null, false);
            }

            System.out.println("Worker/sending done notification from id: " + id);
            doneChannel.send(this);
        }
    }
}