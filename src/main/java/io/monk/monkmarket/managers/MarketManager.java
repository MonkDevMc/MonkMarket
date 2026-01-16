package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.MarketItem;
import io.monk.monkmarket.database.models.Transaction;
import io.monk.monkmarket.database.queries.ItemQueries;
import io.monk.monkmarket.database.queries.TransactionQueries;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class MarketManager {
    
    private final MonkMarket plugin;
    private final ItemQueries itemQueries;
    private final TransactionQueries transactionQueries;
    private final Map<UUID, String> playerSearches;
    private final Map<Integer, MarketItem> itemCache;
    private List<MarketItem> allItemsCache;
    private long allItemsCacheTime;
    private static final long CACHE_DURATION = 30000;
    
    public MarketManager(MonkMarket plugin) {
        this.plugin = plugin;
        this.itemQueries = new ItemQueries(plugin);
        this.transactionQueries = new TransactionQueries(plugin);
        this.playerSearches = new HashMap<>();
        this.itemCache = new HashMap<>();
        this.allItemsCache = null;
        this.allItemsCacheTime = 0;
    }
    
    public boolean addItemToMarket(Player seller, ItemStack item, double pricePerUnit, int totalAmount) {
        MarketItem marketItem = new MarketItem(
            seller.getUniqueId(),
            seller.getName(),
            item,
            pricePerUnit,
            totalAmount,
            plugin.getConfig().getString("server-id", "default")
        );
        
        boolean success = itemQueries.addItem(marketItem);
        if (success && marketItem.getId() > 0) {
            itemCache.put(marketItem.getId(), marketItem);
            invalidateAllItemsCache();
            plugin.getLogger().info("[Market] Item added: " + marketItem.getId() + " by " + seller.getName());
        }
        return success;
    }
    
    public List<MarketItem> getMarketItems(int page, String searchQuery) {
        int itemsPerPage = plugin.getConfig().getInt("items-per-page", 36);
        List<MarketItem> items = itemQueries.getMarketItems(page, itemsPerPage, searchQuery);
        
        if (items == null) {
            return new ArrayList<>();
        }
        
        if (plugin.getConfigManager().isAlphabeticalSorting()) {
            items.sort((item1, item2) -> {
                String name1 = plugin.getConfigManager().getItemDisplayName(item1.getItemMaterial());
                String name2 = plugin.getConfigManager().getItemDisplayName(item2.getItemMaterial());
                if (name1 == null) name1 = "";
                if (name2 == null) name2 = "";
                return name1.compareToIgnoreCase(name2);
            });
        }
        
        return items;
    }
    
    public int getTotalPages(String searchQuery) {
        int itemsPerPage = plugin.getConfig().getInt("items-per-page", 36);
        int totalItems = itemQueries.getTotalItemsCount(searchQuery);
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }
    
    public List<MarketItem> getPlayerItems(UUID playerUuid, int page) {
        int itemsPerPage = plugin.getConfig().getInt("items-per-page", 36);
        List<MarketItem> items = itemQueries.getPlayerItems(playerUuid, page, itemsPerPage);
        return items != null ? items : new ArrayList<>();
    }
    
    public int getPlayerTotalPages(UUID playerUuid) {
        int itemsPerPage = plugin.getConfig().getInt("items-per-page", 36);
        int totalItems = itemQueries.getPlayerItemsCount(playerUuid);
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }
    
    public int getPlayerItemsCount(UUID playerUuid) {
        return itemQueries.getPlayerItemsCount(playerUuid);
    }
    
    public MarketItem getItemById(int itemId) {
        if (itemId <= 0) return null;
        
        if (itemCache.containsKey(itemId)) {
            MarketItem cached = itemCache.get(itemId);
            if (cached != null && !cached.isSold()) {
                return cached;
            }
        }
        
        MarketItem item = itemQueries.getItemById(itemId);
        if (item != null) {
            itemCache.put(itemId, item);
        }
        return item;
    }
    
    public List<MarketItem> getAllMarketItems() {
        long currentTime = System.currentTimeMillis();
        if (allItemsCache != null && (currentTime - allItemsCacheTime) < CACHE_DURATION) {
            return new ArrayList<>(allItemsCache);
        }
        
        List<MarketItem> items = itemQueries.getMarketItems(1, 10000, "");
        if (items == null) {
            items = new ArrayList<>();
        }
        
        allItemsCache = new ArrayList<>(items);
        allItemsCacheTime = currentTime;
        return items;
    }
    
    public List<MarketItem> getSimilarItems(String materialName, int excludeId) {
        if (materialName == null || materialName.isEmpty()) {
            return new ArrayList<>();
        }
        
        return itemQueries.getSimilarItems(materialName, excludeId);
    }
    
    public boolean purchaseItem(Player buyer, int itemId, int amount) {
        MarketItem marketItem = getItemById(itemId);
        if (marketItem == null) {
            plugin.getLogger().warning("[Purchase] Item not found: " + itemId);
            return false;
        }
        
        if (!marketItem.canSell(amount)) {
            plugin.getLogger().warning("[Purchase] Not enough stock: " + itemId + 
                ", requested: " + amount + ", available: " + marketItem.getAvailableAmount());
            return false;
        }
        
        double totalPrice = marketItem.getTotalPrice(amount);
        
        if (!plugin.getEconomyManager().hasEnough(buyer, totalPrice)) {
            plugin.getLogger().warning("[Purchase] Not enough money: " + buyer.getName() + 
                ", need: " + totalPrice + ", have: " + plugin.getEconomyManager().getBalance(buyer));
            return false;
        }
        
        try {
            plugin.getEconomyManager().withdraw(buyer, totalPrice);
        } catch (Exception e) {
            plugin.getLogger().severe("[Purchase] Failed to withdraw money from buyer: " + buyer.getName() + ", error: " + e.getMessage());
            return false;
        }
        
        OfflinePlayer seller = plugin.getServer().getOfflinePlayer(marketItem.getSellerUuid());
        
        try {
            plugin.getEconomyManager().deposit(seller, totalPrice);
        } catch (Exception e) {
            try {
                plugin.getEconomyManager().deposit(buyer, totalPrice);
            } catch (Exception e2) {
                plugin.getLogger().severe("[Purchase] CRITICAL: Failed to return money to buyer after deposit error: " + e2.getMessage());
            }
            plugin.getLogger().severe("[Purchase] Failed to deposit money to seller: " + marketItem.getSellerName() + ", error: " + e.getMessage());
            return false;
        }
        
        int newSoldAmount = marketItem.getSoldAmount() + amount;
        boolean updateSuccess = itemQueries.updateSoldAmount(itemId, newSoldAmount);
        
        if (updateSuccess) {
            marketItem.setSoldAmount(newSoldAmount);
            
            if (newSoldAmount >= marketItem.getTotalAmount()) {
                if (itemQueries.markAsSold(itemId, buyer.getUniqueId())) {
                    marketItem.setSold(true);
                    itemCache.remove(itemId);
                    invalidateAllItemsCache();
                }
            } else {
                itemCache.put(itemId, marketItem);
            }
            
            Transaction transaction = new Transaction(
                buyer.getUniqueId(),
                buyer.getName(),
                marketItem.getSellerUuid(),
                marketItem.getSellerName(),
                marketItem.getItemMaterial(),
                amount,
                totalPrice,
                marketItem.getPricePerUnit(),
                plugin.getConfig().getString("server-id", "default")
            );
            
            transactionQueries.addTransaction(transaction);
            plugin.getLogManager().logTransaction(transaction);
            
            plugin.getLogger().info("[Purchase] Successful: " + buyer.getName() + " bought " + amount + "x " + 
                                  marketItem.getItemMaterial() + " from " + marketItem.getSellerName() + " for " + totalPrice);
            
            return true;
        } else {
            try {
                plugin.getEconomyManager().deposit(buyer, totalPrice);
                plugin.getEconomyManager().withdraw(seller, totalPrice);
            } catch (Exception e) {
                plugin.getLogger().severe("[Purchase] CRITICAL: Failed to rollback money after DB error: " + e.getMessage());
            }
            plugin.getLogger().severe("[Purchase] Database update failed for item: " + itemId);
            return false;
        }
    }
    
    public void setPlayerSearch(UUID playerUuid, String query) {
        if (query == null || query.trim().isEmpty()) {
            playerSearches.remove(playerUuid);
        } else {
            playerSearches.put(playerUuid, query.trim());
        }
    }
    
    public String getPlayerSearch(UUID playerUuid) {
        return playerSearches.get(playerUuid);
    }
    
    public void clearPlayerSearch(UUID playerUuid) {
        playerSearches.remove(playerUuid);
    }
    
    public void clearItemCache(int itemId) {
        itemCache.remove(itemId);
    }
    
    public void invalidateAllItemsCache() {
        allItemsCache = null;
        allItemsCacheTime = 0;
    }
    
    public void clearAllCache() {
        try {
            int itemCacheSize = itemCache.size();
            itemCache.clear();
            invalidateAllItemsCache();
            playerSearches.clear();
        
            plugin.getLogger().info("Кэш очищен: " + itemCacheSize + " предметов");
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при очистке кэша: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getMarketStatistics() {
        return transactionQueries.getMarketStatistics();
    }
    
    public ItemQueries getItemQueries() {
        return itemQueries;
    }
    
    public void cleanup() {
        clearAllCache();
    }
}