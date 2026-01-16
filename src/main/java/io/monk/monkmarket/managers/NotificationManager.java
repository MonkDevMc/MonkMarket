package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.HashMap;

public class NotificationManager {
    
    private final MonkMarket plugin;
    
    public NotificationManager(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    public void sendPurchaseNotification(Player buyer, String itemName, int amount, double totalPrice, String sellerName) {
        if (!plugin.getConfig().getBoolean("notifications.purchase-enabled", true)) {
            return;
        }
        
        String message = plugin.getConfigManager().getMessage("purchase-message");
        if (message.isEmpty()) {
            message = "&aВы приобрели %item%(&ex%amount%) &aза &6%total_price% &aу &e%seller%";
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%item%", itemName);
        placeholders.put("%amount%", String.valueOf(amount));
        placeholders.put("%total_price%", String.format("%.2f", totalPrice));
        placeholders.put("%seller%", sellerName);
        
        String finalMessage = replacePlaceholders(message, placeholders);
        buyer.sendMessage(finalMessage.replace("&", "§"));
    }
    
    public void sendSaleNotification(Player seller, String itemName, int amount, double totalPrice) {
        String message = plugin.getConfigManager().getMessage("sale-notification");
        if (message.isEmpty()) {
            message = "&aВаш предмет &e%item%(&ex%amount%) &aпродан за &6%price%";
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%item%", itemName);
        placeholders.put("%amount%", String.valueOf(amount));
        placeholders.put("%price%", String.format("%.2f", totalPrice));
        
        String finalMessage = replacePlaceholders(message, placeholders);
        seller.sendMessage(finalMessage.replace("&", "§"));
    }
    
    public void sendNoSpaceNotification(Player player) {
        String message = plugin.getConfigManager().getNoSpaceMessage();
        player.sendMessage(message.replace("&", "§"));
    }
    
    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }
}