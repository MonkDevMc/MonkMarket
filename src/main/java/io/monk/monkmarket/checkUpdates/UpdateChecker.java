package io.monk.monkmarket.checkUpdates;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final JavaPlugin plugin;
    private final String VERSION_URL = "https://raw.githubusercontent.com/MonkDevMc/MonkMarket/main/VERSION.txt";
    private String latestVersion = null;
    private boolean updateAvailable = false;
    
    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void checkForUpdates(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                latestVersion = getLatestVersion();
                
                if (latestVersion != null) {
                    String currentVersion = plugin.getDescription().getVersion();
                    updateAvailable = !latestVersion.equals(currentVersion);
                    
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        showUpdateMessage(sender);
                    });
                }
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (sender != null) {
                        sender.sendMessage("§cНе удалось проверить обновления");
                    }
                });
            }
        });
    }
    
    private String getLatestVersion() throws Exception {
        URL url = new URL(VERSION_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        if (connection.getResponseCode() != 200) {
            return null;
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String version = reader.readLine();
        reader.close();
        
        return version != null ? version.trim() : null;
    }
    
    private void showUpdateMessage(CommandSender sender) {
        String currentVersion = plugin.getDescription().getVersion();
        
        if (updateAvailable) {
            if (sender != null) {
                sender.sendMessage("§6═══════════════════════════════════════");
                sender.sendMessage("§6Доступно обновление!");
                sender.sendMessage("§6Текущая версия: §f" + currentVersion);
                sender.sendMessage("§6Новая версия: §a" + latestVersion);
                sender.sendMessage("§6Скачать: https://github.com/MonkDevMc/MonkMarket/releases");
                sender.sendMessage("§6═══════════════════════════════════════");
            } else {
                plugin.getLogger().warning("═══════════════════════════════════════");
                plugin.getLogger().warning("Доступно обновление!");
                plugin.getLogger().warning("Текущая версия: " + currentVersion);
                plugin.getLogger().warning("Новая версия: " + latestVersion);
                plugin.getLogger().warning("Скачать: https://github.com/MonkDevMc/MonkMarket/releases");
                plugin.getLogger().warning("═══════════════════════════════════════");
            }
        } else {
            if (sender != null) {
                sender.sendMessage("§aПлагин обновлен до последней версии.");
            } else {
                plugin.getLogger().info("§aПлагин имеет последнюю версию");
            }
        }
    }
}