package io.monk.monkmarket.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class ItemUtils {
    
    public static ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(StringUtils.colorize(name));
            }
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(StringUtils.colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public static ItemStack createGuiItem(Material material, String name, String... lore) {
        return createGuiItem(material, name, Arrays.asList(lore));
    }
    
    public static boolean hasSpaceForItems(ItemStack[] inventory, ItemStack itemToAdd, int amount) {
        int remaining = amount;
        ItemStack testItem = itemToAdd.clone();
        testItem.setAmount(1);
        
        for (ItemStack slot : inventory) {
            if (slot == null || slot.getType() == Material.AIR) {
                remaining -= testItem.getMaxStackSize();
                if (remaining <= 0) return true;
            } else if (slot.isSimilar(testItem) && slot.getAmount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getAmount();
                remaining -= space;
                if (remaining <= 0) return true;
            }
        }
        
        return remaining <= 0;
    }
    
    public static int calculateSpaceForItem(ItemStack[] inventory, ItemStack itemToAdd) {
        int totalSpace = 0;
        ItemStack testItem = itemToAdd.clone();
        testItem.setAmount(1);
        
        for (ItemStack slot : inventory) {
            if (slot == null || slot.getType() == Material.AIR) {
                totalSpace += testItem.getMaxStackSize();
            } else if (slot.isSimilar(testItem) && slot.getAmount() < slot.getMaxStackSize()) {
                totalSpace += slot.getMaxStackSize() - slot.getAmount();
            }
        }
        
        return totalSpace;
    }
    
    public static void addItemsToInventory(ItemStack[] inventory, ItemStack itemToAdd, int amount) {
        int remaining = amount;
        ItemStack item = itemToAdd.clone();
        item.setAmount(1);
        
        for (int i = 0; i < inventory.length && remaining > 0; i++) {
            if (inventory[i] == null || inventory[i].getType() == Material.AIR) {
                int stackSize = Math.min(remaining, item.getMaxStackSize());
                ItemStack newStack = item.clone();
                newStack.setAmount(stackSize);
                inventory[i] = newStack;
                remaining -= stackSize;
            } else if (inventory[i].isSimilar(item) && inventory[i].getAmount() < item.getMaxStackSize()) {
                int space = item.getMaxStackSize() - inventory[i].getAmount();
                int addAmount = Math.min(remaining, space);
                inventory[i].setAmount(inventory[i].getAmount() + addAmount);
                remaining -= addAmount;
            }
        }
    }
    
    public static String getItemName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "Воздух";
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        
        return StringUtils.formatMaterialName(item.getType().name());
    }
}