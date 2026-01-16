package io.monk.monkmarket.commands;

import io.monk.monkmarket.MonkMarket;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.io.File;
import java.util.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class AdminCommand implements CommandExecutor, TabCompleter {
    
    private final MonkMarket plugin;
    private final List<String> subCommands = Arrays.asList("reload", "stats", "clearcache", "paynow", "dbdelete", "help");
    private final Map<UUID, Long> deleteConfirmations = new HashMap<>();
    private static final long CONFIRM_TIMEOUT = 30000;
    
    public AdminCommand(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("monkmarket.admin")) {
            sender.sendMessage("§cУ вас нет прав для этой команды!");
            return true;
        }
        
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
                
            case "stats":
                handleStats(sender);
                break;
                
            case "clearcache":
                handleClearCache(sender);
                break;
                
            case "paynow":
                handlePayNow(sender);
                break;
                
            case "dbdelete":
                handleDbDelete(sender, args);
                break;
                
            case "help":
                sendHelp(sender);
                break;
                
            default:
                sender.sendMessage("§cНеизвестная команда. Используйте §e/market admin help");
                break;
        }
        
        return true;
    }
    
    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadConfigs();
        sender.sendMessage("§aКонфигурация перезагружена!");
        plugin.getLogger().info("Конфигурация перезагружена " + getSenderName(sender));
    }
    
    private void handleStats(CommandSender sender) {
        Map<String, Object> stats = plugin.getMarketManager().getMarketStatistics();
        
        sender.sendMessage("§6=== Статистика рынка ===");
        sender.sendMessage("§7Всего товаров: §f" + stats.get("total_items"));
        sender.sendMessage("§7Активных лотов: §a" + stats.get("active_items"));
        sender.sendMessage("§7Продано сегодня: §e" + stats.get("sold_today"));
        
        double turnover = (double) stats.get("total_turnover");
        sender.sendMessage("§7Общий оборот: §6" + String.format("%.2f", turnover));
        
        int activeItems = (int) stats.get("active_items");
        if (activeItems > 0) {
            double avgPrice = turnover / activeItems;
            sender.sendMessage("§7Средняя цена: §b" + String.format("%.2f", avgPrice));
        }
    }
    
    private void handleClearCache(CommandSender sender) {
        plugin.getMarketManager().clearAllCache();
        sender.sendMessage("§aКэш очищен!");
        plugin.getLogger().info("Кэш очищен " + getSenderName(sender));
    }
    
    private void handlePayNow(CommandSender sender) {
        plugin.getTransactionManager().processPendingTransactions();
        sender.sendMessage("§aPayFiveM Запущен!");
        plugin.getLogger().info("Запущена выдача денег " + getSenderName(sender));
    }
    
    private void handleDbDelete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            deleteDatabaseFiles(sender);
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            handleDbDeleteConfirmation(player);
            return;
        }
        
        requestDbDeleteConfirmation(player);
    }
    
    private void requestDbDeleteConfirmation(Player player) {
        player.sendMessage("§c§l⚠ ВНИМАНИЕ! ВЫ СОБИРАЕТЕСЬ УДАЛИТЬ БАЗУ ДАННЫХ МАРКЕТА!");
        player.sendMessage("§cЭто действие:");
        player.sendMessage("§c• Удалит ВСЕ товары на рынке");
        player.sendMessage("§c• Удалит ВСЮ историю продаж");
        player.sendMessage("§c• НЕЛЬЗЯ отменить!");
        player.sendMessage("");
        player.sendMessage("§eДля подтверждения введите:");
        player.sendMessage("§6/market admin dbdelete confirm");
        player.sendMessage("");
        player.sendMessage("§7У вас есть 30 секунд для подтверждения");
        
        deleteConfirmations.put(player.getUniqueId(), System.currentTimeMillis());
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (deleteConfirmations.containsKey(player.getUniqueId())) {
                deleteConfirmations.remove(player.getUniqueId());
                if (player.isOnline()) {
                    player.sendMessage("§cВремя подтверждения истекло!");
                }
            }
        }, CONFIRM_TIMEOUT / 50);
    }
    
    private void handleDbDeleteConfirmation(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!deleteConfirmations.containsKey(playerId)) {
            player.sendMessage("§cВремя подтверждения истекло или не было запроса!");
            player.sendMessage("§cВведите сначала §e/market admin dbdelete");
            return;
        }
        
        long requestTime = deleteConfirmations.get(playerId);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - requestTime > CONFIRM_TIMEOUT) {
            player.sendMessage("§cВремя подтверждения истекло!");
            deleteConfirmations.remove(playerId);
            return;
        }
        
        deleteConfirmations.remove(playerId);
        
        deleteDatabaseFiles(player);
    }
    
    private void deleteDatabaseFiles(CommandSender sender) {
        try {
            sender.sendMessage("§7[1/4] Подготовка к удалению БД...");
            
            String dbType = plugin.getConfigManager().getDatabaseType().toUpperCase();
            boolean isMySQL = dbType.equals("MYSQL") || dbType.equals("MARIADB");
            
            if (isMySQL) {
                sender.sendMessage("§7[2/4] Очищаю MySQL таблицы...");
                clearMySqlTables();
                sender.sendMessage("§7[3/4] Таблицы очищены");
            } else {
                sender.sendMessage("§7[2/4] Закрываю подключения к БД...");
                if (plugin.getDatabaseManager() != null) {
                    plugin.getDatabaseManager().disconnect();
                    sender.sendMessage("§7[3/4] Подключения закрыты");
                }
            }
            
            sender.sendMessage("§7[4/4] Очищаю кэш и удаляю файлы...");
            plugin.getMarketManager().clearAllCache();
            
            boolean deleted = false;
            
            if (!isMySQL) {
                deleted = deleteLocalDatabaseFiles();
            } else {
                deleted = true;
            }
            
            if (deleted) {
                sender.sendMessage("§a✅ База данных успешно очищена!");
                
                plugin.getLogger().warning("База данных очищена " + getSenderName(sender));
                
                String alertMessage = "§c§lВНИМАНИЕ: §cБаза данных маркета была очищена " + getSenderName(sender);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("monkmarket.admin")) {
                        online.sendMessage(alertMessage);
                    }
                }
                
                if (plugin.getDatabaseManager() != null && !plugin.getDatabaseManager().isConnected()) {
                    plugin.getDatabaseManager().connect();
                    plugin.getDatabaseManager().createTables();
                    sender.sendMessage("§aПодключение к БД восстановлено!");
                }
            } else {
                sender.sendMessage("§c❌ Не удалось удалить файлы БД!");
            }
            
        } catch (Exception e) {
            sender.sendMessage("§cОшибка при удалении БД: " + e.getMessage());
            plugin.getLogger().severe("Ошибка удаления БД: " + e.getMessage());
        }
    }
    
    private boolean deleteLocalDatabaseFiles() {
        try {
            File pluginFolder = plugin.getDataFolder();
            boolean anyDeleted = false;
            
            File dbFile = new File(pluginFolder, "market.db");
            if (dbFile.exists()) {
                if (dbFile.delete()) {
                    plugin.getLogger().info("Удален файл: " + dbFile.getName());
                    anyDeleted = true;
                } else {
                    plugin.getLogger().warning("Не удалось удалить файл: " + dbFile.getName());
                }
            }
            
            File dbJournal = new File(pluginFolder, "market.db-journal");
            if (dbJournal.exists()) dbJournal.delete();
            
            File dbWal = new File(pluginFolder, "market.db-wal");
            if (dbWal.exists()) dbWal.delete();
            
            File dbShm = new File(pluginFolder, "market.db-shm");
            if (dbShm.exists()) dbShm.delete();
            
            File h2Db = new File(pluginFolder, "market.mv.db");
            if (h2Db.exists()) {
                if (h2Db.delete()) {
                    plugin.getLogger().info("Удален файл H2: " + h2Db.getName());
                    anyDeleted = true;
                }
            }
            
            File h2Trace = new File(pluginFolder, "market.trace.db");
            if (h2Trace.exists()) h2Trace.delete();
            
            File[] backupFiles = pluginFolder.listFiles((dir, name) -> 
                name.startsWith("market_backup_") && (name.endsWith(".db") || name.endsWith(".mv.db")));
            
            if (backupFiles != null) {
                for (File backup : backupFiles) {
                    if (backup.delete()) {
                        plugin.getLogger().info("Удален бэкап: " + backup.getName());
                    }
                }
            }
            
            File dbLogs = new File(pluginFolder, "database.log");
            if (dbLogs.exists()) dbLogs.delete();
            
            File cacheFile = new File(pluginFolder, "cache.db");
            if (cacheFile.exists()) cacheFile.delete();
            
            return anyDeleted;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при удалении файлов БД: " + e.getMessage());
            return false;
        }
    }
    
    private void clearMySqlTables() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = plugin.getDatabaseManager().getConnection();
            if (conn == null || conn.isClosed()) {
                plugin.getDatabaseManager().connect();
                conn = plugin.getDatabaseManager().getConnection();
            }
            
            if (conn != null && !conn.isClosed()) {
                stmt = conn.createStatement();
                
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                
                stmt.execute("TRUNCATE TABLE market_items");
                stmt.execute("TRUNCATE TABLE market_transactions");
                
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                
                plugin.getLogger().info("MySQL таблицы очищены");
            }
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException ignored) {}
            }
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Админ команды MonkMarket ===");
        sender.sendMessage("§e/market admin reload §f- перезагрузить конфиги");
        sender.sendMessage("§e/market admin stats §f- статистика рынка");
        sender.sendMessage("§e/market admin clearcache §f- очистить кэш");
        sender.sendMessage("§e/market admin paynow §f- начать PayFiveM");
        sender.sendMessage("§e/market admin dbdelete §f- удалить БД маркета");
        sender.sendMessage("§e/market admin help §f- показать эту помощь");
    }
    
    private String getSenderName(CommandSender sender) {
        if (sender instanceof Player) {
            return "администратором " + ((Player) sender).getName();
        }
        return "из КОНСОЛИ";
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("monkmarket.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String cmd : subCommands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("dbdelete")) {
            if ("confirm".startsWith(args[1].toLowerCase())) {
                completions.add("confirm");
            }
        }
        
        return completions;
    }
    
    public void cleanup() {
        deleteConfirmations.clear();
    }
}