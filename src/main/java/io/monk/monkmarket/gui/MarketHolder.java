package io.monk.monkmarket.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.UUID;

public class MarketHolder implements InventoryHolder {
    
    private final UUID holderId;
    private InventoryType type;
    
    public MarketHolder() {
        this.holderId = UUID.randomUUID();
        this.type = InventoryType.MAIN;
    }
    
    public MarketHolder(InventoryType type) {
        this.holderId = UUID.randomUUID();
        this.type = type;
    }
    
    @Override
    public Inventory getInventory() {
        return null;
    }
    
    public UUID getHolderId() {
        return holderId;
    }
    
    public InventoryType getType() {
        return type;
    }
    
    public void setType(InventoryType type) {
        this.type = type;
    }
    
    public enum InventoryType {
        MAIN,
        MY_ITEMS,
        BUY
    }
}