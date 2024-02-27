package org.example.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public interface ThreadPoolExecutor<V> {
    MyFuture<V> submit(Callable<V> job); // optional extension: priority for job

    interface MyFuture<V> {
        V get() throws ExecutionException;

        boolean cancel();
    }
}