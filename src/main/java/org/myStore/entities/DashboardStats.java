
package org.myStore.entities;

import java.math.BigDecimal;

public class DashboardStats {

    private long totalProducts;
    private long totalUnits;
    private long lowStockProducts;
    private long outOfStockProducts;
    private long pendingRequests;
    private BigDecimal inventoryValue;

    public DashboardStats(
            long totalProducts,
            long totalUnits,
            long lowStockProducts,
            long outOfStockProducts,
            long pendingRequests,
            BigDecimal inventoryValue) {

        this.totalProducts = totalProducts;
        this.totalUnits = totalUnits;
        this.lowStockProducts = lowStockProducts;
        this.outOfStockProducts = outOfStockProducts;
        this.pendingRequests = pendingRequests;
        this.inventoryValue = inventoryValue;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public long getTotalUnits() {
        return totalUnits;
    }

    public long getLowStockProducts() {
        return lowStockProducts;
    }

    public long getOutOfStockProducts() {
        return outOfStockProducts;
    }

    public long getPendingRequests() {
        return pendingRequests;
    }

    public BigDecimal getInventoryValue() {
        return inventoryValue;
    }
}
