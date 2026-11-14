
import org.myStore.dao.ProductDAO;
import org.myStore.dsa.ProductHashTable;
import org.myStore.dsa.ProductPriorityHeap;
import org.myStore.dsa.ProductSearch;
import org.myStore.entities.Product;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        ProductDAO dao = new ProductDAO();

        // Load products from MySQL
        ArrayList<Product> products = dao.getAllProducts();

        // Build SKU Hash Table
        ProductHashTable hashTable = new ProductHashTable(10);

        for (Product p : products) {
            hashTable.insert(p);
        }

        // Product name search object
        ProductSearch productSearch = new ProductSearch();

        int choice;

        do {
            System.out.println("\n===== INVENTORY MANAGEMENT SYSTEM =====");
            System.out.println("1. Search Product by SKU");
            System.out.println("2. Search Product by Name");
            System.out.println("3. View Low Stock Priorities");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            choice = sc.nextInt();
            sc.nextLine(); // Consume newline

            switch (choice) {

                case 1:
                    // Search by SKU
                    System.out.print("\nEnter Product SKU: ");
                    String sku = sc.nextLine();

                    Product result = hashTable.search(sku);

                    if (result != null) {
                        System.out.println("\n===== PRODUCT FOUND =====");
                        displayProduct(result);
                    } else {
                        System.out.println("Product not found!");
                    }
                    break;

                case 2:
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
                case 3:

                    ProductPriorityHeap priorityHeap =
                            new ProductPriorityHeap(products.size());

                    // Insert low-stock products
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
