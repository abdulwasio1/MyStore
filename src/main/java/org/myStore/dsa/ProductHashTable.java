
package org.myStore.dsa;

import org.myStore.entities.Product;

public class ProductHashTable {

    // Node for separate chaining
    private class Node {
        Product data;
        Node next;

        Node(Product data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node[] table;
    private int capacity;
    private int size;

    // Constructor
    public ProductHashTable(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        table = new Node[capacity];
    }

    // Calculate hash index
    private int hashFunction(String sku) {
        return Math.floorMod(sku.toUpperCase().hashCode(), capacity);
    }

    // Insert product
    public boolean insert(Product product) {

        if (product == null || product.getSku() == null ||
                product.getSku().trim().isEmpty()) {
            return false;
        }

        String sku = product.getSku().trim().toUpperCase();

        int index = hashFunction(sku);

        Node current = table[index];

        // Check duplicate SKU
        while (current != null) {

            if (current.data.getSku().equalsIgnoreCase(sku)) {
                return false;
            }

            current = current.next;
        }

        // Insert at beginning of linked list
        Node newNode = new Node(product);
        newNode.next = table[index];
        table[index] = newNode;

        size++;

        return true;
    }

    // Search product by SKU
    public Product search(String sku) {

        if (sku == null || sku.trim().isEmpty()) {
            return null;
        }

        String key = sku.trim().toUpperCase();

        int index = hashFunction(key);

        Node current = table[index];

        while (current != null) {

            if (current.data.getSku().equalsIgnoreCase(key)) {
                return current.data;
            }

            current = current.next;
        }

        return null;
    }

    // Get total products stored
    public int getSize() {
        return size;
    }

    // Check if table is empty
    public boolean isEmpty() {
        return size == 0;
    }
}
