package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.Transaction;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class LogManager {
    
    private final MonkMarket plugin;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat logDateFormat;
    private File logFolder;
    private File transactionsLogFile;
    
    public LogManager(MonkMarket plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
    
    public void setup() {
        logFolder = new File(plugin.getDataFolder(), "transactions_logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
        
        String today = dateFormat.format(new Date());
        transactionsLogFile = new File(logFolder, "transactions-" + today + ".log");
    }
    
    public void logTransaction(Transaction transaction) {
        if (!plugin.getConfigManager().isLogToConsoleEnabled()) {
            return;
        }
        
        String logFormat = plugin.getConfigManager().getLogFormat();
        if (logFormat.isEmpty()) {
            logFormat = "[%time%] %player% купил %item%(%amount%x) за %price%";
        }
        
        String logMessage = logFormat
            .replace("%time%", logDateFormat.format(new Date()))
            .replace("%player%", transaction.getBuyerName())
            .replace("%amount%", String.valueOf(transaction.getItemAmount()))
            .replace("%item%", transaction.getItemMaterial())
            .replace("%price%", String.format("%.2f", transaction.getTotalPrice()));
        
        plugin.getLogger().info(logMessage);
        
        if (plugin.getConfig().getBoolean("economy.transactions.log-to-file", true)) {
            logToFile(logMessage);
        }
    }
    
    public void logToConsole(String message) {
        plugin.getLogger().info(message);
    }
    
    public void logToFile(String message) {
        if (transactionsLogFile == null) {
            setup();
        }
        
        try (FileWriter fw = new FileWriter(transactionsLogFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.println(message);
            
        } catch (IOException e) {
            plugin.getLogger().info("Ошибка записи в лог-файл :(");
            plugin.getLogger().log(Level.SEVERE, "Ошибка: ", e);
        }
    }
    
    public void logError(String message, Exception e) {
        plugin.getLogger().log(Level.SEVERE, message, e);
        
        File errorLog = new File(logFolder, "errors.log");
        try (FileWriter fw = new FileWriter(errorLog, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.println(logDateFormat.format(new Date()) + " - " + message);
            if (e != null) {
                e.printStackTrace(out);
            }
            out.println();
            
        } catch (IOException ioException) {
            plugin.getLogger().info("Ошибка записи в лог ошибок :(");
            plugin.getLogger().log(Level.SEVERE, "Ошибка: ", ioException);
        }
    }
    
    public void logAdminAction(String adminName, String action, String details) {
        String message = String.format("[%s] Админ %s: %s - %s", 
            logDateFormat.format(new Date()), adminName, action, details);
        
        plugin.getLogger().info(message);
        
        File adminLog = new File(logFolder, "admin-actions.log");
        try (FileWriter fw = new FileWriter(adminLog, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.println(message);
            
        } catch (IOException e) {
            plugin.getLogger().info("Ошибка записи в лог админ-действий :(");
            plugin.getLogger().log(Level.SEVERE, "Ошибка: ", e);
        }
    }
    
    public File getLogFolder() {
        return logFolder;
    }
}