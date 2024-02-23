import org.example.queue.UnsynchronisedQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnsynchronisedQueueTest {
    @Test
    @DisplayName("Peek the queue")
    void testPeek() {
        UnsynchronisedQueue<String> queue = new UnsynchronisedQueue<>(1);
        String toCompare = "egg";
        queue.add(toCompare);

        assertEquals(toCompare, queue.peek());
    }

    @Test
    @DisplayName("Get next value from queue")
    void testNext() {
        UnsynchronisedQueue<String> queue = new UnsynchronisedQueue<>(1);
        String toCompare = "egg";
        queue.add(toCompare);

        assertEquals(toCompare, queue.next());
    }

    @Test
    @DisplayName("Add and read from the queue so that it compacts to reclaim space")
    void testQueueCompacts() {
        UnsynchronisedQueue<String> queue = new UnsynchronisedQueue<>(2);
        queue.add("wow1");
        queue.add("wow2");
        assertEquals("wow1", queue.next());

        queue.add("wow3");
        assertEquals("wow2", queue.next());
        assertEquals("wow3", queue.next());
    }
}
