package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.Transaction;
import io.monk.monkmarket.database.queries.TransactionQueries;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public class TransactionManager {
    
    private final MonkMarket plugin;
    private final TransactionQueries transactionQueries;
    private final List<Transaction> pendingTransactions;
    
    public TransactionManager(MonkMarket plugin) {
        this.plugin = plugin;
        this.transactionQueries = new TransactionQueries(plugin);
        this.pendingTransactions = new ArrayList<>();
    }
    
    public void addPendingTransaction(Transaction transaction) {
        pendingTransactions.add(transaction);
    }
    
    public void processPendingTransactions() {
        if (plugin.getConfigManager().isPayFiveMMarketEnabled() && plugin.isEnabled()) {
            int delay = plugin.getConfigManager().getPayFiveMDelay();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.isEnabled()) {
                    String message = plugin.getConfigManager().getPayFiveMMessage();
                    Bukkit.broadcastMessage(message.replace("&", "§"));
                }
            }, delay * 20L);
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<Transaction> transactions = transactionQueries.getUnpaidTransactions();
            double totalPaid = 0;
            int count = 0;
            
            for (Transaction transaction : transactions) {
                Player seller = Bukkit.getPlayer(transaction.getSellerUuid());
                if (seller != null && seller.isOnline()) {
                    plugin.getEconomyManager().deposit(seller, transaction.getTotalPrice());
                    transactionQueries.markAsPaid(transaction.getId());
                    totalPaid += transaction.getTotalPrice();
                    count++;
                    
                    String message = plugin.getConfigManager().getMessage("sale-notification")
                        .replace("%price%", String.format("%.2f", transaction.getTotalPrice()))
                        .replace("%amount%", String.valueOf(transaction.getItemAmount()))
                        .replace("%item%", transaction.getItemMaterial());
                    
                    seller.sendMessage(message.replace("&", "§"));
                }
            }
            
            if (count > 0 && plugin.getConfigManager().isLogToConsoleEnabled()) {
                String logMessage = plugin.getConfigManager().getLogFormat()
                    .replace("%time%", new Date().toString())
                    .replace("%count%", String.valueOf(count))
                    .replace("%total%", String.format("%.2f", totalPaid));
                
                plugin.getLogger().info(logMessage);
            }
            
        }, (plugin.getConfigManager().getPayFiveMDelay() + 5) * 20L);
    }
    
    public List<Transaction> getPendingTransactions() {
        return new ArrayList<>(pendingTransactions);
    }
    
    public void clearPendingTransactions() {
        pendingTransactions.clear();
    }
}