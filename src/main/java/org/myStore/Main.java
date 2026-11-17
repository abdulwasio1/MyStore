
package org.myStore;

import org.myStore.auth.AuthService;
import org.myStore.auth.Role;
import org.myStore.auth.Session;
import org.myStore.dao.DashboardDAO;
import org.myStore.dao.ProductDAO;
import org.myStore.dao.StockRequestDAO;
import org.myStore.dsa.ProductHashTable;
import org.myStore.dsa.ProductPriorityHeap;
import org.myStore.dsa.StockRequestQueue;
import org.myStore.entities.DashboardStats;
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
        DashboardDAO dashboardDAO = new DashboardDAO();

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

            System.out.println("\n================================================");
            System.out.println("       WELCOME TO MYSTORE");
            System.out.println("================================================");
            System.out.println("Logged in as: " + user.getUsername());
            System.out.println("Role: " + user.getRole());

            boolean logout = false;

            while (!logout) {
                // Display dashboard whenever the user returns to the main menu
                try {
                    displayDashboard(dashboardDAO, user);
                } catch (SQLException e) {
                    System.out.println(
                            "Unable to load dashboard: " + e.getMessage()
                    );
                }

                System.out.println("\n========== MAIN MENU ==========");

                System.out.println("1. Search Product by SKU");
                System.out.println("2. Search Product by Name");
                System.out.println("3. View Low Stock Priorities");

                if (Session.hasRole(Role.ADMIN)) {
                    System.out.println("4. View All Products");
                    System.out.println("5. Add New Product");
                    System.out.println("6. Create Staff Account");
                    System.out.println("7. Process Stock Requests");
                    System.out.println("8. Update Product");
                    System.out.println("9. Delete Product");
                } else {
                    System.out.println("4. View All Products");
                    System.out.println("5. Submit Stock Request");
                    System.out.println("6. View All My Requests");
                }

                System.out.println("0. Logout");
                int choice = readNonNegativeInt(sc, "Enter choice: ");

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
                        case 8:

                            if (Session.hasRole(Role.ADMIN)) {

                                boolean updated = updateProduct(sc, productDAO, hashTable);

                                if (updated) {
                                    products = productDAO.getAllProducts();
                                    hashTable = buildHashTable(products);
                                }

                            } else {
                                System.out.println("Access denied!");
                            }

                            break;
                        case 9:
                            if (Session.hasRole(Role.ADMIN)) {

                                boolean deleted = deleteProduct(
                                        sc, productDAO, hashTable
                                );

                                if (deleted) {
                                    products = productDAO.getAllProducts();
                                    hashTable = buildHashTable(products);
                                }

                            } else {
                                System.out.println("Access denied!");
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




    private static boolean updateProduct(
            Scanner sc,
            ProductDAO productDAO,
            ProductHashTable hashTable) throws SQLException {

        Session.requireAdmin();

        System.out.println("\n========== UPDATE PRODUCT ==========");
        System.out.println("1. Search by Product ID");
        System.out.println("2. Search by SKU");
        System.out.println("3. Search by Product Name");

        int choice = readNonNegativeInt(sc, "Choose search method: ");

        Product product = null;

        // ================= SEARCH PRODUCT =================

        if (choice == 1) {

            int productId = readPositiveInt(sc, "Enter Product ID: ");

            product = hashTable.searchById(productId);

        } else if (choice == 2) {

            System.out.print("Enter SKU: ");
            String sku = sc.nextLine().trim();

            product = hashTable.searchBySKU(sku);

        } else if (choice == 3) {
            System.out.print("Enter Product Name or Keyword: ");
            String search = sc.nextLine().trim();

            if (search.isEmpty()) {
                System.out.println("Search keyword cannot be empty.");
                return false;
            }

            ArrayList<Product> products = productDAO.getAllProducts();
            ArrayList<Product> matches = new ArrayList<>();

            // Partial, case-insensitive linear search
            for (Product p : products) {

                if (p.getProductName() != null &&
                        p.getProductName().toLowerCase()
                                .contains(search.toLowerCase())) {

                    matches.add(p);
                }
            }

            if (matches.isEmpty()) {
                System.out.println("No matching products found.");
                return false;
            }

            System.out.println("\n========== SEARCH RESULTS ==========");
            System.out.println("Matching Products: " + matches.size());

            System.out.printf(
                    "%-6s %-25s %-18s %-18s%n",
                    "ID", "PRODUCT NAME", "SKU", "CATEGORY"
            );

            System.out.println("----------------------------------------------------------------");

            for (Product p : matches) {

                System.out.printf(
                        "%-6d %-25.25s %-18.18s %-18.18s%n",
                        p.getProductId(),
                        p.getProductName(),
                        p.getSku(),
                        p.getCategory() == null ? "N/A" : p.getCategory()
                );
            }

            System.out.println("----------------------------------------------------------------");

            int selectedId = readPositiveInt(
                    sc, "Enter Product ID to update (0 to cancel): "
            );

            if (selectedId == 0) {
                System.out.println("Update cancelled.");
                return false;
            }

            // Only allow selection from displayed search results
            for (Product p : matches) {

                if (p.getProductId() == selectedId) {
                    product = p;
                    break;
                }
            }

            if (product == null) {
                System.out.println("Invalid Product ID selection.");
                return false;
            }
        } else {

            System.out.println("Invalid search choice.");
            return false;
        }

        // ================= CHECK PRODUCT =================

        if (product == null) {
            System.out.println("Product not found.");
            return false;
        }

        System.out.println("\nCurrent Product Details:");
        displayProduct(product);

        System.out.println("\n========== UPDATE DETAILS ==========");
        System.out.println("Enter SAME to keep the existing value.");

        // ================= UPDATE NAME =================

        System.out.print("Product Name [" + product.getProductName() + "]: ");

        while (true) {

            String name = sc.nextLine().trim();

            if (name.equalsIgnoreCase("SAME")) {
                break;
            }

            if (name.isEmpty()) {
                System.out.print("Name cannot be empty. Enter again: ");
                continue;
            }

            product.setProductName(name);
            break;
        }

        // ================= UPDATE SKU =================

        System.out.print("SKU [" + product.getSku() + "]: ");

        while (true) {

            String sku = sc.nextLine().trim();

            if (sku.equalsIgnoreCase("SAME")) {
                break;
            }

            if (sku.isEmpty()) {
                System.out.print("SKU cannot be empty. Enter again: ");
                continue;
            }

            if (productDAO.skuExistsForOtherProduct(
                    sku, product.getProductId())) {

                System.out.print(
                        "This SKU already belongs to another product. Enter a different SKU: "
                );
                continue;
            }

            product.setSku(sku);
            break;
        }

        // ================= UPDATE CATEGORY =================

        System.out.print("Category [" + product.getCategory() + "]: ");

        String category = sc.nextLine().trim();

        if (!category.equalsIgnoreCase("SAME")) {

            if (category.isEmpty()) {
                product.setCategory(null);
            } else {
                product.setCategory(category);
            }
        }

        // ================= UPDATE PRICE =================

        System.out.print("Price [" + product.getPrice() + "]: ");

        while (true) {

            String priceInput = sc.nextLine().trim();

            if (priceInput.equalsIgnoreCase("SAME")) {
                break;
            }

            try {

                if (priceInput.isEmpty()) {
                    System.out.print("Price cannot be empty. Enter again: ");
                    continue;
                }

                BigDecimal price = new BigDecimal(priceInput);

                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.print("Price cannot be negative. Enter again: ");
                    continue;
                }

                product.setPrice(price);
                break;

            } catch (NumberFormatException e) {

                System.out.print("Invalid price. Enter again: ");
            }
        }

        // ================= UPDATE REORDER LEVEL =================

        System.out.print(
                "Reorder Level [" + product.getReorderLevel() + "]: ");

        while (true) {

            String reorderInput = sc.nextLine().trim();

            if (reorderInput.equalsIgnoreCase("SAME")) {
                break;
            }

            try {

                if (reorderInput.isEmpty()) {
                    System.out.print(
                            "Reorder level cannot be empty. Enter again: ");
                    continue;
                }

                int reorderLevel = Integer.parseInt(reorderInput);

                if (reorderLevel < 0) {
                    System.out.print(
                            "Reorder level cannot be negative. Enter again: ");
                    continue;
                }

                product.setReorderLevel(reorderLevel);
                break;

            } catch (NumberFormatException e) {

                System.out.print("Invalid reorder level. Enter again: ");
            }
        }

        // ================= SAVE CHANGES =================

        System.out.println("\nSaving changes...");

        boolean updated = productDAO.updateProduct(product);

        if (updated) {

            System.out.println("\nProduct updated successfully!");

            System.out.println("\nUpdated Product Details:");
            displayProduct(product);

            return true;

        } else {

            System.out.println("Product update failed.");
            return false;
        }
    }





    // ================= SEARCH BY SKU =================

    private static void searchProductBySKU(
            Scanner sc,
            ProductHashTable hashTable) {

        System.out.print("Enter product SKU: ");
        String sku = sc.nextLine().trim();

        Product product = hashTable.searchBySKU(sku);

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

        Product product = hashTable.searchBySKU(sku);

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


    private static void displayDashboard(
            DashboardDAO dashboardDAO,
            User user) throws SQLException {

        boolean isAdmin = user.getRole() == Role.ADMIN;

        DashboardStats stats = dashboardDAO.getDashboardStats(
                user.getUserId(), isAdmin
        );

        System.out.println("\n");
        System.out.println("==============================================");
        System.out.println("              MYSTORE DASHBOARD");
        System.out.println("==============================================");

        System.out.println("Welcome, " + user.getUsername());
        System.out.println("Role: " + user.getRole());

        System.out.println("----------------------------------------------");
        System.out.println("             INVENTORY OVERVIEW");
        System.out.println("----------------------------------------------");

        System.out.printf("%-28s : %d%n",
                "Total Products", stats.getTotalProducts());

        System.out.printf("%-28s : %d%n",
                "Total Units in Stock", stats.getTotalUnits());

        System.out.printf("%-28s : %d%n",
                "Low Stock Products", stats.getLowStockProducts());

        System.out.printf("%-28s : %d%n",
                "Out of Stock Products", stats.getOutOfStockProducts());

        if (isAdmin) {
            System.out.printf("%-28s : %s%n",
                    "Total Inventory Value",
                    stats.getInventoryValue().toPlainString());
        }

        System.out.println("----------------------------------------------");

        if (isAdmin) {
            System.out.println("Pending Stock Requests: "
                    + stats.getPendingRequests());
        } else {
            System.out.println("My Pending Requests: "
                    + stats.getPendingRequests());
        }

        System.out.println("==============================================");
    }


    private static boolean deleteProduct(
            Scanner sc,
            ProductDAO productDAO,
            ProductHashTable hashTable) {

        Session.requireAdmin();

        try {
            ArrayList<Product> products = productDAO.getAllProducts();

            if (products.isEmpty()) {
                System.out.println("No products available to delete.");
                return false;
            }

            System.out.println("\n========== DELETE PRODUCT ==========");
            System.out.println("1. Search by Product ID");
            System.out.println("2. Search by SKU");
            System.out.println("3. Search by Product Name / Keyword");
            System.out.println("0. Cancel");

            int choice = readNonNegativeInt(sc, "Enter choice: ");

            Product selectedProduct = null;

            // Search by Product ID
            if (choice == 1) {

                int id = readNonNegativeInt(sc, "Enter Product ID: ");

                if (id == 0) {
                    System.out.println("Deletion cancelled.");
                    return false;
                }

                selectedProduct = hashTable.searchById(id);
            }

            // Search by SKU
            else if (choice == 2) {

                System.out.print("Enter Product SKU: ");
                String sku = sc.nextLine().trim();

                if (sku.isEmpty()) {
                    System.out.println("SKU cannot be empty.");
                    return false;
                }

                selectedProduct = hashTable.searchBySKU(sku);
            }

            // Search by Product Name / Keyword
            else if (choice == 3) {

                System.out.print("Enter Product Name or Keyword: ");
                String keyword = sc.nextLine().trim();

                if (keyword.isEmpty()) {
                    System.out.println("Search keyword cannot be empty.");
                    return false;
                }

                ArrayList<Product> matches = new ArrayList<>();

                for (Product p : products) {
                    if (p.getProductName().toLowerCase()
                            .contains(keyword.toLowerCase())) {
                        matches.add(p);
                    }
                }

                if (matches.isEmpty()) {
                    System.out.println("No matching products found.");
                    return false;
                }

                System.out.println("\n========== MATCHING PRODUCTS ==========");
                System.out.printf("%-8s %-25s %-18s %-15s%n",
                        "ID", "Product Name", "SKU", "Category");

                for (Product p : matches) {
                    System.out.printf("%-8d %-25s %-18s %-15s%n",
                            p.getProductId(),
                            p.getProductName(),
                            p.getSku(),
                            p.getCategory());
                }

                System.out.println("---------------------------------------");
                System.out.println("Enter 0 to cancel.");

                int selectedId =
                        readNonNegativeInt(sc, "Select Product ID: ");

                if (selectedId == 0) {
                    System.out.println("Deletion cancelled.");
                    return false;
                }

                // Ensure selected ID belongs to search results
                for (Product p : matches) {
                    if (p.getProductId() == selectedId) {
                        selectedProduct = p;
                        break;
                    }
                }

                if (selectedProduct == null) {
                    System.out.println("Invalid Product ID selection.");
                    return false;
                }
            }

            else if (choice == 0) {
                System.out.println("Deletion cancelled.");
                return false;
            }

            else {
                System.out.println("Invalid choice.");
                return false;
            }

            if (selectedProduct == null) {
                System.out.println("Product not found.");
                return false;
            }

            // Display selected product
            System.out.println("\n========== SELECTED PRODUCT ==========");
            displayProduct(selectedProduct);

            System.out.println("\nWARNING: This action will permanently delete:");
            System.out.println("1. The selected product");
            System.out.println("2. All associated stock requests");
            System.out.println("3. Pending, approved, and rejected requests");

            System.out.print("\nAre you sure? (Y/N): ");
            String confirmation = sc.nextLine().trim();

            if (!confirmation.equalsIgnoreCase("Y")) {
                System.out.println("Deletion cancelled.");
                return false;
            }

            boolean deleted =
                    productDAO.deleteProduct(selectedProduct.getProductId());

            if (deleted) {
                System.out.println(
                        "\nProduct and associated stock requests deleted successfully!");
                return true;
            }

            System.out.println("Product could not be deleted.");
            return false;

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }



}
