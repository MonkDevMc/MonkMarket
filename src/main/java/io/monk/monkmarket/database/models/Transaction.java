package io.monk.monkmarket.database.models;

import java.util.UUID;
import java.util.Date;

public class Transaction {
    private int id;
    private UUID buyerUuid;
    private String buyerName;
    private UUID sellerUuid;
    private String sellerName;
    private String itemMaterial;
    private int itemAmount;
    private double totalPrice;
    private double pricePerUnit;
    private Date transactionTime;
    private String serverId;
    private boolean isPaid;
    private Date paidAt;
    
    public Transaction() {}
    
    public Transaction(UUID buyerUuid, String buyerName, UUID sellerUuid, 
                      String sellerName, String itemMaterial, int itemAmount,
                      double totalPrice, double pricePerUnit, String serverId) {
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemMaterial = itemMaterial;
        this.itemAmount = itemAmount;
        this.totalPrice = totalPrice;
        this.pricePerUnit = pricePerUnit;
        this.serverId = serverId;
        this.isPaid = false;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public UUID getBuyerUuid() { return buyerUuid; }
    public void setBuyerUuid(UUID buyerUuid) { this.buyerUuid = buyerUuid; }
    
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    
    public UUID getSellerUuid() { return sellerUuid; }
    public void setSellerUuid(UUID sellerUuid) { this.sellerUuid = sellerUuid; }
    
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    
    public String getItemMaterial() { return itemMaterial; }
    public void setItemMaterial(String itemMaterial) { this.itemMaterial = itemMaterial; }
    
    public int getItemAmount() { return itemAmount; }
    public void setItemAmount(int itemAmount) { this.itemAmount = itemAmount; }
    
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    
    public double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = pricePerUnit; }
    
    public Date getTransactionTime() { return transactionTime; }
    public void setTransactionTime(Date transactionTime) { this.transactionTime = transactionTime; }
    
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
    
    public Date getPaidAt() { return paidAt; }
    public void setPaidAt(Date paidAt) { this.paidAt = paidAt; }
}