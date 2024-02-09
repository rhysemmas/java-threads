package org.example;

public class LockFreeConcurrentCounter implements Counter {
    private final Stack stack;

    LockFreeConcurrentCounter() {
        this.stack = new Stack();
    }

    public void increment(int i) {
        this.stack.push(i);
    }

    public void decrement(int i) {
        this.stack.push(-i);
    }

    public int getValue() {
        int value = 0;
        while (!this.stack.isEmpty()) {
            value += stack.pop();
        }
        return value;
    }
}

class Stack {
    private final SingleLinkedList list;

    Stack() {
        this.list = new SingleLinkedList();
    }

    public void push(int value) {
        Node newTop = new Node(value);
        while (true) {
            Node currentTop = this.list.top;
            newTop.setNext(currentTop);
            if (compareAndSwap(currentTop, newTop)) {
                return;
            }
        }
    }

    public int pop() {
        Node currentTop = this.list.top;
        this.list.top = currentTop.getNext();
        return currentTop.getValue();
    }

    public boolean isEmpty() {
        return this.list.top == null;
    }

    // Can we make this operation atomic without the use of synchronized or pre-built atomic data types?
    // How do we call the processors CAS instruction directly?
    private synchronized boolean compareAndSwap(Node currentTop, Node newTop) {
        if (this.list.top == currentTop) {
            this.list.top = newTop;
            return true;
        } else {
            return false;
        }
    }
}

class SingleLinkedList {
    public Node top;
}

class Node {
    private Node next;
    private final int value;

    Node(int value) {
        this.value = value;
    }

    public void setNext(Node n) {
        this.next = n;
    }

    public Node getNext() {
        return this.next;
    }

    public int getValue() {
        return this.value;
    }
}
