
package org.myStore.entities;

import java.sql.Timestamp;

public class StockRequest {

    // Request types
    public enum RequestType {
        STOCK_IN,
        STOCK_OUT
    }

    // Request statuses
    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    private int requestId;
    private int productId;
    private String productName;
    private int requestedBy;
    private String username;
    private RequestType requestType;
    private int requestedQuantity;
    private String reason;
    private RequestStatus status;
    private Timestamp createdAt;

    public StockRequest() {
    }

    public StockRequest(int requestId, int productId,
                        String productName, int requestedBy,
                        String username, RequestType requestType,
                        int requestedQuantity, String reason,
                        RequestStatus status, Timestamp createdAt) {

        this.requestId = requestId;
        this.productId = productId;
        this.productName = productName;
        this.requestedBy = requestedBy;
        this.username = username;
        this.requestType = requestType;
        this.requestedQuantity = requestedQuantity;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(int requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
