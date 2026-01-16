package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.MarketItem;
import io.monk.monkmarket.utils.InventoryUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class ValidationManager {
    
    private final MonkMarket plugin;
    
    public ValidationManager(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    public ValidationResult validateSell(Player player, ItemStack item, String priceStr, String amountStr) {
        ValidationResult result = new ValidationResult();
        
        if (item == null || item.getType() == Material.AIR) {
            result.setSuccess(false);
            result.setMessage("no-item-in-hand");
            return result;
        }
        
        if (!plugin.getItemManager().isValidItem(item)) {
            result.setSuccess(false);
            result.setMessage("invalid-item");
            return result;
        }
        
        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                result.setSuccess(false);
                result.setMessage("invalid-price");
                return result;
            }
            
            double maxPrice = plugin.getConfig().getDouble("economy.max-price", 1000000.0);
            if (price > maxPrice) {
                result.setSuccess(false);
                result.setMessage("price-too-high");
                return result;
            }
            
            double minPrice = plugin.getConfig().getDouble("economy.min-price", 1.0);
            if (price < minPrice) {
                result.setSuccess(false);
                result.setMessage("price-too-low");
                return result;
            }
        } catch (NumberFormatException e) {
            result.setSuccess(false);
            result.setMessage("invalid-price-format");
            return result;
        }
        
        int amount;
        if (amountStr != null && !amountStr.isEmpty()) {
            try {
                amount = Integer.parseInt(amountStr);
                if (amount <= 0) {
                    result.setSuccess(false);
                    result.setMessage("invalid-amount");
                    return result;
                }
                
                if (amount > item.getAmount()) {
                    result.setSuccess(false);
                    result.setMessage("not-enough-items");
                    return result;
                }
                
                int maxAmount = plugin.getConfig().getInt("items.max-sell-amount", 2304);
                if (amount > maxAmount) {
                    result.setSuccess(false);
                    result.setMessage("amount-too-high");
                    return result;
                }
            } catch (NumberFormatException e) {
                result.setSuccess(false);
                result.setMessage("invalid-amount-format");
                return result;
            }
        } else {
            amount = item.getAmount();
            
            int maxAmount = plugin.getConfig().getInt("items.max-sell-amount", 2304);
            if (amount > maxAmount) {
                result.setSuccess(false);
                result.setMessage("amount-too-high");
                return result;
            }
        }
        
        List<String> blacklist = plugin.getConfigManager().getBlacklistedItems();
        if (blacklist.contains(item.getType().name())) {
            result.setSuccess(false);
            result.setMessage("item-blacklisted");
            return result;
        }
        
        int maxItemsPerPlayer = plugin.getConfig().getInt("settings.max-items-per-player", 45);
        if (!player.hasPermission("monkmarket.bypass.limit")) {
            int playerItemsCount = plugin.getMarketManager().getPlayerTotalPages(player.getUniqueId());
            if (playerItemsCount >= maxItemsPerPlayer) {
                result.setSuccess(false);
                result.setMessage("item-limit-reached");
                return result;
            }
        }
        
        result.setSuccess(true);
        result.setPrice(price);
        result.setAmount(amount);
        result.setItem(item);
        return result;
    }
    
    public ValidationResult validatePurchase(Player player, int itemId, int amount) {
        ValidationResult result = new ValidationResult();
        
        MarketItem marketItem = plugin.getMarketManager().getItemById(itemId);
        if (marketItem == null) {
            result.setSuccess(false);
            result.setMessage("item-not-found");
            return result;
        }
        
        if (marketItem.isSold()) {
            result.setSuccess(false);
            result.setMessage("item-already-sold");
            return result;
        }
        
        if (!marketItem.canSell(amount)) {
            result.setSuccess(false);
            result.setMessage("not-enough-stock");
            return result;
        }
        
        double totalPrice = marketItem.getTotalPrice(amount);
        
        if (!plugin.getEconomyManager().hasEnough(player, totalPrice)) {
            result.setSuccess(false);
            result.setMessage("not-enough-money");
            return result;
        }
        
        if (plugin.getConfigManager().isCheckInventorySpace()) {
            ItemStack itemStack = marketItem.getItemStack();
            if (itemStack != null) {
                if (!InventoryUtils.hasInventorySpace(player, itemStack, amount)) {
                    result.setSuccess(false);
                    result.setMessage("no-inventory-space");
                    return result;
                }
            }
        }
        
        if (player.getUniqueId().equals(marketItem.getSellerUuid())) {
            result.setSuccess(false);
            result.setMessage("cannot-buy-own-item");
            return result;
        }
        
        result.setSuccess(true);
        result.setMarketItem(marketItem);
        result.setTotalPrice(totalPrice);
        result.setAmount(amount);
        return result;
    }
    
    public static class ValidationResult {
        private boolean success;
        private String message;
        private double price;
        private int amount;
        private double totalPrice;
        private ItemStack item;
        private MarketItem marketItem;
        
        public boolean isSuccess() { 
            return success; 
        }
        
        public void setSuccess(boolean success) { 
            this.success = success; 
        }
        
        public String getMessage() { 
            return message; 
        }
        
        public void setMessage(String message) { 
            this.message = message; 
        }
        
        public double getPrice() { 
            return price; 
        }
        
        public void setPrice(double price) { 
            this.price = price; 
        }
        
        public int getAmount() { 
            return amount; 
        }
        
        public void setAmount(int amount) { 
            this.amount = amount; 
        }
        
        public double getTotalPrice() { 
            return totalPrice; 
        }
        
        public void setTotalPrice(double totalPrice) { 
            this.totalPrice = totalPrice; 
        }
        
        public ItemStack getItem() { 
            return item; 
        }
        
        public void setItem(ItemStack item) { 
            this.item = item; 
        }
        
        public MarketItem getMarketItem() { 
            return marketItem; 
        }
        
        public void setMarketItem(MarketItem marketItem) { 
            this.marketItem = marketItem; 
        }
    }
}