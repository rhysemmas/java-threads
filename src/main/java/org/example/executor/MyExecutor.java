package org.example.executor;

public interface MyExecutor {
    void execute(Runnable job);

    void shutdown();
}
