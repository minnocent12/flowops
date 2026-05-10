package com.flowops.warehouse.model;

public class WarehouseStock {
    private String warehouseId;
    private String sku;
    private int quantity;

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String w) { this.warehouseId = w; }
    public String getSku() { return sku; }
    public void setSku(String s) { this.sku = s; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }
}
