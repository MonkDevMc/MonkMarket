package io.monk.monkmarket;

import io.monk.monkmarket.commands.MarketCommand;
import io.monk.monkmarket.commands.MarketTabCompleter;
import io.monk.monkmarket.checkUpdates.UpdateChecker; // импорт
import io.monk.monkmarket.gui.BuyGUI;
import io.monk.monkmarket.gui.GUIManager;
import io.monk.monkmarket.gui.MarketGUI;
import io.monk.monkmarket.gui.MyItemsGUI;
import io.monk.monkmarket.listeners.MarketListener;
import io.monk.monkmarket.managers.*;
import io.monk.monkmarket.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class MonkMarket extends JavaPlugin {
    
    private static MonkMarket instance;
    private MarketManager marketManager;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private EconomyManager economyManager;
    private TransactionManager transactionManager;
    private ItemManager itemManager;
    private NotificationManager notificationManager;
    private SoundManager soundManager;
    private SortingManager sortingManager;
    private ValidationManager validationManager;
    private LogManager logManager;
    private GUIManager guiManager;
    private InventoryUtils inventoryUtils;
    private UpdateChecker updateChecker; // переменная
    private Logger logger;
    private boolean databaseConnected = false;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        logger.info("§a=======================================");
        logger.info("§b          MonkMarket");
        logger.info("§f            v" + getDescription().getVersion());
        logger.info("§b         by &l@M0nkDev ");
        logger.info("§bUpdateLog  -->");
        logger.info("§c   &lt.me/MonkPlUpdates");
        logger.info("§b    Приятного использования :0");
        logger.info("§a=======================================");
        
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        saveDefaultConfig();

        File itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
        }

        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        File soundsFile = new File(getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            saveResource("sounds.yml", false);
        }
        
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.severe("§cVault не Установлен! Плагин отключен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            logger.severe("§cPlaceholderAPI не Установлен! Плагин отключен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        configManager = new ConfigManager(this);
        configManager.setupConfigs();
        
        databaseManager = new DatabaseManager(this);
        databaseConnected = databaseManager.connect();
        
        if (!databaseConnected) {
            logger.severe("§cНе удалось подключиться к базе данных!");
            logger.severe("§cМаркет временно не работает!");
            logger.severe("§cПроверьте настройки database в config.yml");
            return;
        }
        
        databaseManager.createTables();
        
        economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            logger.warning("§cЭкономика Vault не настроена!");
            logger.warning("§cНастройте, затем вернитесь и перезапустить плагин/сервер!");
        }
        
        itemManager = new ItemManager(this);
        notificationManager = new NotificationManager(this);
        soundManager = new SoundManager(this);
        sortingManager = new SortingManager(this);
        validationManager = new ValidationManager(this);
        logManager = new LogManager(this);
        logManager.setup();
        
        inventoryUtils = new InventoryUtils();
        marketManager = new MarketManager(this);
        transactionManager = new TransactionManager(this);
        guiManager = new GUIManager(this);
        updateChecker = new UpdateChecker(this); // инициализация
        
        getCommand("market").setExecutor(new MarketCommand(this));
        getCommand("market").setTabCompleter(new MarketTabCompleter(this));
        
        Bukkit.getPluginManager().registerEvents(new MarketListener(this), this);
        
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (transactionManager != null) {
                transactionManager.processPendingTransactions();
            }
        }, 1200L, 1200L);
        
        logger.info("§aПлагин активен!");
        logger.info("§a=======================================");
    }
    
    @Override
    public void onDisable() {
        if (guiManager != null) {
            guiManager.cleanup();
        }
        
        if (marketManager != null) {
            marketManager.cleanup();
        }
        
        BuyGUI.clearAllSelections();
        MarketGUI.removePlayerPage(null);
        MyItemsGUI.removePlayerPage(null);
        
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        
        logger.info("§cMonkMarket отключен");
    }
    
    public static MonkMarket getInstance() {
        return instance;
    }
    
    public MarketManager getMarketManager() {
        return marketManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
    
    public ItemManager getItemManager() {
        return itemManager;
    }
    
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
    
    public SoundManager getSoundManager() {
        return soundManager;
    }
    
    public SortingManager getSortingManager() {
        return sortingManager;
    }
    
    public ValidationManager getValidationManager() {
        return validationManager;
    }
    
    public LogManager getLogManager() {
        return logManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public InventoryUtils getInventoryUtils() {
        return inventoryUtils;
    }
    
    public boolean isDatabaseConnected() {
        return databaseConnected;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public void checkUpdates(org.bukkit.command.CommandSender sender) {
        if (updateChecker != null) {
            updateChecker.checkForUpdates(sender);
        }
    }
}