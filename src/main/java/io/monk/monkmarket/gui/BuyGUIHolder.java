package io.monk.monkmarket.gui;

import org.bukkit.inventory.Inventory;

public class BuyGUIHolder extends MarketHolder {
    
    private final int itemId;
    private final double pricePerUnit;
    private int selectedAmount;
    private Inventory inventory;
    
    public BuyGUIHolder(int itemId, double pricePerUnit) {
        super(InventoryType.BUY);
        this.itemId = itemId;
        this.pricePerUnit = pricePerUnit;
        this.selectedAmount = 1;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
    
    public int getItemId() {
        return itemId;
    }
    
    public double getPricePerUnit() {
        return pricePerUnit;
    }
    
    public int getSelectedAmount() {
        return selectedAmount;
    }
    
    public void setSelectedAmount(int selectedAmount) {
        if (selectedAmount < 1) {
            this.selectedAmount = 1;
        } else {
            this.selectedAmount = selectedAmount;
        }
    }
    
    public void incrementAmount(int amount) {
        this.selectedAmount += amount;
        if (this.selectedAmount < 1) {
            this.selectedAmount = 1;
        }
    }
    
    public void setMaxAmount(int maxAmount) {
        if (this.selectedAmount > maxAmount) {
            this.selectedAmount = maxAmount;
        }
    }
    
    public double getTotalPrice() {
        return pricePerUnit * selectedAmount;
    }
    
    public double getTotalPriceForAmount(int amount) {
        return pricePerUnit * amount;
    }
    
    public boolean isValidAmount(int amount) {
        return amount > 0;
    }
    
    public String getFormattedPrice() {
        return String.format("%.2f", getTotalPrice());
    }
    
    public String getFormattedPriceForAmount(int amount) {
        return String.format("%.2f", getTotalPriceForAmount(amount));
    }
}