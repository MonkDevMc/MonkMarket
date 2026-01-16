package io.monk.monkmarket.listeners;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.MarketItem;
import io.monk.monkmarket.gui.*;
import io.monk.monkmarket.managers.ValidationManager;
import io.monk.monkmarket.utils.InventoryUtils;
import io.monk.monkmarket.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class MarketListener implements Listener {
    
    private final MonkMarket plugin;
    private final Map<UUID, Long> lastCalculation = new HashMap<>();
    
    public MarketListener(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        boolean isMarketGUI = inventory.getHolder() instanceof MarketHolder;
        
        if (!isMarketGUI) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!plugin.getGUIManager().canClick(player)) {
            return;
        }
        
        int slot = event.getSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;
        
        plugin.getSoundManager().playClickSound(player);
        
        Object holder = inventory.getHolder();
        
        if (holder instanceof BuyGUIHolder) {
            handleBuyGUIClick(player, inventory, slot, clickedItem, event.getClick(), (BuyGUIHolder) holder);
            return;
        }
        
        if (holder instanceof MarketHolder) {
            MarketHolder marketHolder = (MarketHolder) holder;
            
            switch (marketHolder.getType()) {
                case MAIN:
                    handleMainMenuClick(player, inventory, slot, clickedItem, event.getClick());
                    break;
                    
                case MY_ITEMS:
                    handleMyItemsClick(player, inventory, slot, clickedItem, event.getClick());
                    break;
                    
                default:
                    break;
            }
        }
    }
    
    private void handleBuyGUIClick(Player player, Inventory inventory, int slot, ItemStack clickedItem, ClickType clickType, BuyGUIHolder buyHolder) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int itemId = buyHolder.getItemId();
        MarketItem marketItem = plugin.getMarketManager().getItemById(itemId);
        if (marketItem == null) {
            player.sendMessage("§cПредмет не найден!");
            player.closeInventory();
            return;
        }
        
        int currentAmount = BuyGUI.getSelectedAmount(player.getUniqueId());
        
        if (slot == 30 && clickedItem.getType() == Material.ARROW) {
            int newAmount = currentAmount - 1;
            if (newAmount < 1) newAmount = 1;
            if (newAmount > marketItem.getAvailableAmount()) {
                newAmount = marketItem.getAvailableAmount();
            }
            BuyGUI.updateSelectedAmount(player, newAmount);
            updateBuyGUI(player, inventory, marketItem);
        }
        else if (slot == 32 && clickedItem.getType() == Material.ARROW) {
            int newAmount = currentAmount + 1;
            if (newAmount > 64) newAmount = 64;
            if (newAmount > marketItem.getAvailableAmount()) {
                newAmount = marketItem.getAvailableAmount();
            }
            BuyGUI.updateSelectedAmount(player, newAmount);
            updateBuyGUI(player, inventory, marketItem);
        }
        else if (slot == 29 && clickedItem.getType() == Material.GOLD_BLOCK) {
            int available = marketItem.getAvailableAmount();
            int halfAmount;
            if (available <= 1) {
                halfAmount = 1;
            } else if (available % 2 == 0) {
                halfAmount = available / 2;
            } else {
                halfAmount = (available / 2) + 1;
            }
            halfAmount = Math.min(halfAmount, 64);
            BuyGUI.updateSelectedAmount(player, halfAmount);
            updateBuyGUI(player, inventory, marketItem);
        }
        else if (slot == 33 && clickedItem.getType() == Material.EMERALD) {
            int allAmount = Math.min(marketItem.getAvailableAmount(), 64);
            BuyGUI.updateSelectedAmount(player, allAmount);
            updateBuyGUI(player, inventory, marketItem);
        }
        else if (slot == 48 && clickedItem.getType() == Material.EMERALD_BLOCK) {
            long now = System.currentTimeMillis();
            if (lastCalculation.containsKey(player.getUniqueId())) {
                long lastClick = lastCalculation.get(player.getUniqueId());
                if (now - lastClick < 3000) {
                    player.sendMessage("§cПодождите немного перед следующей покупкой!");
                    return;
                }
            }
            
            int amount = BuyGUI.getSelectedAmount(player.getUniqueId());
            if (amount < 1) amount = 1;
            if (amount > marketItem.getAvailableAmount()) {
                amount = marketItem.getAvailableAmount();
            }
            
            startPurchaseAnimation(player, inventory, marketItem, amount);
            lastCalculation.put(player.getUniqueId(), now);
        }
        else if (slot == 50 && clickedItem.getType() == Material.REDSTONE_BLOCK) {
            int page = MarketGUI.getPlayerPage(player.getUniqueId());
            MarketGUI.openMainMenu(player, page);
        }
    }
    
    private void startPurchaseAnimation(Player player, Inventory inventory, MarketItem marketItem, int amount) {
        showCalculationAnimation(inventory, "§6Расчёт...", marketItem, amount, player);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !inventory.equals(player.getOpenInventory().getTopInventory())) {
                return;
            }
            
            ValidationManager.ValidationResult result = plugin.getValidationManager().validatePurchase(player, marketItem.getId(), amount);
            
            if (!result.isSuccess()) {
                String message = plugin.getConfigManager().getMessage(result.getMessage());
                if (!message.isEmpty()) {
                    player.sendMessage(message.replace("&", "§"));
                }
                showCalculationAnimation(inventory, "§cОшибка!", marketItem, amount, player);
                return;
            }
            
            if (!InventoryUtils.hasInventorySpace(player, marketItem.getItemStack(), amount)) {
                player.sendMessage(plugin.getConfigManager().getNoSpaceMessage().replace("&", "§"));
                showCalculationAnimation(inventory, "§cНет места!", marketItem, amount, player);
                return;
            }
            
            showCalculationAnimation(inventory, "§aПокупка...", marketItem, amount, player);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || !inventory.equals(player.getOpenInventory().getTopInventory())) {
                    return;
                }
                
                boolean purchaseSuccess = plugin.getMarketManager().purchaseItem(player, marketItem.getId(), amount);
                
                if (purchaseSuccess) {
                    ItemStack itemToGive = marketItem.getItemStack().clone();
                    itemToGive.setAmount(amount);
                    
                    HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(itemToGive);
                    if (!leftovers.isEmpty()) {
                        for (ItemStack leftover : leftovers.values()) {
                            player.getWorld().dropItem(player.getLocation(), leftover);
                        }
                    }
                    
                    String itemName = plugin.getItemManager().getItemDisplayName(marketItem.getItemStack());
                    plugin.getNotificationManager().sendPurchaseNotification(player, itemName, amount, result.getTotalPrice(), marketItem.getSellerName());
                    plugin.getSoundManager().playPurchaseSound(player);
                    
                    showCalculationAnimation(inventory, "§aУспешно!", marketItem, amount, player);
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.closeInventory();
                            player.sendMessage("§aПокупка успешна! Предметы добавлены в инвентарь.");
                            
                            int page = MarketGUI.getPlayerPage(player.getUniqueId());
                            MarketGUI.openMainMenu(player, page);
                        }
                    }, 20L);
                } else {
                    showCalculationAnimation(inventory, "§cОшибка!", marketItem, amount, player);
                    player.sendMessage("§cНе удалось завершить покупку!");
                }
            }, 20L);
            
        }, 40L);
    }
    
    private void showCalculationAnimation(Inventory inventory, String status, MarketItem marketItem, int amount, Player player) {
        List<String> lore = new ArrayList<>();
        double totalPrice = marketItem.getPricePerUnit() * amount;
        
        lore.add("§7Количество: §a" + amount + "x");
        lore.add("§7Цена за шт: §6" + String.format("%.2f", marketItem.getPricePerUnit()));
        lore.add("§7Итого: §6" + String.format("%.2f", totalPrice));
        lore.add("");
        lore.add(status);
        
        ItemStack calculationItem = ItemUtils.createGuiItem(Material.GOLD_INGOT, "§6" + status, lore);
        inventory.setItem(49, calculationItem);
    }
    
    private void updateBuyGUI(Player player, Inventory inventory, MarketItem marketItem) {
        int currentAmount = BuyGUI.getSelectedAmount(player.getUniqueId());
        int availableAmount = marketItem.getAvailableAmount();
        int remainingAmount = availableAmount - currentAmount;
        double pricePerUnit = marketItem.getPricePerUnit();
        double totalPrice = currentAmount * pricePerUnit;
        String formattedTotalPrice = String.format("%.2f", totalPrice);

        ItemStack amountDisplay = ItemUtils.createGuiItem(
            Material.BOOK, 
            "§eВыбрано: " + currentAmount,
            "§7Ещё есть: " + Math.max(0, remainingAmount),
            "",
            "§7Цена: §6" + formattedTotalPrice
        );
        inventory.setItem(40, amountDisplay);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7" + currentAmount + "x " + String.format("%.2f", pricePerUnit));
        lore.add("");
        lore.add("§6Итого: " + formattedTotalPrice);
        
        MonkMarket plugin = MonkMarket.getInstance();
        double balance = plugin.getEconomyManager().getBalance(player);
        lore.add("§7Баланс: " + (balance >= totalPrice ? "§a" : "§c") + String.format("%.2f", balance));
        lore.add(balance >= totalPrice ? "§aХватит" : "§cНе хватит");
        
        ItemStack calculationItem = ItemUtils.createGuiItem(
            Material.GOLD_INGOT,
            "§6Расчёт...",
            lore
        );
        inventory.setItem(49, calculationItem);
    }

    private void handleMainMenuClick(Player player, Inventory inventory, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            MyItemsGUI.openMenu(player, 1);
            return;
        }
        
        if (slot == plugin.getConfigManager().getRefreshButtonSlot()) {
            MarketGUI.refreshForPlayer(player);
            player.sendMessage("§aСписок товаров обновлен!");
            return;
        }
        
        if (slot == 45) {
            int currentPage = MarketGUI.getPlayerPage(player.getUniqueId());
            if (currentPage > 1) {
                MarketGUI.openMainMenu(player, currentPage - 1);
                return;
            }
        }

        if (slot == 53) {
            int currentPage = MarketGUI.getPlayerPage(player.getUniqueId());
            String searchQuery = plugin.getMarketManager().getPlayerSearch(player.getUniqueId());
            int totalPages = plugin.getMarketManager().getTotalPages(searchQuery);
    
            if (currentPage < totalPages) {
                MarketGUI.openMainMenu(player, currentPage + 1);
                return;
            }
        }
        
        if (slot >= 9 && slot < 45) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null) return;
            
            int itemId = meta.getCustomModelData();
            if (itemId <= 0) {
                plugin.getLogger().warning("[Market] CustomModelData не найден для предмета. Использую fallback.");
                
                int itemIndex = slot - 9;
                int page = MarketGUI.getPlayerPage(player.getUniqueId());
                String searchQuery = plugin.getMarketManager().getPlayerSearch(player.getUniqueId());
                List<MarketItem> items = plugin.getMarketManager().getMarketItems(page, searchQuery);
                
                if (itemIndex < items.size()) {
                    MarketItem marketItem = items.get(itemIndex);
                    itemId = marketItem.getId();
                } else {
                    player.sendMessage("§cПредмет не найден!");
                    return;
                }
            }
            
            MarketItem marketItem = plugin.getMarketManager().getItemById(itemId);
            if (marketItem == null) {
                player.sendMessage("§cПредмет не найден в базе данных!");
                return;
            }
            
            List<MarketItem> similarItems = plugin.getMarketManager().getSimilarItems(marketItem.getItemMaterial(), marketItem.getId());
            BuyGUI.openAdvancedBuyMenu(player, marketItem, similarItems);
        }
    }
    
    private void handleMyItemsClick(Player player, Inventory inventory, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 45) {
            int currentPage = MyItemsGUI.getPlayerPage(player.getUniqueId());
            if (currentPage > 1) {
                MyItemsGUI.openMenu(player, currentPage - 1);
                return;
            }
        }

        if (slot == 53) {
            int currentPage = MyItemsGUI.getPlayerPage(player.getUniqueId());
            int totalPages = plugin.getMarketManager().getPlayerTotalPages(player.getUniqueId());
            if (currentPage < totalPages) {
                MyItemsGUI.openMenu(player, currentPage + 1);
                return;
            }
        }
        
        if (slot >= 9 && slot < 45) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null) return;
            
            int itemId = meta.getCustomModelData();
            if (itemId <= 0) {
                int itemIndex = slot - 9;
                int page = MyItemsGUI.getPlayerPage(player.getUniqueId());
                List<MarketItem> items = plugin.getMarketManager().getPlayerItems(player.getUniqueId(), page);
                
                if (itemIndex < items.size()) {
                    MarketItem marketItem = items.get(itemIndex);
                    itemId = marketItem.getId();
                } else {
                    return;
                }
            }
            
            MarketItem marketItem = plugin.getMarketManager().getItemById(itemId);
            if (marketItem == null) return;
            
            if (clickType == ClickType.SHIFT_RIGHT) {
                boolean success = MyItemsGUI.removeFromMarket(player, marketItem.getId());
                if (success) {
                    player.sendMessage("§aЛот снят с продажи!");
                    int currentPage = MyItemsGUI.getPlayerPage(player.getUniqueId());
                    MyItemsGUI.openMenu(player, currentPage);
                }
                return;
            }
            
            if (clickType == ClickType.LEFT) {
                player.sendMessage("§6Цена за x1: §f" + String.format("%.2f", marketItem.getPricePerUnit()));
                player.sendMessage("§6Уже Продано: §f" + marketItem.getSoldAmount() + "/" + marketItem.getTotalAmount());
                player.sendMessage("§6Ещё Не Продано: §f" + marketItem.getAvailableAmount() + " шт.");
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        if (inventory.getHolder() instanceof MarketHolder) {
            MarketGUI.removePlayerPage(player.getUniqueId());
            plugin.getMarketManager().clearPlayerSearch(player.getUniqueId());
            lastCalculation.remove(player.getUniqueId());
        }
    }
    
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        if (inventory.getHolder() instanceof MarketHolder) {
            if (plugin.getConfigManager().isSoundEnabled("open")) {
                plugin.getSoundManager().playClickSound(player);
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isDatabaseConnected()) {
            event.getPlayer().sendMessage("§cМаркет не работает!");
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getGUIManager().closePlayerGUI(player);
        plugin.getMarketManager().clearPlayerSearch(player.getUniqueId());
        lastCalculation.remove(player.getUniqueId());
    }
}