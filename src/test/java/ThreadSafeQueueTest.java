import org.example.queue.ThreadSafeQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ThreadSafeQueueTest {
    static class RunnableQueueAdder<T> implements Runnable {
        private final ThreadSafeQueue<T> queue;

        private final T value;

        RunnableQueueAdder(ThreadSafeQueue<T> queue, T value) {
            this.queue = queue;
            this.value = value;
        }

        public void run() {
            System.out.println("thread: " + Thread.currentThread().threadId() + " adding value: " + this.value);
            this.queue.add(this.value);
            System.out.println("thread: " + Thread.currentThread().threadId() + " sees: " + this.queue.peek());
        }
    }

    @Test
    @DisplayName("Peek the queue")
    void testAdd() {
        ThreadSafeQueue<String> queue = new ThreadSafeQueue<>(10);

        String s1 = "s1";
        Runnable r1 = new RunnableQueueAdder<>(queue, s1);
        Thread t1 = new Thread(r1);

        String s2 = "s2";
        Runnable r2 = new RunnableQueueAdder<>(queue, s2);
        Thread t2 = new Thread(r2);

        String s3 = "s3";
        Runnable r3 = new RunnableQueueAdder<>(queue, s3);
        Thread t3 = new Thread(r3);

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException ie) {
            System.out.println("got interrupted exception: " + ie);
        }
    }
}
