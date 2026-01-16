package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import java.util.*;

public class ItemManager {
    
    private final MonkMarket plugin;
    
    public ItemManager(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    public boolean isValidItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        List<String> blacklist = plugin.getConfigManager().getBlacklistedItems();
        if (blacklist.contains(item.getType().name())) {
            return false;
        }
        
        if (plugin.getConfigManager().isCheckNBT()) {
            return validateItemNBT(item);
        }

        return true;
    }
    
    private boolean validateItemNBT(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return true;
        }
        
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            if (displayName.contains("§k") || displayName.contains("&k")) {
                return false;
            }
        }
        
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                if (line.contains("§k") || line.contains("&k")) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public String getItemDisplayName(ItemStack item) {
        Material material = item.getType();
        String materialName = material.name();
        
        String displayName = plugin.getConfigManager().getItemDisplayName(materialName);
        if (displayName.isEmpty()) {
            displayName = formatMaterialName(materialName);
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            displayName = meta.getDisplayName().replace("§", "&");
        }
        
        if (material == Material.POTION || material == Material.SPLASH_POTION || 
            material == Material.LINGERING_POTION || material == Material.TIPPED_ARROW) {
            displayName = getPotionName(item, displayName);
        }
        
        if (material == Material.TOTEM_OF_UNDYING) {
            if (plugin.getConfigManager().isCustomItemsSupport()) {
                if (meta != null && meta.hasDisplayName()) {
                    displayName = meta.getDisplayName().replace("§", "&");
                }
            }
        }
        
        return displayName;
    }
    
    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    private String getPotionName(ItemStack item, String baseName) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) meta;
            PotionData potionData = potionMeta.getBasePotionData();
            PotionType type = potionData.getType();
            
            String typeName = type.name().toLowerCase();
            typeName = typeName.replace("_", " ");
            
            StringBuilder name = new StringBuilder();
            if (item.getType() == Material.SPLASH_POTION) {
                name.append("Взрывное зелье ");
            } else if (item.getType() == Material.LINGERING_POTION) {
                name.append("Туманное зелье ");
            } else if (item.getType() == Material.TIPPED_ARROW) {
                name.append("Стрела ");
            } else {
                name.append("Зелье ");
            }
            
            name.append(formatMaterialName(typeName));
            
            if (potionData.isUpgraded()) {
                name.append(" II");
            }
            
            if (potionData.isExtended()) {
                name.append("+");
            }
            
            return name.toString();
        }
        
        return baseName;
    }
    
    public boolean isCustomItem(ItemStack item) {
        if (!plugin.getConfigManager().isCustomItemsSupport()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        if (meta.hasDisplayName()) {
            return true;
        }
        
        if (meta.hasEnchants()) {
            return true;
        }
        
        if (meta.hasLore()) {
            return true;
        }
        
        Material material = item.getType();
        return material == Material.TOTEM_OF_UNDYING || 
               material == Material.POTION || 
               material == Material.SPLASH_POTION || 
               material == Material.LINGERING_POTION ||
               material == Material.ENCHANTED_BOOK;
    }
    
    public String getItemCategory(ItemStack item) {
        Material material = item.getType();
        String materialName = material.name();
        
        if (materialName.contains("SWORD") || materialName.contains("AXE") || 
            materialName.contains("BOW") || material == Material.CROSSBOW || 
            material == Material.TRIDENT) {
            return "weapon";
        }
        
        if (materialName.contains("PICKAXE") || materialName.contains("SHOVEL") || 
            materialName.contains("HOE") || material == Material.FISHING_ROD || 
            material == Material.SHEARS) {
            return "tool";
        }
        
        if (materialName.contains("HELMET") || materialName.contains("CHESTPLATE") || 
            materialName.contains("LEGGINGS") || materialName.contains("BOOTS")) {
            return "armor";
        }
        
        if (material == Material.POTION || material == Material.SPLASH_POTION || 
            material == Material.LINGERING_POTION || material == Material.TIPPED_ARROW) {
            return "potion";
        }
        
        if (material == Material.TOTEM_OF_UNDYING) {
            return "totem";
        }
        
        if (material == Material.ENCHANTED_BOOK) {
            return "enchanted_book";
        }
        
        if (material.isBlock()) {
            return "block";
        }
        
        return "other";
    }
}