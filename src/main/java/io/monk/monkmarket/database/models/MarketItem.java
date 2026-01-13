package io.monk.monkmarket.database.models;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.google.gson.Gson;
import java.util.UUID;
import java.util.Date;

public class MarketItem {
    private int id;
    private UUID sellerUuid;
    private String sellerName;
    private String itemMaterial;
    private String itemData;
    private int itemAmount;
    private double pricePerUnit;
    private int totalAmount;
    private int soldAmount;
    private Date createdAt;
    private String serverId;
    private boolean isSold;
    private UUID buyerUuid;
    private Date soldAt;
    
    private transient ItemStack itemStack;
    private transient Gson gson = new Gson();
    
    public MarketItem() {}
    
    public MarketItem(UUID sellerUuid, String sellerName, ItemStack item, 
                     double pricePerUnit, int totalAmount, String serverId) {
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemMaterial = item.getType().name();
        this.itemAmount = item.getAmount();
        this.pricePerUnit = pricePerUnit;
        this.totalAmount = totalAmount;
        this.soldAmount = 0;
        this.serverId = serverId;
        this.isSold = false;
        this.itemStack = item.clone();
        
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            this.itemData = gson.toJson(meta);
        }
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public UUID getSellerUuid() { return sellerUuid; }
    public void setSellerUuid(UUID sellerUuid) { this.sellerUuid = sellerUuid; }
    
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    
    public String getItemMaterial() { return itemMaterial; }
    public void setItemMaterial(String itemMaterial) { this.itemMaterial = itemMaterial; }
    
    public String getItemData() { return itemData; }
    public void setItemData(String itemData) { this.itemData = itemData; }
    
    public int getItemAmount() { return itemAmount; }
    public void setItemAmount(int itemAmount) { this.itemAmount = itemAmount; }
    
    public double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = pricePerUnit; }
    
    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }
    
    public int getSoldAmount() { return soldAmount; }
    public void setSoldAmount(int soldAmount) { this.soldAmount = soldAmount; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    
    public boolean isSold() { return isSold; }
    public void setSold(boolean sold) { isSold = sold; }
    
    public UUID getBuyerUuid() { return buyerUuid; }
    public void setBuyerUuid(UUID buyerUuid) { this.buyerUuid = buyerUuid; }
    
    public Date getSoldAt() { return soldAt; }
    public void setSoldAt(Date soldAt) { this.soldAt = soldAt; }
    
    public ItemStack getItemStack() { return itemStack; }
    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }
    
    public int getAvailableAmount() {
        return totalAmount - soldAmount;
    }
    
    public double getTotalPrice(int amount) {
        return pricePerUnit * amount;
    }
    
    public boolean canSell(int amount) {
        return getAvailableAmount() >= amount;
    }
}