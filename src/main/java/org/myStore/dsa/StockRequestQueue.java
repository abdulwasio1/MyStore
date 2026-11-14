
package org.myStore.dsa;

import org.myStore.entities.StockRequest;

public class StockRequestQueue {

    // Node of our linked list
    private static class Node {
        StockRequest data;
        Node next;

        Node(StockRequest data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node front;
    private Node rear;
    private int size;

    // Add request at the rear
    public void enqueue(StockRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }

        Node newNode = new Node(request);

        if (front == null) {
            front = newNode;
            rear = newNode;
        } else {
            rear.next = newNode;
            rear = newNode;
        }

        size++;
    }

    // Remove request from the front
    public StockRequest dequeue() {

        if (isEmpty()) {
            return null;
        }

        StockRequest removedRequest = front.data;

        front = front.next;

        if (front == null) {
            rear = null;
        }

        size--;

        return removedRequest;
    }

    // See the first request without removing it
    public StockRequest peek() {

        if (isEmpty()) {
            return null;
        }

        return front.data;
    }

    public boolean isEmpty() {
        return front == null;
    }

    public int getSize() {
        return size;
    }

    // Display requests in FIFO order
    public void display() {

        if (isEmpty()) {
            System.out.println("No pending requests.");
            return;
        }

        Node current = front;

        System.out.println("\n========== PENDING REQUESTS ==========");

        while (current != null) {

            StockRequest request = current.data;

            System.out.println("Request ID: " + request.getRequestId());
            System.out.println("Product: " + request.getProductName());
            System.out.println("Requested By: " + request.getUsername());
            System.out.println("Type: " + request.getRequestType());
            System.out.println("Quantity: " + request.getRequestedQuantity());
            System.out.println("--------------------------------------");

            current = current.next;
        }
    }
}
