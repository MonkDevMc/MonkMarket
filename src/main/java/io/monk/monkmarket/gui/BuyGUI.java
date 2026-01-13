package io.monk.monkmarket.gui;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.MarketItem;
import io.monk.monkmarket.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class BuyGUI {
    
    private static final Map<UUID, Integer> selectedAmounts = new HashMap<>();
    private static final Map<UUID, Long> lastCleanup = new HashMap<>();
    private static final long CLEANUP_INTERVAL = 300000;
    
    public static void openAdvancedBuyMenu(Player player, MarketItem marketItem, List<MarketItem> similarItems) {
        MonkMarket plugin = MonkMarket.getInstance();
        
        String itemName = plugin.getItemManager().getItemDisplayName(marketItem.getItemStack());
        
        BuyGUIHolder holder = new BuyGUIHolder(marketItem.getId(), marketItem.getPricePerUnit());
        Inventory inv = Bukkit.createInventory(holder, 54, "§8Покупка: " + itemName);
        holder.setInventory(inv);
        
        setupAdvancedBuyMenu(inv, player, marketItem, similarItems);
        selectedAmounts.put(player.getUniqueId(), 1);
        player.openInventory(inv);
        
        cleanupOldEntries();
    }
    
    private static void setupAdvancedBuyMenu(Inventory inv, Player player, MarketItem mainItem, List<MarketItem> similarItems) {
        setupBackground(inv);
        setupHeader(inv, mainItem);
        setupAmountSelection(inv, player, mainItem);
        setupCalculation(inv, player, mainItem, similarItems);
        setupActionButtons(inv);
    }
    
    private static void setupBackground(Inventory inv) {
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<>()));
            }
        }
    }
    
    private static void setupHeader(Inventory inv, MarketItem mainItem) {
        MonkMarket plugin = MonkMarket.getInstance();
        ItemStack item = mainItem.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Цена: §6" + String.format("%.2f", mainItem.getPricePerUnit()));
            lore.add("§7Есть: §a" + mainItem.getAvailableAmount() + " шт.");
            
            meta.setLore(lore);
            meta.setDisplayName("§f" + plugin.getItemManager().getItemDisplayName(item));
            item.setItemMeta(meta);
        }
        inv.setItem(22, item);
    }
    
    private static void setupAmountSelection(Inventory inv, Player player, MarketItem mainItem) {
        inv.setItem(31, ItemUtils.createGuiItem(Material.EXPERIENCE_BOTTLE, "§6Выбор количества", 
            "§7Выберите сколько купить"));
        
        ItemStack minus1 = ItemUtils.createGuiItem(Material.ARROW, "§c-1 шт.", 
            "§7Уменьшить на 1", 
            "§7ЛКМ: -1",
            "§7Shift+ЛКМ: -16");
        inv.setItem(30, minus1);
        
        ItemStack plus1 = ItemUtils.createGuiItem(Material.ARROW, "§a+1 шт.", 
            "§7Увеличить на 1",
            "§7ЛКМ: +1",
            "§7Shift+ЛКМ: +16");
        inv.setItem(32, plus1);
        
        int currentAmount = selectedAmounts.getOrDefault(player.getUniqueId(), 1);
        currentAmount = Math.min(currentAmount, 64);
        currentAmount = Math.min(currentAmount, mainItem.getAvailableAmount());
        currentAmount = Math.max(1, currentAmount);
        
        selectedAmounts.put(player.getUniqueId(), currentAmount);
        
        int availableAmount = mainItem.getAvailableAmount();
        int remainingAmount = availableAmount - currentAmount;
        double pricePerUnit = mainItem.getPricePerUnit();
        double totalPrice = currentAmount * pricePerUnit;
        String formattedTotalPrice = String.format("%.2f", totalPrice);

        ItemStack amountDisplay = ItemUtils.createGuiItem(
            Material.BOOK, 
            "§eВыбрано: " + currentAmount,
            "§7Ещё есть: " + Math.max(0, remainingAmount),
            "",
            "§7Цена: §6" + formattedTotalPrice
        );
        inv.setItem(40, amountDisplay);
        
        int available = mainItem.getAvailableAmount();
        int halfAmount;

        if (available <= 1) {
            halfAmount = 1;
        } else if (available % 2 == 0) {
            halfAmount = available / 2;
        } else {
            halfAmount = (available / 2) + 1;
        }
        
        halfAmount = Math.min(halfAmount, 64);

        double halfPrice = mainItem.getPricePerUnit() * halfAmount;
        String formattedHalfPrice = String.format("%.2f", halfPrice);

        ItemStack buyHalf = ItemUtils.createGuiItem(Material.GOLD_BLOCK, "§6Купить половину", 
            "§7Купить половину",
            "§7Количество: " + halfAmount,
            "§7Цена: §6" + formattedHalfPrice);
        inv.setItem(29, buyHalf);
        
        int allAmount = Math.min(mainItem.getAvailableAmount(), 64);
        double allPrice = mainItem.getPricePerUnit() * allAmount;
        String formattedAllPrice = String.format("%.2f", allPrice);

        ItemStack buyAll = ItemUtils.createGuiItem(Material.EMERALD, "§6Купить всё", 
            "§7Купить всё",
            "§7Количество: " + allAmount,
            "§7Цена: §6" + formattedAllPrice);
        inv.setItem(33, buyAll);
    }
    
    private static void setupCalculation(Inventory inv, Player player, MarketItem mainItem, List<MarketItem> similarItems) {
        int wantedAmount = selectedAmounts.getOrDefault(player.getUniqueId(), 1);
        wantedAmount = Math.min(wantedAmount, 64);
        
        List<String> lore = new ArrayList<>();
        double totalPrice = 0;
        int totalGot = 0;
        int stillNeed = wantedAmount;
        
        lore.add("§7Запрошено: §a" + wantedAmount + " шт.");
        lore.add("");
        
        int fromMain = Math.min(stillNeed, mainItem.getAvailableAmount());
        if (fromMain > 0) {
            double priceForThis = fromMain * mainItem.getPricePerUnit();
            lore.add("§7" + fromMain + "x §6" + String.format("%.2f", mainItem.getPricePerUnit()) + 
                    " = §6" + String.format("%.2f", priceForThis));
            totalPrice += priceForThis;
            totalGot += fromMain;
            stillNeed -= fromMain;
        }
        
        if (stillNeed > 0 && similarItems != null && !similarItems.isEmpty()) {
            List<MarketItem> sortedSimilar = new ArrayList<>(similarItems);
            sortedSimilar.sort(Comparator.comparingDouble(MarketItem::getPricePerUnit));
            
            for (MarketItem similar : sortedSimilar) {
                if (stillNeed <= 0) break;
                
                int fromThis = Math.min(stillNeed, similar.getAvailableAmount());
                if (fromThis > 0) {
                    double priceForThis = fromThis * similar.getPricePerUnit();
                    lore.add("§7" + fromThis + "x §6" + String.format("%.2f", similar.getPricePerUnit()) + 
                            " = §6" + String.format("%.2f", priceForThis));
                    totalPrice += priceForThis;
                    totalGot += fromThis;
                    stillNeed -= fromThis;
                }
            }
        }
        
        lore.add("");
        
        if (stillNeed > 0) {
            lore.add("§cНедостаточно предметов на маркете: " + stillNeed + " шт.");
            lore.add("§cУменьшите количество");
        }
        
        lore.add("§7Всего найдено: §a" + totalGot + " шт.");
        lore.add("§6Итог: " + String.format("%.2f", totalPrice));
        
        MonkMarket plugin = MonkMarket.getInstance();
        double balance = plugin.getEconomyManager().getBalance(player);
        lore.add("§7Баланс: " + (balance >= totalPrice ? "§a" : "§c") + String.format("%.2f", balance));
        lore.add(balance >= totalPrice ? "§aХватит" : "§cНе хватит");
        
        ItemStack calculationItem = ItemUtils.createGuiItem(Material.GOLD_INGOT, "§6Расчет", lore);
        inv.setItem(49, calculationItem);
    }
    
    private static void setupActionButtons(Inventory inv) {
        ItemStack confirmItem = ItemUtils.createGuiItem(Material.EMERALD_BLOCK, "§aКупить",
            "§7Подтвердить покупку");
        inv.setItem(48, confirmItem);
        
        ItemStack cancelItem = ItemUtils.createGuiItem(Material.REDSTONE_BLOCK, "§cОтмена",
            "§7Отменить покупку");
        inv.setItem(50, cancelItem);
    }
    
    public static void updateSelectedAmount(Player player, int amount) {
        amount = Math.max(1, Math.min(amount, 64));
        selectedAmounts.put(player.getUniqueId(), amount);
    }
    
    public static int getSelectedAmount(UUID uuid) {
        return selectedAmounts.getOrDefault(uuid, 1);
    }
    
    public static void clearSelection(UUID uuid) {
        selectedAmounts.remove(uuid);
    }
    
    public static void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        Long lastCleanupTime = lastCleanup.getOrDefault(null, 0L);
        
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            Set<UUID> toRemove = new HashSet<>();
            for (UUID uuid : selectedAmounts.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    toRemove.add(uuid);
                }
            }
            
            for (UUID uuid : toRemove) {
                selectedAmounts.remove(uuid);
            }
            
            lastCleanup.put(null, currentTime);
        }
    }
    
    public static void clearAllSelections() {
        selectedAmounts.clear();
        lastCleanup.clear();
    }
}