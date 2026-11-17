
package org.myStore.dsa;

import org.myStore.entities.Product;

public class ProductHashTable {

    // Node for separate chaining
    private class Node {
        Product data;

        // Separate links for both indexes
        Node skuNext;
        Node idNext;

        Node(Product data) {
            this.data = data;
            this.skuNext = null;
            this.idNext = null;
        }
    }

    // Two indexes inside the same hash table class
    private Node[] skuTable;
    private Node[] idTable;

    private int capacity;
    private int size;

    // Constructor
    public ProductHashTable(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be greater than zero."
            );
        }

        this.capacity = capacity;
        this.size = 0;

        skuTable = new Node[capacity];
        idTable = new Node[capacity];
    }

    // ================= SKU HASH FUNCTION =================

    private int hashSKU(String sku) {
        return Math.floorMod(
                sku.toUpperCase().hashCode(), capacity
        );
    }

    // ================= ID HASH FUNCTION =================

    private int hashID(int productId) {
        return Math.floorMod(
                Integer.hashCode(productId), capacity
        );
    }

    // ================= INSERT PRODUCT =================

    public boolean insert(Product product) {

        if (product == null ||
                product.getSku() == null ||
                product.getSku().trim().isEmpty() ||
                product.getProductId() <= 0) {

            return false;
        }

        String sku = product.getSku().trim().toUpperCase();
        int productId = product.getProductId();

        // Check duplicate SKU and Product ID
        if (searchBySKU(sku) != null ||
                searchById(productId) != null) {

            return false;
        }

        int skuIndex = hashSKU(sku);
        int idIndex = hashID(productId);

        // Create one node for both indexes
        Node newNode = new Node(product);

        // Insert into SKU index
        newNode.skuNext = skuTable[skuIndex];
        skuTable[skuIndex] = newNode;

        // Insert into ID index
        newNode.idNext = idTable[idIndex];
        idTable[idIndex] = newNode;

        size++;

        return true;
    }

    // ================= SEARCH BY SKU =================

    public Product searchBySKU(String sku) {

        if (sku == null || sku.trim().isEmpty()) {
            return null;
        }

        String key = sku.trim().toUpperCase();

        int index = hashSKU(key);

        Node current = skuTable[index];

        while (current != null) {

            if (current.data.getSku().equalsIgnoreCase(key)) {
                return current.data;
            }

            current = current.skuNext;
        }

        return null;
    }

    // ================= SEARCH BY PRODUCT ID =================

    public Product searchById(int productId) {

        if (productId <= 0) {
            return null;
        }

        int index = hashID(productId);

        Node current = idTable[index];

        while (current != null) {

            if (current.data.getProductId() == productId) {
                return current.data;
            }

            current = current.idNext;
        }

        return null;
    }

    // Backward compatibility with existing search(String sku) calls
    public Product search(String sku) {
        return searchBySKU(sku);
    }

    // ================= GET SIZE =================

    public int getSize() {
        return size;
    }

    // ================= CHECK EMPTY =================

    public boolean isEmpty() {
        return size == 0;
    }
}
