package org.example.executor;

public interface Executor {
    void execute(Runnable job);

    void shutdown();
}
