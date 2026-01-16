package io.monk.monkmarket.gui;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.MarketItem;
import io.monk.monkmarket.utils.InventoryUtils;
import io.monk.monkmarket.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class MyItemsGUI {
    
    private static final Map<UUID, Integer> playerPages = new HashMap<>();
    
    public static void openMenu(Player player, int page) {
        MonkMarket plugin = MonkMarket.getInstance();
        
        if (!plugin.isDatabaseConnected()) {
            player.sendMessage("§cМаркет временно не работает!");
            return;
        }
        
        List<MarketItem> items = plugin.getMarketManager().getPlayerItems(player.getUniqueId(), page);
        int totalPages = plugin.getMarketManager().getPlayerTotalPages(player.getUniqueId());
        
        if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }
        
        String title = plugin.getConfig().getString("gui.my-items.title", "&8Мои лоты - Страница %page%/%total%")
            .replace("%page%", String.valueOf(page))
            .replace("%total%", String.valueOf(totalPages));
        Inventory inv = Bukkit.createInventory(new MarketHolder(MarketHolder.InventoryType.MY_ITEMS), 54, title);
        
        setupDecoration(inv);
        setupItems(inv, player, items, plugin);
        setupNavigation(inv, page, totalPages);
        setupInfo(inv, player, plugin);
        
        playerPages.put(player.getUniqueId(), page);
        player.openInventory(inv);
    }
    
    private static void setupDecoration(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<String>()));
        }
        
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<String>()));
        }
    }
    
    private static void setupItems(Inventory inv, Player player, List<MarketItem> items, MonkMarket plugin) {
        int startSlot = 9;
        
        for (int i = 0; i < items.size(); i++) {
            if (startSlot + i >= 45) break;
            
            MarketItem marketItem = items.get(i);
            ItemStack displayItem = createItemDisplay(marketItem, player, plugin);
            inv.setItem(startSlot + i, displayItem);
        }
    }
    
    private static void setupNavigation(Inventory inv, int currentPage, int totalPages) {
        if (currentPage > 1) {
            ItemStack prevItem = ItemUtils.createGuiItem(
                Material.ARROW,
                "§eПредыдущая страница",
                "§7Страница " + (currentPage - 1)
            );
            inv.setItem(45, prevItem);
        }
        
        ItemStack pageInfo = ItemUtils.createGuiItem(
            Material.BOOK,
            "§6Страница " + currentPage + "/" + totalPages,
            "§7Всего страниц: " + totalPages
        );
        inv.setItem(49, pageInfo);
        
        if (currentPage < totalPages) {
            ItemStack nextItem = ItemUtils.createGuiItem(
                Material.ARROW,
                "§eСледующая страница",
                "§7Страница " + (currentPage + 1)
            );
            inv.setItem(53, nextItem);
        }
    }
    
    private static void setupInfo(Inventory inv, Player player, MonkMarket plugin) {
        ItemStack infoItem = ItemUtils.createGuiItem(
            Material.NETHER_STAR,
            "§6Информация",
            "§7Ник: §f" + player.getName(),
            "§7Баланс: §a" + String.format("%.2f", plugin.getEconomyManager().getBalance(player)),
            "§7Всего слотов: §a" + plugin.getMarketManager().getPlayerTotalPages(player.getUniqueId()),
            "",
            "§eЛКМ - Просмотр деталей",
            "§cShift+ПКМ - Снять с продажи"
        );
        inv.setItem(4, infoItem);
    }
    
    private static ItemStack createItemDisplay(MarketItem marketItem, Player player, MonkMarket plugin) {
        ItemStack item = marketItem.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }
        
        if (meta != null) {
            String displayName = plugin.getItemManager().getItemDisplayName(item);
            
            List<String> lore = new ArrayList<String>();
            lore.add("§7Цена за x1: §6" + String.format("%.2f", marketItem.getPricePerUnit()));
            lore.add("§7Продано: §a" + marketItem.getSoldAmount() + " из " + marketItem.getTotalAmount());
            lore.add("§7Доступно: §a" + marketItem.getAvailableAmount() + " шт.");
            
            if (marketItem.getCreatedAt() != null) {
                String dateStr = marketItem.getCreatedAt().toString();
                if (dateStr.length() > 16) {
                    dateStr = dateStr.substring(0, 16);
                }
                lore.add("§7Выставлен: §f" + dateStr);
            }
            
            lore.add("");
            lore.add("§eЛКМ - Просмотр деталей");
            lore.add("§cShift+ПКМ - Снять с продажи");
            
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
    
    public static boolean removeFromMarket(Player player, int itemId) {
        MonkMarket plugin = MonkMarket.getInstance();
        
        MarketItem marketItem = plugin.getMarketManager().getItemById(itemId);
        if (marketItem == null) {
            marketItem = plugin.getMarketManager().getItemQueries().getItemById(itemId);
            if (marketItem == null) {
                player.sendMessage("§cПредмет не найден!");
                return false;
            }
        }
        
        if (!marketItem.getSellerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§cЭто не ваш предмет!");
            return false;
        }
        
        int remaining = marketItem.getAvailableAmount();
        
        if (remaining > 0) {
            ItemStack item = marketItem.getItemStack();
            
            if (item == null) {
                try {
                    Material material = Material.getMaterial(marketItem.getItemMaterial());
                    if (material != null && material != Material.AIR) {
                        item = new ItemStack(material, 1);
                    } else {
                        player.sendMessage("§cОшибка: неизвестный тип предмета");
                        return false;
                    }
                } catch (Exception e) {
                    item = new ItemStack(Material.STONE, 1);
                }
            } else {
                item = item.clone();
            }
            
            item.setAmount(remaining);
            
            if (InventoryUtils.addToInventory(player, item, remaining)) {
                player.sendMessage("§aВам возвращено " + remaining + " предметов");
            } else {
                player.getWorld().dropItem(player.getLocation(), item);
                player.sendMessage("§aНедостаточно места! Предметы выпали на землю");
            }
        }
        
        boolean success = false;
        try {
            success = plugin.getMarketManager().getItemQueries().removeItem(itemId);
        } catch (Exception e) {
            player.sendMessage("§cОшибка базы данных!");
            return false;
        }
        
        if (success) {
            plugin.getMarketManager().clearItemCache(itemId);
            player.sendMessage("§aЛот успешно снят с продажи!");
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && player.getOpenInventory() != null) {
                    int currentPage = getPlayerPage(player.getUniqueId());
                    openMenu(player, currentPage);
                }
            });
            
            return true;
        } else {
            player.sendMessage("§cНе удалось снять лот с продажи!");
            return false;
        }
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
}