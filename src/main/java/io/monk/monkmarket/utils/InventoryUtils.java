package io.monk.monkmarket.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryUtils {
    
    public static boolean hasInventorySpace(Player player, ItemStack item, int amount) {
        PlayerInventory inventory = player.getInventory();
        return ItemUtils.hasSpaceForItems(inventory.getContents(), item, amount);
    }
    
    public static int getAvailableSpace(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        return ItemUtils.calculateSpaceForItem(inventory.getContents(), item);
    }
    
    public static boolean addToInventory(Player player, ItemStack item, int amount) {
        if (!hasInventorySpace(player, item, amount)) {
            return false;
        }
        
        PlayerInventory inventory = player.getInventory();
        ItemUtils.addItemsToInventory(inventory.getContents(), item, amount);
        player.updateInventory();
        return true;
    }
    
    public static int countItems(Player player, ItemStack item) {
        int total = 0;
        PlayerInventory inventory = player.getInventory();
        
        for (ItemStack slot : inventory.getContents()) {
            if (slot != null && slot.isSimilar(item)) {
                total += slot.getAmount();
            }
        }
        
        return total;
    }
    
    public static void removeItems(Player player, ItemStack item, int amount) {
        int remaining = amount;
        PlayerInventory inventory = player.getInventory();
        
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot != null && slot.isSimilar(item)) {
                int slotAmount = slot.getAmount();
                if (slotAmount <= remaining) {
                    inventory.setItem(i, null);
                    remaining -= slotAmount;
                } else {
                    slot.setAmount(slotAmount - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.updateInventory();
    }
}