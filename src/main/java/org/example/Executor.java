package org.example;

public interface Executor {
    void execute(Runnable job);

    void shutdown();
}
