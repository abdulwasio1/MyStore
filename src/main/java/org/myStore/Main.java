
package org.myStore;

import org.myStore.auth.AuthService;
import org.myStore.auth.Role;
import org.myStore.auth.Session;
import org.myStore.dao.ProductDAO;
import org.myStore.dao.StockRequestDAO;
import org.myStore.dsa.ProductHashTable;
import org.myStore.dsa.ProductPriorityHeap;
import org.myStore.dsa.StockRequestQueue;
import org.myStore.entities.Product;
import org.myStore.entities.StockRequest;
import org.myStore.entities.StockRequest.RequestStatus;
import org.myStore.entities.StockRequest.RequestType;
import org.myStore.entities.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        ProductDAO productDAO = new ProductDAO();
        StockRequestDAO requestDAO = new StockRequestDAO();
        AuthService authService = new AuthService();

        ArrayList<Product> products = productDAO.getAllProducts();

        ProductHashTable hashTable = buildHashTable(products);

        boolean running = true;

        while (running) {

            User user = loginUser(sc, authService);

            if (user == null) {
                System.out.println("Unable to login. Please try again.");
                continue;
            }

            Session.login(user);

            System.out.println("\n================================");
            System.out.println("       WELCOME TO MYSTORE");
            System.out.println("================================");
            System.out.println("Logged in as: " + user.getUsername());
            System.out.println("Role: " + user.getRole());

            boolean logout = false;

            while (!logout) {

                System.out.println("\n========== MAIN MENU ==========");

                System.out.println("1. Search Product by SKU");
                System.out.println("2. Search Product by Name");
                System.out.println("3. View Low Stock Priorities");

                if (Session.hasRole(Role.ADMIN)) {
                    System.out.println("4. View All Products");
                    System.out.println("5. Add New Product");
                    System.out.println("6. Create Staff Account");
                    System.out.println("7. Process Stock Requests");
                } else {
                    System.out.println("4. View All Products");
                    System.out.println("5. Submit Stock Request");
                    System.out.println("6. View All My Requests");
                }

                System.out.println("0. Logout");
                System.out.print("Enter choice: ");

                int choice = readNonNegativeInt(sc, "Choice: ");

                try {

                    switch (choice) {

                        case 1:
                            searchProductBySKU(sc, hashTable);
                            break;

                        case 2:
                            searchProductByName(sc, products);
                            break;

                        case 3:
                            viewLowStockPriorities(products);
                            break;
                        case 4:
                            viewAllProducts(productDAO);
                            break;

                        case 5:

                            if (Session.hasRole(Role.ADMIN)) {

                                addProduct(sc, productDAO);

                                products = productDAO.getAllProducts();
                                hashTable = buildHashTable(products);

                            } else {

                                submitStockRequest(sc, hashTable, requestDAO);

                            }

                            break;

                        case 6:

                            if (Session.hasRole(Role.ADMIN)) {

                                createStaffAccount(sc, authService);

                            } else {

                                viewMyRequests(requestDAO);

                            }

                            break;

                        case 7:

                            if (Session.hasRole(Role.ADMIN)) {

                                processStockRequests(sc, requestDAO);

                                products = productDAO.getAllProducts();
                                hashTable = buildHashTable(products);

                            } else {

                                System.out.println("Invalid choice!");

                            }

                            break;

                        case 0:

                            Session.logout();
                            logout = true;

                            System.out.println("Logged out successfully.");

                            break;

                        default:
                            System.out.println("Invalid choice!");

                    }

                } catch (SQLException e) {

                    System.out.println("Database error: " + e.getMessage());

                } catch (SecurityException e) {

                    System.out.println(e.getMessage());

                }
            }
        }

        sc.close();
    }

    // ================= LOGIN =================

    private static User loginUser(Scanner sc, AuthService authService) {

        System.out.println("\n========== LOGIN ==========");

        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        try {

            User user = authService.login(username, password);

            if (user == null) {
                System.out.println("Invalid username or password.");
            }

            return user;

        } catch (SQLException e) {

            System.out.println("Login failed: " + e.getMessage());
            return null;

        }
    }

    // ================= HASH TABLE =================

    private static ProductHashTable buildHashTable(ArrayList<Product> products) {

        int capacity = Math.max(10, products.size() * 2);

        ProductHashTable hashTable = new ProductHashTable(capacity);

        for (Product product : products) {
            hashTable.insert(product);
        }

        return hashTable;
    }

    // ================= SEARCH BY SKU =================

    private static void searchProductBySKU(
            Scanner sc,
            ProductHashTable hashTable) {

        System.out.print("Enter product SKU: ");
        String sku = sc.nextLine().trim();

        Product product = hashTable.search(sku);

        if (product == null) {

            System.out.println("Product not found.");

        } else {

            displayProduct(product);

        }
    }

    // ================= SEARCH BY NAME =================

    private static void searchProductByName(
            Scanner sc,
            ArrayList<Product> products) {

        System.out.print("Enter product name: ");
        String name = sc.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Product name cannot be empty.");
            return;
        }

        boolean found = false;

        for (Product product : products) {

            if (product.getProductName()
                    .toLowerCase()
                    .contains(name.toLowerCase())) {

                displayProduct(product);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No matching products found.");
        }
    }

    // ================= LOW STOCK PRIORITIES =================

    private static void viewLowStockPriorities(
            ArrayList<Product> products) {

        ProductPriorityHeap heap = new ProductPriorityHeap();

        for (Product product : products) {

            if (product.getQuantity() <= product.getReorderLevel()) {
                heap.insert(product);
            }
        }

        if (heap.isEmpty()) {

            System.out.println("No low-stock products found.");
            return;

        }

        System.out.println("\n========== LOW STOCK PRIORITIES ==========");

        System.out.println("Products are displayed from highest shortage priority.");

        while (!heap.isEmpty()) {

            Product product = heap.extractMax();

            displayProduct(product);

            int shortage = product.getReorderLevel()
                    - product.getQuantity();

            System.out.println("Shortage: " + shortage);
            System.out.println("--------------------------------");
        }
    }

    // ================= ADD PRODUCT =================

    private static void addProduct(
            Scanner sc,
            ProductDAO productDAO) throws SQLException {

        Session.requireAdmin();

        Product product = new Product();

        System.out.println("\n========== ADD NEW PRODUCT ==========");

        System.out.print("Product name: ");
        String name = sc.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Product name cannot be empty.");
            return;
        }

        System.out.print("SKU: ");
        String sku = sc.nextLine().trim();

        if (sku.isEmpty()) {
            System.out.println("SKU cannot be empty.");
            return;
        }

        if (productDAO.skuExists(sku)) {
            System.out.println("This SKU already exists.");
            return;
        }

        System.out.print("Category: ");
        String category = sc.nextLine().trim();

        BigDecimal price = readNonNegativePrice(sc);

        int quantity = readNonNegativeInt(sc, "Quantity: ");

        int reorderLevel = readNonNegativeInt(sc, "Reorder level: ");

        product.setProductName(name);
        product.setSku(sku);
        product.setCategory(category);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setReorderLevel(reorderLevel);

        int productId = productDAO.addProduct(product);

        System.out.println("\nProduct added successfully!");
        System.out.println("Generated Product ID: " + productId);
    }

    // ================= CREATE STAFF ACCOUNT =================

    private static void createStaffAccount(
            Scanner sc,
            AuthService authService) throws SQLException {

        Session.requireAdmin();

        System.out.println("\n========== CREATE STAFF ACCOUNT ==========");

        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        System.out.print("Confirm password: ");
        String confirmPassword = sc.nextLine();

        if (!password.equals(confirmPassword)) {

            System.out.println("Passwords do not match.");
            return;

        }

        try {

            authService.createStaffAccount(username, password);

            System.out.println("Staff account created successfully.");

        } catch (IllegalArgumentException e) {

            System.out.println(e.getMessage());

        }
    }

    // ================= SUBMIT STOCK REQUEST =================

    private static void submitStockRequest(
            Scanner sc,
            ProductHashTable hashTable,
            StockRequestDAO requestDAO) throws SQLException {

        System.out.println("\n========== SUBMIT STOCK REQUEST ==========");

        System.out.print("Enter product SKU: ");
        String sku = sc.nextLine().trim();

        Product product = hashTable.search(sku);

        if (product == null) {

            System.out.println("Product not found.");
            return;

        }

        displayProduct(product);

        System.out.println("\n1. STOCK IN");
        System.out.println("2. STOCK OUT");

        int typeChoice = readNonNegativeInt(sc, "Select request type: ");

        RequestType requestType;

        if (typeChoice == 1) {

            requestType = RequestType.STOCK_IN;

        } else if (typeChoice == 2) {

            requestType = RequestType.STOCK_OUT;

        } else {

            System.out.println("Invalid request type.");
            return;

        }

        int quantity = readPositiveInt(sc, "Enter quantity: ");

        System.out.print("Enter reason: ");
        String reason = sc.nextLine().trim();

        StockRequest request = new StockRequest();

        request.setProductId(product.getProductId());
        request.setRequestedBy(Session.getCurrentUser().getUserId());
        request.setRequestType(requestType);
        request.setRequestedQuantity(quantity);
        request.setReason(reason);

        int requestId = requestDAO.createRequest(request);

        System.out.println("\nStock request submitted successfully!");
        System.out.println("Request ID: " + requestId);
        System.out.println("Status: PENDING");
        System.out.println("An admin must approve this request.");
    }

    // ================= VIEW ALL PRODUCTS =================

    private static void viewAllProducts(ProductDAO productDAO) {

        System.out.println("\n========== ALL PRODUCTS ==========");

        ArrayList<Product> products = productDAO.getAllProducts();

        if (products.isEmpty()) {

            System.out.println("No products available in inventory.");
            return;

        }

        System.out.println("Total Products: " + products.size());

        System.out.println("----------------------------------------------------------------------------------------------");

        System.out.printf(
                "%-6s %-20s %-15s %-15s %-12s %-10s %-12s%n",
                "ID",
                "NAME",
                "SKU",
                "CATEGORY",
                "PRICE",
                "QUANTITY",
                "REORDER"
        );

        System.out.println("----------------------------------------------------------------------------------------------");

        for (Product product : products) {

            System.out.printf(
                    "%-6d %-20.20s %-15.15s %-15.15s %-12s %-10d %-12d%n",
                    product.getProductId(),
                    product.getProductName(),
                    product.getSku(),
                    product.getCategory() == null
                            ? "N/A"
                            : product.getCategory(),
                    product.getPrice().toString(),
                    product.getQuantity(),
                    product.getReorderLevel()
            );

        }

        System.out.println("----------------------------------------------------------------------------------------------");

    }

    // ================= VIEW MY REQUESTS =================

    private static void viewMyRequests(
            StockRequestDAO requestDAO) throws SQLException {

        int userId = Session.getCurrentUser().getUserId();

        ArrayList<StockRequest> requests =
                requestDAO.getMyRequests(userId);

        System.out.println("\n========== MY STOCK REQUESTS ==========");

        if (requests.isEmpty()) {

            System.out.println("You have not submitted any requests.");
            return;

        }

        for (StockRequest request : requests) {

            System.out.println("\nRequest ID: " + request.getRequestId());
            System.out.println("Product: " + request.getProductName());
            System.out.println("Type: " + request.getRequestType());
            System.out.println("Quantity: " + request.getRequestedQuantity());
            System.out.println("Reason: " + request.getReason());
            System.out.println("Status: " + request.getStatus());
            System.out.println("Date: " + request.getCreatedAt());

            System.out.println("--------------------------------");
        }
    }

    // ================= PROCESS STOCK REQUESTS =================

    private static void processStockRequests(
            Scanner sc,
            StockRequestDAO requestDAO) throws SQLException {

        Session.requireAdmin();

        ArrayList<StockRequest> pendingRequests =
                requestDAO.getPendingRequests();

        StockRequestQueue queue = new StockRequestQueue();

        for (StockRequest request : pendingRequests) {
            queue.enqueue(request);
        }

        if (queue.isEmpty()) {

            System.out.println("No pending stock requests.");
            return;

        }

        System.out.println("\n========== PENDING STOCK REQUESTS ==========");

//        queue.display();

        while (!queue.isEmpty()) {

            StockRequest request = queue.peek();

            System.out.println("\n========== PROCESS REQUEST ==========");

            System.out.println("Request ID: " + request.getRequestId());
            System.out.println("Product: " + request.getProductName());
            System.out.println("Requested By: " + request.getUsername());
            System.out.println("Type: " + request.getRequestType());
            System.out.println("Quantity: " + request.getRequestedQuantity());
            System.out.println("Reason: " + request.getReason());

            System.out.println("\n1. Approve");
            System.out.println("2. Reject");
            System.out.println("0. Stop processing");

            int choice = readNonNegativeInt(sc, "Enter choice: ");

            if (choice == 0) {

                System.out.println("Processing stopped.");
                break;

            }

            int adminId = Session.getCurrentUser().getUserId();

            if (choice == 1) {

                requestDAO.approveRequest(
                        request.getRequestId(),
                        adminId
                );

                System.out.println("Request approved successfully.");

                queue.dequeue();

            } else if (choice == 2) {

                requestDAO.rejectRequest(
                        request.getRequestId(),
                        adminId
                );

                System.out.println("Request rejected successfully.");

                queue.dequeue();

            } else {

                System.out.println("Invalid choice.");
            }
        }

        System.out.println("\nRemaining requests in queue: "
                + queue.getSize());
    }

    // ================= DISPLAY PRODUCT =================

    private static void displayProduct(Product product) {

        System.out.println("\n========== PRODUCT DETAILS ==========");

        System.out.println("Product ID: " + product.getProductId());
        System.out.println("Name: " + product.getProductName());
        System.out.println("SKU: " + product.getSku());
        System.out.println("Category: " + product.getCategory());
        System.out.println("Price: " + product.getPrice());
        System.out.println("Quantity: " + product.getQuantity());
        System.out.println("Reorder Level: " + product.getReorderLevel());

        System.out.println("=====================================");
    }

    // ================= INPUT VALIDATION =================

    private static int readNonNegativeInt(
            Scanner sc,
            String message) {

        while (true) {

            System.out.print(message);

            try {

                int value = Integer.parseInt(sc.nextLine().trim());

                if (value >= 0) {
                    return value;
                }

                System.out.println("Value cannot be negative.");

            } catch (NumberFormatException e) {

                System.out.println("Please enter a valid integer.");

            }
        }
    }

    private static int readPositiveInt(
            Scanner sc,
            String message) {

        while (true) {

            int value = readNonNegativeInt(sc, message);

            if (value > 0) {
                return value;
            }

            System.out.println("Value must be greater than zero.");
        }
    }

    private static BigDecimal readNonNegativePrice(Scanner sc) {

        while (true) {

            System.out.print("Price: ");

            try {

                BigDecimal price =
                        new BigDecimal(sc.nextLine().trim());

                if (price.compareTo(BigDecimal.ZERO) >= 0) {
                    return price;
                }

                System.out.println("Price cannot be negative.");

            } catch (NumberFormatException e) {

                System.out.println("Enter a valid price.");

            }
        }
    }
}
