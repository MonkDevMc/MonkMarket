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

public class GUIManager {
    
    private final MonkMarket plugin;
    private final Map<UUID, Inventory> playerInventories;
    private final Map<UUID, Long> lastClickTime;
    
    public GUIManager(MonkMarket plugin) {
        this.plugin = plugin;
        this.playerInventories = new HashMap<UUID, Inventory>();
        this.lastClickTime = new HashMap<UUID, Long>();
    }
    
    public void openMarketGUI(Player player, int page) {
        closePlayerGUI(player);
        MarketGUI.openMainMenu(player, page);
        registerPlayerInventory(player, player.getOpenInventory().getTopInventory());
    }
    
    public void openBuyGUI(Player player, int itemId, double pricePerUnit) {
        closePlayerGUI(player);
        
        MarketItem marketItem = plugin.getMarketManager().getItemById(itemId);
        if (marketItem != null) {
            String title = plugin.getConfig().getString("gui.buy-menu.title", "&8Покупка");
            BuyGUIHolder holder = new BuyGUIHolder(itemId, pricePerUnit);
            Inventory inv = Bukkit.createInventory(holder, 27, StringUtils.colorize(title));
            holder.setInventory(inv);
            setupBuyGUI(inv, player, marketItem);
            player.openInventory(inv);
            registerPlayerInventory(player, inv);
        }
    }
    
    public void openMyItemsGUI(Player player, int page) {
        closePlayerGUI(player);
        MyItemsGUI.openMenu(player, page);
        registerPlayerInventory(player, player.getOpenInventory().getTopInventory());
    }
    
    private void setupBuyGUI(Inventory inv, Player player, MarketItem marketItem) {
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, ItemUtils.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<String>()));
        }
        
        ItemStack itemDisplay = marketItem.getItemStack().clone();
        ItemMeta meta = itemDisplay.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<String>();
            lore.add("§7Продавец: §f" + marketItem.getSellerName());
            lore.add("§7Цена за 1: §6" + String.format("%.2f", marketItem.getPricePerUnit()));
            lore.add("§7Доступно: §a" + marketItem.getAvailableAmount());
            lore.add("");
            lore.add("§eВыберите количество:");
            
            meta.setLore(lore);
            itemDisplay.setItemMeta(meta);
        }
        inv.setItem(13, itemDisplay);
        
        int[] amountSlots = {10, 11, 12, 14, 15, 16};
        int[] amounts = {1, 8, 16, 32, 64, marketItem.getAvailableAmount()};
        
        for (int i = 0; i < amountSlots.length; i++) {
            int amount = Math.min(amounts[i], marketItem.getAvailableAmount());
            if (amount > 0) {
                ItemStack amountItem = ItemUtils.createGuiItem(
                    Material.PAPER,
                    "§eКупить " + amount + " шт.",
                    "§7Цена: §6" + String.format("%.2f", marketItem.getPricePerUnit() * amount)
                );
                inv.setItem(amountSlots[i], amountItem);
            }
        }
        
        ItemStack confirmItem = ItemUtils.createGuiItem(
            Material.LIME_DYE,
            "§aПодтвердить покупку",
            "§7Нажмите для покупки"
        );
        inv.setItem(22, confirmItem);
    }
    
    public void refreshMarketGUI(Player player) {
        Inventory currentInv = player.getOpenInventory().getTopInventory();
        if (currentInv.getHolder() instanceof MarketHolder) {
            MarketHolder holder = (MarketHolder) currentInv.getHolder();
            
            switch (holder.getType()) {
                case MAIN:
                    int page = MarketGUI.getPlayerPage(player.getUniqueId());
                    openMarketGUI(player, page);
                    break;
                    
                case MY_ITEMS:
                    int myPage = MyItemsGUI.getPlayerPage(player.getUniqueId());
                    openMyItemsGUI(player, myPage);
                    break;
                    
                case BUY:
                    if (holder instanceof BuyGUIHolder) {
                        BuyGUIHolder buyHolder = (BuyGUIHolder) holder;
                        openBuyGUI(player, buyHolder.getItemId(), buyHolder.getPricePerUnit());
                    }
                    break;
            }
        }
    }
    
    public void closePlayerGUI(Player player) {
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        playerInventories.remove(player.getUniqueId());
        MarketGUI.removePlayerPage(player.getUniqueId());
    }
    
    public void registerPlayerInventory(Player player, Inventory inventory) {
        playerInventories.put(player.getUniqueId(), inventory);
    }
    
    public boolean hasOpenGUI(Player player) {
        return playerInventories.containsKey(player.getUniqueId());
    }
    
    public Inventory getPlayerInventory(Player player) {
        return playerInventories.get(player.getUniqueId());
    }
    
    public boolean canClick(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (lastClickTime.containsKey(uuid)) {
            long lastClick = lastClickTime.get(uuid);
            long delay = plugin.getConfig().getLong("security.anti-spam.commands-delay", 1000);
            
            if (currentTime - lastClick < delay) {
                return false;
            }
        }
        
        lastClickTime.put(uuid, currentTime);
        return true;
    }
    
    public void cleanup() {
        playerInventories.clear();
        lastClickTime.clear();
    }
}