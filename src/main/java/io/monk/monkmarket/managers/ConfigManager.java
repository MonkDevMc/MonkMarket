package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {
    
    private final MonkMarket plugin;
    private FileConfiguration config;
    private FileConfiguration itemsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration soundsConfig;
    private File itemsFile;
    private File messagesFile;
    private File soundsFile;
    
    public ConfigManager(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    public void setupConfigs() {
        config = plugin.getConfig();
        
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        soundsFile = new File(plugin.getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            plugin.saveResource("sounds.yml", false);
        }
        soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);
    }
    
    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
            itemsConfig.save(itemsFile);
            messagesConfig.save(messagesFile);
            soundsConfig.save(soundsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка сохранения конфигурации: " + e.getMessage());
        }
    }
    
    public void reloadConfigs() {
        try {
            plugin.reloadConfig();
            config = plugin.getConfig();
        
            itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
            soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);
        
            plugin.getLogger().info("Конфигурация перезагружена:");
            plugin.getLogger().info("  - config.yml");
            plugin.getLogger().info("  - items.yml (" + itemsConfig.getKeys(false).size() + " предметов)");
            plugin.getLogger().info("  - messages.yml");
            plugin.getLogger().info("  - sounds.yml");
        
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при перезагрузке конфигурации: " + e.getMessage());
        }
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }
    
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    public FileConfiguration getSoundsConfig() {
        return soundsConfig;
    }
    
    public String getDatabaseType() {
        return config.getString("database.type", "SQLITE");
    }
    
    public boolean isPayFiveMMarketEnabled() {
        return config.getBoolean("notifications.pay-fivem-market.enabled", true);
    }
    
    public boolean isPayFiveMMessageEnabled() {
        return config.getBoolean("notifications.pay-fivem-market.message-enabled", true);
    }
    
    public String getPayFiveMMessage() {
        return config.getString("notifications.pay-fivem-market.message", "&7[&6Маркет&7] &fЧерез &e5 &fсекунд будут выданы деньги за проданные товары");
    }
    
    public int getPayFiveMDelay() {
        return config.getInt("notifications.pay-fivem-market.delay-seconds", 5);
    }
    
    public boolean isAlphabeticalSorting() {
        return config.getBoolean("sorting.alphabetically", true);
    }
    
    public boolean isLogToConsoleEnabled() {
        return config.getBoolean("economy.transactions.log-to-console", true);
    }
    
    public String getLogFormat() {
        return config.getString("economy.transactions.log-format", "[%time%] %player% купил %item%(%amount%x) за %price%");
    }
    
    public boolean isCustomBackgroundEnabled() {
        return config.getBoolean("gui.custom-background.enabled", false);
    }
    
    public String getCustomBackgroundTexture() {
        return config.getString("gui.custom-background.resourcepack-texture", "");
    }
    
    public boolean isRefreshButtonEnabled() {
        return config.getBoolean("gui.main-menu.refresh-button.enabled", true);
    }
    
    public int getRefreshButtonSlot() {
        int slot = config.getInt("gui.main-menu.refresh-button.slot", 48);
        return Math.max(0, Math.min(slot, 53));
    }
    
    public boolean isCheckInventorySpace() {
        return config.getBoolean("economy.item-delivery.check-inventory-space", true);
    }
    
    public boolean isCancelIfNoSpace() {
        return config.getBoolean("economy.item-delivery.cancel-if-no-space", true);
    }
    
    public String getNoSpaceMessage() {
        return config.getString("economy.item-delivery.no-space-message", "&cНедостаточно места в инвентаре для покупки!");
    }
    
    public boolean isCustomItemsSupport() {
        return config.getBoolean("items.custom-items-support", true);
    }
    
    public boolean isCheckNBT() {
        return config.getBoolean("items.validation.check-nbt", true);
    }
    
    public List<String> getBlacklistedItems() {
        List<String> blacklist = config.getStringList("items.validation.blacklist");
        return blacklist != null ? blacklist : new ArrayList<>();
    }
    
    public boolean isSoundEnabled(String soundType) {
        return config.getBoolean("notifications.sounds." + soundType + ".enabled", true);
    }
    
    public String getSound(String soundType) {
        return config.getString("notifications.sounds." + soundType + ".sound", "ENTITY_PLAYER_LEVELUP");
    }
    
    public float getSoundVolume(String soundType) {
        return (float) config.getDouble("notifications.sounds." + soundType + ".volume", 1.0);
    }
    
    public float getSoundPitch(String soundType) {
        return (float) config.getDouble("notifications.sounds." + soundType + ".pitch", 1.0);
    }
    
    public Map<String, String> getItemTranslations() {
        Map<String, String> translations = new HashMap<>();
        if (itemsConfig == null) return translations;
        
        for (String key : itemsConfig.getKeys(false)) {
            String value = itemsConfig.getString(key, "");
            if (value != null && value.contains(":")) {
                translations.put(key, value.split(":", 2)[0].trim());
            } else if (value != null) {
                translations.put(key, value.trim());
            }
        }
        return translations;
    }
    
    public String getItemDisplayName(String materialName) {
        if (materialName == null || itemsConfig == null) {
            return "";
        }
        String translation = itemsConfig.getString(materialName, "");
        if (translation == null) return "";
        
        if (translation.contains(":")) {
            return translation.split(":", 2)[0].trim();
        }
        return translation.trim();
    }

    public List<String> getItemTags(String materialName) {
        List<String> tags = new ArrayList<>();
        if (materialName == null || itemsConfig == null) return tags;
        
        String value = itemsConfig.getString(materialName, "");
        if (value == null) return tags;
        
        if (value.contains(":")) {
            String[] parts = value.split(":");
            for (int i = 1; i < parts.length; i++) {
                tags.add(parts[i].trim());
            }
        }
        
        return tags;
    }

    public Map<String, List<String>> getAllItemTranslationsWithTags() {
        Map<String, List<String>> allItems = new HashMap<>();
        if (itemsConfig == null) return allItems;
        
        for (String key : itemsConfig.getKeys(false)) {
            allItems.put(key, getItemTags(key));
        }
        
        return allItems;
    }
    
    public String getMessage(String path) {
        if (path == null) return "";
        
        String message = messagesConfig.getString(path, "");
        if (message == null || message.isEmpty()) {
            message = config.getString("messages." + path, "");
        }
        
        return message != null ? message : "";
    }
}

//by t.me/M0nkAdapter
//Hello SharpLeak And AkyRayy 