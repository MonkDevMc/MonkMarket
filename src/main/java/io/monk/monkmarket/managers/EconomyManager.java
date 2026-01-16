package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    
    private final MonkMarket plugin;
    private Economy economy;
    
    public EconomyManager(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    public boolean hasEnough(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }
    
    public boolean hasEnough(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }
    
    public void withdraw(Player player, double amount) {
        if (economy != null) {
            economy.withdrawPlayer(player, amount);
        }
    }
    
    public void withdraw(OfflinePlayer player, double amount) {
        if (economy != null) {
            economy.withdrawPlayer(player, amount);
        }
    }
    
    public void deposit(Player player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }
    
    public void deposit(OfflinePlayer player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }
    
    public double getBalance(Player player) {
        if (economy == null) return 0.0;
        return economy.getBalance(player);
    }
    
    public double getBalance(OfflinePlayer player) {
        if (economy == null) return 0.0;
        return economy.getBalance(player);
    }
    
    public boolean isEconomyEnabled() {
        return economy != null;
    }
}