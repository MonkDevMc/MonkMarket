package io.monk.monkmarket.checkUpdates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final JavaPlugin plugin;
    private final String GITHUB_API_URL = "https://api.github.com/repos/MonkDevMc/MonkMarket/releases/latest";
    private String latestVersion = null;
    private String downloadUrl = null;
    private boolean updateAvailable = false;
    
    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void checkForUpdates(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                JsonObject releaseData = getReleaseData(GITHUB_API_URL);
                
                if (releaseData != null) {
                    latestVersion = releaseData.get("tag_name").getAsString();
                    downloadUrl = releaseData.get("html_url").getAsString();
                    
                    String currentVersion = plugin.getDescription().getVersion();
                    
                    if (!latestVersion.equals(currentVersion)) {
                        updateAvailable = true;
                        
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (sender != null) {
                                sender.sendMessage("§6═══════════════════════════════════════");
                                sender.sendMessage("§6Доступно обновление!");
                                sender.sendMessage("§6Текущая версия: §f" + currentVersion);
                                sender.sendMessage("§6Новая версия: §a" + latestVersion);
                                sender.sendMessage("§6Скачать: §f" + downloadUrl);
                                sender.sendMessage("§6═══════════════════════════════════════");
                            } else {
                                plugin.getLogger().warning("═══════════════════════════════════════");
                                plugin.getLogger().warning("Доступно обновление!");
                                plugin.getLogger().warning("Текущая версия: " + currentVersion);
                                plugin.getLogger().warning("Новая версия: " + latestVersion);
                                plugin.getLogger().warning("Скачать: " + downloadUrl);
                                plugin.getLogger().warning("═══════════════════════════════════════");
                            }
                        });
                    } else {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (sender != null) {
                                sender.sendMessage("§aПлагин обновлен до последней версии.");
                            }
                            else {
                                plugin.getLogger().info("§cПлагин имеет последнюю версию");
                            }
                        });
                    }
                }
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (sender != null) {
                        sender.sendMessage("§cОшибка при проверке обновлений :(");
                    }
                    else {
                        plugin.getLogger().warning("§cОшибка при проверке обновлений :(");
                    }
                });
            }
        });
    }
    
    private JsonObject getReleaseData(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return JsonParser.parseString(response.toString()).getAsJsonObject();
    }
    
    public String getUpdateMessage() {
        if (updateAvailable) {
            return "§6═══════════════════════════════════════\n" +
                   "§6Доступно обновление §e" + plugin.getDescription().getName() + "§6!\n" +
                   "§6Текущая версия: §f" + plugin.getDescription().getVersion() + "\n" +
                   "§6Новая версия: §a" + latestVersion + "\n" +
                   "§6Скачать: §f" + downloadUrl + "\n" +
                   "§6═══════════════════════════════════════";
        }
        return null;
    }
}