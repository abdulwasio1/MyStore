
package org.myStore;

import org.myStore.dao.*;
import org.myStore.dsa.ProductHashTable;
import org.myStore.dsa.ProductPriorityHeap;
import org.myStore.dsa.ProductSearch;
import org.myStore.entities.Product;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws SQLException {

        ProductDAO dao = new ProductDAO();

        // Load products from MySQL
        ArrayList<Product> products = dao.getAllProducts();

        // Build SKU Hash Table
        ProductHashTable hashTable = buildHashTable(products);

        // Product name search object
        ProductSearch productSearch = new ProductSearch();

        int choice;

        do {
            System.out.println("\n===== INVENTORY MANAGEMENT SYSTEM =====");
            System.out.println("1. Add Product");
            System.out.println("2. Search Product by SKU");
            System.out.println("3. Search Product by Name");
            System.out.println("4. View Low Stock Priorities");
            System.out.println("0. Exit");

            choice = readInt("Enter your choice: ");

            switch (choice) {

                case 1:

                    Product newProduct = inputProduct();

                    try {

                        int generatedId = dao.addProduct(newProduct);

                        if (generatedId != -1) {

                            System.out.println("\nProduct added successfully!");
                            System.out.println("Generated Product ID: " + generatedId);

                            // Reload products from database
                            products = dao.getAllProducts();

                            // Rebuild hash table with updated products
                            hashTable = buildHashTable(products);

                        } else {
                            System.out.println("Product could not be added!");
                        }

                    } catch (SQLException e) {

                        if (e.getErrorCode() == 1062) {
                            System.out.println("Error: This SKU already exists!");
                        } else if (e.getErrorCode() == 3819) {
                            System.out.println("Error: Product values violate a database CHECK constraint!");
                        } else {
                            System.out.println("Database Error: " + e.getMessage());
                        }
                    }

                    break;

                case 2:

                    // Search by SKU
                    System.out.print("\nEnter Product SKU: ");
                    String sku = sc.nextLine().trim();

                    Product result = hashTable.search(sku);

                    if (result != null) {
                        System.out.println("\n===== PRODUCT FOUND =====");
                        displayProduct(result);
                    } else {
                        System.out.println("Product not found!");
                    }

                    break;

                case 3:

                    // Search by Name
                    System.out.print("\nEnter Product Name: ");
                    String name = sc.nextLine();

                    ArrayList<Product> results =
                            productSearch.searchByName(products, name);

                    if (results.isEmpty()) {
                        System.out.println("No matching products found!");
                    } else {

                        System.out.println("\n===== SEARCH RESULTS =====");

                        for (Product p : results) {
                            displayProduct(p);
                        }
                    }

                    break;

                case 4:

                    // Create Max Heap for low-stock products
                    ProductPriorityHeap priorityHeap =
                            new ProductPriorityHeap(products.size());

                    for (Product p : products) {
                        priorityHeap.insert(p);
                    }

                    if (priorityHeap.isEmpty()) {

                        System.out.println("\nNo products require restocking!");

                    } else {

                        System.out.println("\n===== LOW STOCK PRIORITIES =====");

                        while (!priorityHeap.isEmpty()) {

                            Product p = priorityHeap.extractMax();

                            System.out.println("\nStock Shortage: " +
                                    (p.getReorderLevel() - p.getQuantity()));

                            displayProduct(p);
                        }
                    }

                    break;

                case 0:

                    System.out.println("\nExiting Inventory System...");
                    break;

                default:

                    System.out.println("\nInvalid choice! Try again.");
            }

        } while (choice != 0);

        sc.close();
    }

    // Take product details from user
    public static Product inputProduct() throws SQLException {

        System.out.println("\n===== ADD NEW PRODUCT =====");

        Product p = new Product();

        // Product name
        String name;

        do {
            System.out.print("Enter Product Name: ");
            name = sc.nextLine().trim();

            if (name.isEmpty()) {
                System.out.println("Product name cannot be empty!");
            }

        } while (name.isEmpty());

        p.setProductName(name);

        // SKU
        String sku;

        while (true) {

            System.out.print("Enter Product SKU: ");
            sku = sc.nextLine().trim();

            if (sku.isEmpty()) {
                System.out.println("SKU cannot be empty!");
                continue;
            }

            if (ProductDAO.skuExists(sku)) {
                System.out.println("This SKU already exists! Enter another SKU.");
                continue;
            }

            break;
        }

        p.setSku(sku);

        // Category (optional)
        System.out.print("Enter Category: ");
        String category = sc.nextLine().trim();

        if (category.isEmpty()) {
            p.setCategory(null);
        } else {
            p.setCategory(category);
        }

        // Price
        BigDecimal price;

        while (true) {

            System.out.print("Enter Selling Price: ");
            String priceInput = sc.nextLine().trim();

            try {

                price = new BigDecimal(priceInput);

                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("Price cannot be negative!");
                } else {
                    break;
                }

            } catch (NumberFormatException e) {
                System.out.println("Enter a valid price!");
            }
        }

        p.setPrice(price);

        // Quantity
        int quantity;

        do {
            quantity = readInt("Enter Quantity: ");

            if (quantity < 0) {
                System.out.println("Quantity cannot be negative!");
            }

        } while (quantity < 0);

        p.setQuantity(quantity);

        // Reorder level
        int reorderLevel;

        do {
            reorderLevel = readInt("Enter Reorder Level: ");

            if (reorderLevel < 0) {
                System.out.println("Reorder level cannot be negative!");
            }

        } while (reorderLevel < 0);

        p.setReorderLevel(reorderLevel);

        return p;
    }

    // Read integer safely
    public static int readInt(String message) {

        while (true) {

            System.out.print(message);
            String input = sc.nextLine().trim();

            try {
                return Integer.parseInt(input);

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number!");
            }
        }
    }

    // Build SKU Hash Table
    public static ProductHashTable buildHashTable(ArrayList<Product> products) {

        ProductHashTable hashTable = new ProductHashTable(10);

        for (Product p : products) {
            hashTable.insert(p);
        }

        return hashTable;
    }

    // Display product details
    public static void displayProduct(Product p) {

        System.out.println("-----------------------------");
        System.out.println("Product ID: " + p.getProductId());
        System.out.println("SKU: " + p.getSku());
        System.out.println("Name: " + p.getProductName());
        System.out.println("Category: " + p.getCategory());
        System.out.println("Price: " + p.getPrice());
        System.out.println("Quantity: " + p.getQuantity());
        System.out.println("Reorder Level: " + p.getReorderLevel());
        System.out.println("-----------------------------");
    }
}
