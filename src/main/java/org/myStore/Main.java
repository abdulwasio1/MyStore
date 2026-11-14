
package org.myStore;

import org.myStore.auth.AuthService;
import org.myStore.auth.Role;
import org.myStore.auth.Session;
import org.myStore.dao.ProductDAO;
import org.myStore.entities.Product;
import org.myStore.entities.User;
import org.myStore.dsa.ProductHashTable;
import org.myStore.dsa.ProductPriorityHeap;
import org.myStore.dsa.ProductSearch;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        ProductDAO dao = new ProductDAO();
        AuthService authService = new AuthService();

        // Load products from database
        ArrayList<Product> products = dao.getAllProducts();

        // Build Hash Table using existing products
        ProductHashTable hashTable = buildHashTable(products);

        boolean running = true;

        while (running) {

            // ================= LOGIN =================

            User user = loginUser(sc, authService);

            if (user == null) {
                continue;
            }

            Session.login(user);

            System.out.println("\nLogin successful!");
            System.out.println("Welcome, " + user.getUsername());
            System.out.println("Role: " + user.getRole());

            boolean logout = false;

            // ================= USER MENU =================

            while (!logout) {

                System.out.println("\n========== MYSTORE ==========");

                if (Session.hasRole(Role.ADMIN)) {
                    System.out.println("Logged in as: ADMIN");
                } else {
                    System.out.println("Logged in as: STAFF");
                }

                System.out.println("\n1. Search Product by SKU");
                System.out.println("2. Search Product by Name");
                System.out.println("3. View Low Stock Priorities");

                // ADMIN-only options
                if (Session.hasRole(Role.ADMIN)) {
                    System.out.println("4. Add New Product");
                    System.out.println("5. Create Staff Account");
                }

                System.out.println("0. Logout");

                System.out.print("\nEnter your choice: ");

                int choice;

                try {
                    choice = Integer.parseInt(sc.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number!");
                    continue;
                }

                switch (choice) {

                    // ================= SEARCH BY SKU =================

                    case 1:

                        System.out.print("\nEnter Product SKU: ");
                        String sku = sc.nextLine().trim();

                        Product foundProduct = hashTable.search(sku);

                        if (foundProduct != null) {
                            System.out.println("\nProduct Found!");
                            displayProduct(foundProduct);
                        } else {
                            System.out.println("\nProduct not found!");
                        }

                        break;

                    // ================= SEARCH BY NAME =================

                    case 2:

                        System.out.print("\nEnter Product Name: ");
                        String keyword = sc.nextLine().trim();

                        if (keyword.isEmpty()) {
                            System.out.println("Search keyword cannot be empty!");
                            break;
                        }

                        ArrayList<Product> results =
                                ProductSearch.searchByName(products, keyword);

                        if (results.isEmpty()) {
                            System.out.println("\nNo matching products found!");
                        } else {

                            System.out.println("\n===== SEARCH RESULTS =====");

                            for (Product p : results) {
                                displayProduct(p);
                            }
                        }

                        break;

                    // ================= LOW STOCK PRIORITIES =================

                    case 3:

                        ProductPriorityHeap heap =
                                new ProductPriorityHeap(
                                        Math.max(10, products.size())
                                );

                        // Insert only low-stock products
                        for (Product p : products) {

                            if (p.getQuantity() <= p.getReorderLevel()) {
                                heap.insert(p);
                            }
                        }

                        if (heap.isEmpty()) {
                            System.out.println("\nNo low-stock products!");
                        } else {

                            System.out.println(
                                    "\n===== LOW STOCK PRIORITIES ====="
                            );

                            while (!heap.isEmpty()) {

                                Product p = heap.extractMax();

                                displayProduct(p);

                                System.out.println(
                                        "Shortage: "
                                                + (p.getReorderLevel()
                                                - p.getQuantity())
                                );

                                System.out.println("--------------------------");
                            }
                        }

                        break;

                    // ================= ADD PRODUCT =================

                    case 4:

                        try {

                            // Only ADMIN can add products
                            Session.requireAdmin();

                            addProduct(sc, dao);

                            // Refresh products from database
                            products = dao.getAllProducts();

                            // Rebuild Hash Table
                            hashTable = buildHashTable(products);

                        } catch (SecurityException e) {

                            System.out.println(e.getMessage());

                        } catch (SQLException e) {

                            if (e.getErrorCode() == 1062) {
                                System.out.println(
                                        "\nError: This SKU already exists!"
                                );
                            } else {
                                System.out.println(
                                        "\nDatabase Error: " + e.getMessage()
                                );
                            }
                        }

                        break;

                    // ================= CREATE STAFF ACCOUNT =================

                    case 5:

                        try {

                            // Only ADMIN can create staff accounts
                            Session.requireAdmin();

                            System.out.println(
                                    "\n===== CREATE STAFF ACCOUNT ====="
                            );

                            System.out.print("Enter Username: ");
                            String newUsername = sc.nextLine().trim();

                            System.out.print("Enter Password: ");
                            String newPassword = sc.nextLine();

                            authService.createStaffAccount(
                                    newUsername,
                                    newPassword
                            );

                            System.out.println(
                                    "\nStaff account created successfully!"
                            );

                        } catch (SecurityException |
                                 IllegalArgumentException e) {

                            System.out.println(e.getMessage());

                        } catch (SQLException e) {

                            if (e.getErrorCode() == 1062) {
                                System.out.println(
                                        "Username already exists!"
                                );
                            } else {
                                System.out.println(
                                        "Database Error: " + e.getMessage()
                                );
                            }
                        }

                        break;

                    // ================= LOGOUT =================

                    case 0:

                        Session.logout();

                        System.out.println(
                                "\nLogged out successfully!"
                        );

                        logout = true;

                        break;

                    default:

                        System.out.println("Invalid choice!");

                }
            }
        }

        sc.close();
    }


    // ================= LOGIN METHOD =================

    public static User loginUser(
            Scanner sc,
            AuthService authService) {

        while (true) {

            System.out.println("\n========== MYSTORE LOGIN ==========");

            System.out.print("Username: ");
            String username = sc.nextLine().trim();

            System.out.print("Password: ");
            String password = sc.nextLine();

            try {

                User user = authService.login(username, password);

                if (user != null) {
                    return user;
                }

                System.out.println(
                        "\nInvalid username or password!"
                );

            } catch (SQLException e) {

                System.out.println(
                        "\nDatabase Error: " + e.getMessage()
                );

                return null;
            }
        }
    }


    // ================= BUILD HASH TABLE =================

    public static ProductHashTable buildHashTable(
            ArrayList<Product> products) {

        ProductHashTable hashTable = new ProductHashTable(
                Math.max(10, products.size() * 2)
        );

        for (Product p : products) {
            hashTable.insert(p);
        }

        return hashTable;
    }


    // ================= DISPLAY PRODUCT =================

    public static void displayProduct(Product p) {

        System.out.println("\n--------------------------");
        System.out.println("Product ID: " + p.getProductId());
        System.out.println("SKU: " + p.getSku());
        System.out.println("Name: " + p.getProductName());
        System.out.println("Category: " + p.getCategory());
        System.out.println("Price: " + p.getPrice());
        System.out.println("Quantity: " + p.getQuantity());
        System.out.println("Reorder Level: " + p.getReorderLevel());
        System.out.println("--------------------------");
    }

    public static void registerStaff(
            Scanner sc,
            AuthService authService) {

        System.out.println("\n===== STAFF REGISTRATION =====");

        System.out.print("Enter Username: ");
        String username = sc.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        System.out.print("Confirm Password: ");
        String confirmPassword = sc.nextLine();

        if (!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match!");
            return;
        }

        try {

            authService.createStaffAccount(username, password);

            System.out.println("\nRegistration successful!");
            System.out.println("Your account role is STAFF.");
            System.out.println("You can now log in.");

        } catch (IllegalArgumentException e) {

            System.out.println(e.getMessage());

        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                System.out.println("Username already exists!");
            } else {
                System.out.println(
                        "Database Error: " + e.getMessage()
                );
            }
        }
    }


    // ================= ADD PRODUCT =================

    public static void addProduct(
            Scanner sc,
            ProductDAO dao) throws SQLException {

        System.out.println("\n===== ADD NEW PRODUCT =====");

        // Product Name
        String name;

        while (true) {

            System.out.print("Enter Product Name: ");
            name = sc.nextLine().trim();

            if (!name.isEmpty()) {
                break;
            }

            System.out.println("Product name cannot be empty!");
        }


        // SKU
        String sku;

        while (true) {

            System.out.print("Enter Product SKU: ");
            sku = sc.nextLine().trim();

            if (sku.isEmpty()) {
                System.out.println("SKU cannot be empty!");
                continue;
            }

            // Check duplicate SKU immediately
            if (dao.skuExists(sku)) {
                System.out.println(
                        "This SKU already exists! Enter another SKU."
                );
                continue;
            }

            break;
        }


        // Category
        System.out.print("Enter Category: ");
        String category = sc.nextLine().trim();

        if (category.isEmpty()) {
            category = null;
        }


        // Price
        BigDecimal price;

        while (true) {

            System.out.print("Enter Selling Price: ");

            try {

                price = new BigDecimal(sc.nextLine().trim());

                if (price.compareTo(BigDecimal.ZERO) >= 0) {
                    break;
                }

                System.out.println("Price cannot be negative!");

            } catch (NumberFormatException e) {

                System.out.println("Enter a valid price!");
            }
        }


        // Quantity
        int quantity = readNonNegativeInt(
                sc,
                "Enter Quantity: "
        );


        // Reorder Level
        int reorderLevel = readNonNegativeInt(
                sc,
                "Enter Reorder Level: "
        );


        // Create Product Object
        Product product = new Product();

        product.setProductName(name);
        product.setSku(sku);
        product.setCategory(category);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setReorderLevel(reorderLevel);


        // Save Product in Database
        int productId = dao.addProduct(product);

        System.out.println("\nProduct added successfully!");
        System.out.println("Generated Product ID: " + productId);
    }


    // ================= READ NON-NEGATIVE INTEGER =================

    public static int readNonNegativeInt(
            Scanner sc,
            String message) {

        while (true) {

            System.out.print(message);

            try {

                int value = Integer.parseInt(
                        sc.nextLine().trim()
                );

                if (value >= 0) {
                    return value;
                }

                System.out.println("Value cannot be negative!");

            } catch (NumberFormatException e) {

                System.out.println(
                        "Please enter a valid whole number!"
                );
            }
        }
    }
}
