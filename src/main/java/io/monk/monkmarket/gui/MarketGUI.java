package io.monk.monkmarket.gui;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.MarketItem;
import io.monk.monkmarket.utils.ItemUtils;
import io.monk.monkmarket.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class MarketGUI {
    
    private static final Map<UUID, Integer> playerPages = new HashMap<>();
    
    public static void openMainMenu(Player player, int page) {
        MonkMarket plugin = MonkMarket.getInstance();
        
        if (!plugin.isDatabaseConnected()) {
            player.sendMessage("§cМаркет временно не работает!");
            return;
        }
        
        String searchQuery = plugin.getMarketManager().getPlayerSearch(player.getUniqueId());
        List<MarketItem> items = plugin.getMarketManager().getMarketItems(page, searchQuery);
        int totalPages = plugin.getMarketManager().getTotalPages(searchQuery);
        
        if (page < 1) page = 1;
        if (totalPages > 0 && page > totalPages) page = totalPages;
        
        String title = plugin.getConfig().getString("gui.main-menu.title", "&8Рынок - Страница %page%")
            .replace("%page%", String.valueOf(page));
            
        Inventory inv = Bukkit.createInventory(new MarketHolder(MarketHolder.InventoryType.MAIN), 54, StringUtils.colorize(title));
        
        setupDecoration(inv);
        setupItems(inv, player, items, plugin);
        setupNavigation(inv, page, totalPages, player);
        
        if (plugin.getConfigManager().isRefreshButtonEnabled()) {
            setupRefreshButton(inv);
        }
        
        setupSearchInfo(inv, searchQuery);
        
        playerPages.put(player.getUniqueId(), page);
        
        player.openInventory(inv);
        
        if (plugin.getConfigManager().isSoundEnabled("open")) {
            plugin.getSoundManager().playClickSound(player);
        }
    }
    
    public static void openMyItemsMenu(Player player, int page) {
        MyItemsGUI.openMenu(player, page);
    }
    
    public static void openBuyMenu(Player player, MarketItem marketItem) {
        MonkMarket plugin = MonkMarket.getInstance();
        List<MarketItem> similarItems = plugin.getMarketManager().getSimilarItems(marketItem.getItemMaterial(), marketItem.getId());
        BuyGUI.openAdvancedBuyMenu(player, marketItem, similarItems);
    }
    
    private static void setupDecoration(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<>()));
        }
        
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<>()));
        }
    }
    
    private static void setupItems(Inventory inv, Player player, List<MarketItem> items, MonkMarket plugin) {
        int startSlot = 9;
        int itemsPerPage = plugin.getConfig().getInt("gui.items-per-page", 36);
        
        Map<String, MarketItem> groupedItems = new LinkedHashMap<>();
        
        for (MarketItem item : items) {
            String materialName = item.getItemMaterial();
            if (!groupedItems.containsKey(materialName)) {
                groupedItems.put(materialName, item);
            }
        }
        
        List<MarketItem> displayItems = new ArrayList<>(groupedItems.values());
        
        for (int i = 0; i < Math.min(displayItems.size(), itemsPerPage) && startSlot + i < 45; i++) {
            MarketItem marketItem = displayItems.get(i);
            ItemStack displayItem = createMarketItemDisplay(marketItem, player, plugin);
            inv.setItem(startSlot + i, displayItem);
        }
    }
    
    private static void setupNavigation(Inventory inv, int currentPage, int totalPages, Player player) {
        if (currentPage > 1) {
            ItemStack prevItem = ItemUtils.createGuiItem(
                Material.ARROW,
                "§eПредыдущая страница",
                "§7Страница " + (currentPage - 1) + " из " + totalPages,
                "",
                "§eНажмите для перехода"
            );
            inv.setItem(45, prevItem);
        }
        
        MonkMarket plugin = MonkMarket.getInstance();
        int myItemsCount = plugin.getMarketManager().getPlayerTotalPages(player.getUniqueId());
        ItemStack myItemsButton = ItemUtils.createGuiItem(
            Material.CHEST,
            "§6Мои лоты",
            "§7Ваши предметы на продаже",
            "§7Активных лотов: §a" + myItemsCount,
            "",
            "§eНажмите чтобы открыть"
        );
        inv.setItem(49, myItemsButton);
        
        if (currentPage < totalPages) {
            ItemStack nextItem = ItemUtils.createGuiItem(
                Material.ARROW,
                "§eСледующая страница",
                "§7Страница " + (currentPage + 1) + " из " + totalPages,
                "",
                "§eНажмите для перехода"
            );
            inv.setItem(53, nextItem);
        }
    }
    
    private static void setupRefreshButton(Inventory inv) {
        int slot = MonkMarket.getInstance().getConfigManager().getRefreshButtonSlot();
        
        ItemStack refreshItem = ItemUtils.createGuiItem(
            Material.CLOCK,
            "§eОбновить",
            "§7Обновить список товаров",
            "§7Новые товары появятся сразу",
            "",
            "§eНажмите для обновления"
        );
        inv.setItem(slot, refreshItem);
    }
    
    private static void setupSearchInfo(Inventory inv, String searchQuery) {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            ItemStack searchInfo = ItemUtils.createGuiItem(
                Material.COMPASS,
                "§6Поиск: " + searchQuery,
                "§7Результаты по запросу",
                "§7Для сброса поиска напишите",
                "§7&o- /market search"
            );
            inv.setItem(8, searchInfo);
        }
    }
    
    private static ItemStack createMarketItemDisplay(MarketItem marketItem, Player player, MonkMarket plugin) {
        ItemStack item = marketItem.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }
        
        if (meta != null) {
            String displayName = plugin.getItemManager().getItemDisplayName(item);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Цена за 1: §6" + String.format("%.2f", marketItem.getPricePerUnit()));
            lore.add("§7Доступно: §a" + marketItem.getAvailableAmount() + " шт.");
            lore.add("§7У продавцов: §a" + countSellers(marketItem, plugin) + " игроков");
            lore.add("");
            lore.add("§eНажмите для покупки");
            
            if (meta.hasLore()) {
                List<String> itemLore = meta.getLore();
                if (itemLore != null) {
                    lore.addAll(itemLore);
                }
            }
            
            meta.setDisplayName("§f" + displayName);
            meta.setLore(lore);
            
            meta.setCustomModelData(marketItem.getId());
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private static int countSellers(MarketItem targetItem, MonkMarket plugin) {
        if (targetItem == null || targetItem.getItemMaterial() == null) {
            return 0;
        }
        
        List<MarketItem> allItems = plugin.getMarketManager().getMarketItems(1, "");
        if (allItems == null || allItems.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        String targetMaterial = targetItem.getItemMaterial();
        
        for (MarketItem item : allItems) {
            if (item != null && targetMaterial.equals(item.getItemMaterial())) {
                count++;
            }
        }
        
        return count;
    }
    
    public static int getPlayerPage(UUID uuid) {
        return playerPages.getOrDefault(uuid, 1);
    }
    
    public static void removePlayerPage(UUID uuid) {
        if (uuid == null) {
            playerPages.clear();
        } else {
            playerPages.remove(uuid);
        }
    }
    
    public static void refreshForPlayer(Player player) {
        int page = getPlayerPage(player.getUniqueId());
        openMainMenu(player, page);
    }
}