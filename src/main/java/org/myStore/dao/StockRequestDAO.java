
package org.myStore.dao;

import org.myStore.db.dbConnection;
import org.myStore.entities.StockRequest;
import org.myStore.entities.StockRequest.RequestStatus;
import org.myStore.entities.StockRequest.RequestType;

import java.sql.*;
import java.util.ArrayList;

public class StockRequestDAO {

    // 1. Save a new stock request
    public int createRequest(StockRequest request) throws SQLException {

        if (request == null || request.getRequestedQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Request quantity must be greater than zero."
            );
        }

        String query = """
                INSERT INTO stock_requests
                (product_id, requested_by, request_type,
                 requested_quantity, reason)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = dbConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(
                     query, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, request.getProductId());
            pst.setInt(2, request.getRequestedBy());
            pst.setString(3, request.getRequestType().name());
            pst.setInt(4, request.getRequestedQuantity());
            pst.setString(5, request.getReason());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new SQLException("Could not retrieve request ID.");
        }
    }

    // 2. Load all pending requests in FIFO order
    public ArrayList<StockRequest> getPendingRequests()
            throws SQLException {

        ArrayList<StockRequest> requests = new ArrayList<>();

        String query = """
                SELECT sr.*, p.product_name, u.username
                FROM stock_requests sr
                JOIN products p ON sr.product_id = p.product_id
                JOIN users u ON sr.requested_by = u.user_id
                WHERE sr.status = 'PENDING'
                ORDER BY sr.request_id ASC
                """;

        try (Connection con = dbConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                requests.add(mapRequest(rs));
            }
        }

        return requests;
    }

    // 3. Load requests submitted by a particular staff member
    public ArrayList<StockRequest> getMyRequests(int userId)
            throws SQLException {

        ArrayList<StockRequest> requests = new ArrayList<>();

        String query = """
                SELECT sr.*, p.product_name, u.username
                FROM stock_requests sr
                JOIN products p ON sr.product_id = p.product_id
                JOIN users u ON sr.requested_by = u.user_id
                WHERE sr.requested_by = ?
                ORDER BY sr.request_id DESC
                """;

        try (Connection con = dbConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            pst.setInt(1, userId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRequest(rs));
                }
            }
        }

        return requests;
    }

    // 4. Approve a request and update product quantity
    public void approveRequest(int requestId, int adminId)
            throws SQLException {

        String requestQuery = """
                SELECT product_id, request_type, requested_quantity, status
                FROM stock_requests
                WHERE request_id = ?
                FOR UPDATE
                """;

        String productQuery = """
                SELECT quantity
                FROM products
                WHERE product_id = ?
                FOR UPDATE
                """;

        String updateProductQuery = """
                UPDATE products
                SET quantity = ?
                WHERE product_id = ?
                """;

        String updateRequestQuery = """
                UPDATE stock_requests
                SET status = 'APPROVED',
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP
                WHERE request_id = ?
                  AND status = 'PENDING'
                """;

        try (Connection con = dbConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                int productId;
                int requestedQuantity;
                String requestType;

                // Lock and retrieve the request
                try (PreparedStatement pst =
                             con.prepareStatement(requestQuery)) {

                    pst.setInt(1, requestId);

                    try (ResultSet rs = pst.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException("Request not found.");
                        }

                        if (!"PENDING".equals(rs.getString("status"))) {
                            throw new SQLException(
                                    "This request has already been processed."
                            );
                        }

                        productId = rs.getInt("product_id");
                        requestedQuantity =
                                rs.getInt("requested_quantity");
                        requestType = rs.getString("request_type");
                    }
                }

                // Lock and retrieve current product quantity
                int currentQuantity;

                try (PreparedStatement pst =
                             con.prepareStatement(productQuery)) {

                    pst.setInt(1, productId);

                    try (ResultSet rs = pst.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException("Product not found.");
                        }

                        currentQuantity = rs.getInt("quantity");
                    }
                }

                // Calculate new stock quantity
                long newQuantity;

                if ("STOCK_IN".equals(requestType)) {

                    newQuantity = (long) currentQuantity
                            + requestedQuantity;

                    if (newQuantity > Integer.MAX_VALUE) {
                        throw new SQLException(
                                "Stock quantity exceeds the allowed limit."
                        );
                    }

                } else if ("STOCK_OUT".equals(requestType)) {

                    if (requestedQuantity > currentQuantity) {
                        throw new SQLException(
                                "Insufficient stock to approve this request."
                        );
                    }

                    newQuantity = (long) currentQuantity
                            - requestedQuantity;

                } else {
                    throw new SQLException("Invalid request type.");
                }

                // Update product quantity
                try (PreparedStatement pst =
                             con.prepareStatement(updateProductQuery)) {

                    pst.setInt(1, (int) newQuantity);
                    pst.setInt(2, productId);
                    pst.executeUpdate();
                }

                // Mark request as approved
                try (PreparedStatement pst =
                             con.prepareStatement(updateRequestQuery)) {

                    pst.setInt(1, adminId);
                    pst.setInt(2, requestId);

                    int rows = pst.executeUpdate();

                    if (rows == 0) {
                        throw new SQLException(
                                "Request could not be approved."
                        );
                    }
                }

                // Save both changes together
                con.commit();

            } catch (SQLException e) {

                con.rollback();
                throw e;
            }
        }
    }

    // 5. Reject a pending request
    public void rejectRequest(int requestId, int adminId)
            throws SQLException {

        String query = """
                UPDATE stock_requests
                SET status = 'REJECTED',
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP
                WHERE request_id = ?
                  AND status = 'PENDING'
                """;

        try (Connection con = dbConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            pst.setInt(1, adminId);
            pst.setInt(2, requestId);

            int rows = pst.executeUpdate();

            if (rows == 0) {
                throw new SQLException(
                        "Request not found or already processed."
                );
            }
        }
    }

    // Convert a database row into a StockRequest object
    private StockRequest mapRequest(ResultSet rs)
            throws SQLException {

        return new StockRequest(
                rs.getInt("request_id"),
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getInt("requested_by"),
                rs.getString("username"),
                RequestType.valueOf(rs.getString("request_type")),
                rs.getInt("requested_quantity"),
                rs.getString("reason"),
                RequestStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at")
        );
    }
}
