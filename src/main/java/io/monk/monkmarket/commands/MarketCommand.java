package io.monk.monkmarket.commands;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.gui.MarketGUI;
import io.monk.monkmarket.managers.ValidationManager;
import io.monk.monkmarket.utils.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class MarketCommand implements CommandExecutor {
    
    private final MonkMarket plugin;
    
    public MarketCommand(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда только для игроков!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!plugin.isDatabaseConnected()) {
            player.sendMessage("§cМаркет временно не работает! Проверьте подключение к базе данных.");
            return true;
        }
        
        if (args.length == 0) {
            MarketGUI.openMainMenu(player, 1);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "sell":
                handleSellCommand(player, args);
                break;
                
            case "search":
                handleSearchCommand(player, args);
                break;
                
            case "my":
                handleMyCommand(player);
                break;
                
            case "admin":
                handleAdminCommand(player, args);
                break;
                
            case "help":
                sendHelp(player);
                break;
                
            default:
                player.sendMessage("§cНеизвестная команда. Используйте §e/market help");
                break;
        }
        
        return true;
    }
    
    private void handleSellCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользование: §e/market sell <цена> [количество]");
            player.sendMessage("  §7&oЕсли количество не указано, продаётся весь стак из руки");
            player.sendMessage("§cПримеры:");
            player.sendMessage("§e/market sell 100 §7- продать всё в руке, 100 за шт.");
            player.sendMessage("§e/market sell 100 32 §7- продать 32 предмета, 100 за шт.");
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        String priceStr = args[1];
        String amountStr = args.length > 2 ? args[2] : null;
        
        ValidationManager.ValidationResult result = plugin.getValidationManager().validateSell(
            player, item, priceStr, amountStr);
        
        if (!result.isSuccess()) {
            sendErrorMessage(player, result.getMessage());
            return;
        }
        
        boolean success = plugin.getMarketManager().addItemToMarket(
            player, 
            result.getItem(), 
            result.getPrice(), 
            result.getAmount()
        );
        
        if (success) {
            if (result.getAmount() == result.getItem().getAmount()) {
                player.getInventory().setItemInMainHand(null);
            } else {
                item.setAmount(item.getAmount() - result.getAmount());
            }
            
            String message = plugin.getConfigManager().getMessage("item-listed");
            if (message.isEmpty()) {
                message = "&aПредмет выставлен на продажу за &e%price% монет за шт. = %total% монет Всего";
                if (result.getAmount() > 1) {
                    message += " (количество: %amount%)";
                }
            }
            
            double totalPrice = result.getPrice() * result.getAmount();
            message = message.replace("%price%", String.format("%.2f", result.getPrice()))
                            .replace("%amount%", String.valueOf(result.getAmount()))
                            .replace("%total%", String.format("%.2f", totalPrice));
            
            player.sendMessage(StringUtils.colorize(message));
            plugin.getSoundManager().playSellSound(player);
            
            if (result.getAmount() == item.getAmount()) {}
        } else {
            player.sendMessage("§cНе удалось выставить предмет на продажу! :(");
        }
    }
    
    private void sendErrorMessage(Player player, String messageKey) {
        String message = plugin.getConfigManager().getMessage(messageKey);
        
        if (message.isEmpty()) {
            switch (messageKey) {
                case "no-item-in-hand":
                    message = "§cВозьмите предмет в руку для продажи!";
                    break;
                case "invalid-price":
                    message = "§cНеверная цена! Укажите число больше 0.";
                    break;
                case "price-too-high":
                    double maxPrice = plugin.getConfig().getDouble("economy.max-price", 1000000.0);
                    message = "§cЦена слишком высокая! Максимум: " + String.format("%.2f", maxPrice);
                    break;
                case "price-too-low":
                    double minPrice = plugin.getConfig().getDouble("economy.min-price", 1.0);
                    message = "§cЦена слишком низкая! Минимум: " + String.format("%.2f", minPrice);
                    break;
                case "invalid-price-format":
                    message = "§cНеверный формат цены!";
                    break;
                case "invalid-amount":
                    message = "§cНеверное количество! Укажите число >0.";
                    break;
                case "not-enough-items":
                    message = "§cУ вас недостаточно предметов для продажи!";
                    break;
                case "amount-too-high":
                    int maxAmount = plugin.getConfig().getInt("items.max-sell-amount", 2304);
                    message = "§cКоличество слишком большое! Максимум: " + maxAmount;
                    break;
                case "invalid-amount-format":
                    message = "§cНеверный формат количества!";
                    break;
                case "item-blacklisted":
                    message = "§cЭтот предмет нельзя продавать!";
                    break;
                case "item-limit-reached":
                    int limit = plugin.getConfig().getInt("settings.max-items-per-player", 45);
                    message = "§cВы достигли лимита лотов! Максимум: " + limit;
                    break;
                case "invalid-item":
                    message = "§cЭтот предмет нельзя продавать!";
                    break;
                default:
                    message = "§cОшибка при продаже предмета";
            }
        } else {
            message = StringUtils.colorize(message);
            
            if (messageKey.equals("price-too-high")) {
                double maxPrice = plugin.getConfig().getDouble("economy.max-price", 1000000.0);
                message = message.replace("%max_price%", String.format("%.2f", maxPrice));
            } else if (messageKey.equals("price-too-low")) {
                double minPrice = plugin.getConfig().getDouble("economy.min-price", 1.0);
                message = message.replace("%min_price%", String.format("%.2f", minPrice));
            } else if (messageKey.equals("amount-too-high")) {
                int maxAmount = plugin.getConfig().getInt("items.max-sell-amount", 2304);
                message = message.replace("%max_amount%", String.valueOf(maxAmount));
            } else if (messageKey.equals("item-limit-reached")) {
                int limit = plugin.getConfig().getInt("settings.max-items-per-player", 45);
                message = message.replace("%limit%", String.valueOf(limit));
            }
        }
        
        player.sendMessage(message);
    }
    
    private void handleSearchCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользование: §e/market search <название предмета>");
            player.sendMessage("§cПример: §e/market search алмаз");
            return;
        }
        
        StringBuilder queryBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            queryBuilder.append(args[i]);
            if (i < args.length - 1) {
                queryBuilder.append(" ");
            }
        }
        String query = queryBuilder.toString().trim();
        
        plugin.getMarketManager().setPlayerSearch(player.getUniqueId(), query);
        MarketGUI.openMainMenu(player, 1);
        
        String message = plugin.getConfigManager().getMessage("search-results");
        if (message.isEmpty()) {
            message = "&fРезультаты поиска по запросу: &e%query%";
        }
        
        message = message.replace("%query%", query);
        player.sendMessage(StringUtils.colorize(message));
    }
    
    private void handleMyCommand(Player player) {
        MarketGUI.openMyItemsMenu(player, 1);
    }
    
    private void handleAdminCommand(Player player, String[] args) {
        if (!player.hasPermission("monkmarket.admin")) {
            player.sendMessage("§cУ вас нет прав для этой команды!");
            return;
        }
        
        if (args.length < 2) {
            sendAdminHelp(player);
            return;
        }
        
        String adminSub = args[1].toLowerCase();
        
        switch (adminSub) {
            case "reload":
                plugin.getConfigManager().reloadConfigs();
                player.sendMessage("§aКонфигурация перезагружена!");
                break;
                
            case "stats":
                Map<String, Object> stats = plugin.getMarketManager().getMarketStatistics();
                player.sendMessage("§6=== Статистика рынка ===");
                player.sendMessage("§7Всего товаров: §f" + stats.get("total_items"));
                player.sendMessage("§7Активных лотов: §a" + stats.get("active_items"));
                player.sendMessage("§7Продано сегодня: §e" + stats.get("sold_today"));
                player.sendMessage("§7Общий оборот: §6" + String.format("%.2f", stats.get("total_turnover")));
                break;
                
            case "clearcache":
                plugin.getMarketManager().clearAllCache();
                player.sendMessage("§aКэш очищен!");
                break;
                
            case "paynow":
                plugin.getTransactionManager().processPendingTransactions();
                player.sendMessage("§aPayFiveM Запущен!");
                break;
                
            case "dbdelete":
                AdminCommand adminCommand = new AdminCommand(plugin);
                String[] newArgs = new String[args.length - 1];
                newArgs[0] = "dbdelete";
                for (int i = 2; i < args.length; i++) {
                    newArgs[i - 1] = args[i];
                }
                adminCommand.onCommand(player, null, "admin", newArgs);
                break;
                
            case "help":
                sendAdminHelp(player);
                break;
                
            default:
                player.sendMessage("§cНеизвестная админ команда :(");
                sendAdminHelp(player);
                break;
        }
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6=== MonkMarket Помощь ===");
        player.sendMessage("§e/market §f- открыть рынок");
        player.sendMessage("§e/market sell <цена> [количество] §f- продать предмет в руке");
        player.sendMessage("§7&l&o   Если количество не указано, продаётся ВСЁ в РУКЕ!");
        player.sendMessage("§e/market search <название> §f- поиск предмета на рынке");
        player.sendMessage("§e/market my §f- мои лоты");
        player.sendMessage("§e/market help §f- показать эту помощь");
        
        if (player.hasPermission("monkmarket.admin")) {
            player.sendMessage("§c/market admin <команда> §f- админ панель");
        }
    }
    
    private void sendAdminHelp(Player player) {
        player.sendMessage("§6=== Админ команды MonkMarket ===");
        player.sendMessage("§e/market admin reload §f- перезагрузить конфиги");
        player.sendMessage("§e/market admin stats §f- статистика рынка");
        player.sendMessage("§e/market admin clearcache §f- очистить кэш");
        player.sendMessage("§e/market admin paynow §f- начать PayFiveM");
        player.sendMessage("§e/market admin dbdelete §f- удалить БД маркета");
        player.sendMessage("§e/market admin help §f- показать эту помощь");
    }
}