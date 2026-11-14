
package org.myStore.dsa;

import org.myStore.entities.Product;

public class ProductPriorityHeap {

    private Product[] heap;
    private int size;
    private int capacity;

    public ProductPriorityHeap() {
        this.capacity = Math.max(1, capacity);
        heap = new Product[this.capacity];
        size = 0;
    }

    // Calculate stock shortage
    private int getShortage(Product p) {
        return p.getReorderLevel() - p.getQuantity();
    }

    // Check which product has higher priority
    private boolean hasHigherPriority(Product p1, Product p2) {

        int shortage1 = getShortage(p1);
        int shortage2 = getShortage(p2);

        if (shortage1 != shortage2) {
            return shortage1 > shortage2;
        }

        // If shortage is equal, lower quantity gets priority
        return p1.getQuantity() < p2.getQuantity();
    }

    // Insert product into heap
    public void insert(Product product) {

        if (product == null) {
            return;
        }

        // Only insert products requiring restocking
        if (product.getQuantity() > product.getReorderLevel()) {
            return;
        }

        if (size == capacity) {
            resize();
        }

        heap[size] = product;

        int current = size;
        size++;

        // Heapify Up
        while (current > 0) {

            int parent = (current - 1) / 2;

            if (hasHigherPriority(heap[current], heap[parent])) {

                Product temp = heap[current];
                heap[current] = heap[parent];
                heap[parent] = temp;

                current = parent;

            } else {
                break;
            }
        }
    }

    // Get highest priority product without removing it
    public Product peek() {

        if (size == 0) {
            return null;
        }

        return heap[0];
    }

    // Remove and return highest priority product
    public Product extractMax() {

        if (size == 0) {
            return null;
        }

        Product max = heap[0];

        heap[0] = heap[size - 1];
        heap[size - 1] = null;
        size--;

        // Heapify Down
        int current = 0;

        while (true) {

            int left = 2 * current + 1;
            int right = 2 * current + 2;

            int highest = current;

            if (left < size &&
                    hasHigherPriority(heap[left], heap[highest])) {
                highest = left;
            }

            if (right < size &&
                    hasHigherPriority(heap[right], heap[highest])) {
                highest = right;
            }

            if (highest == current) {
                break;
            }

            Product temp = heap[current];
            heap[current] = heap[highest];
            heap[highest] = temp;

            current = highest;
        }

        return max;
    }

    // Increase array capacity when full
    private void resize() {

        capacity = capacity * 2;

        Product[] newHeap = new Product[capacity];

        for (int i = 0; i < size; i++) {
            newHeap[i] = heap[i];
        }

        heap = newHeap;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }
}
