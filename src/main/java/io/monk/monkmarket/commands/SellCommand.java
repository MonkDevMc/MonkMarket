package io.monk.monkmarket.commands;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.managers.ValidationManager;
import io.monk.monkmarket.utils.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand implements CommandExecutor {
    
    private final MonkMarket plugin;
    
    public SellCommand(MonkMarket plugin) {
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
            player.sendMessage("§cМаркет временно не работает!");
            return true;
        }
        
        if (args.length < 1) {
            player.sendMessage("§cИспользование: §e/sell <цена> [количество]");
            player.sendMessage("§c&l&oВажно!!! §cЕсли не указывать количество:");
            player.sendMessage("1- продаётся всё что есть в руке!");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        String priceStr = args[0];
        String amountStr = args.length > 1 ? args[1] : null;
        
        ValidationManager.ValidationResult result = plugin.getValidationManager().validateSell(
            player, item, priceStr, amountStr);
        
        if (!result.isSuccess()) {
            sendErrorMessage(player, result.getMessage());
            return true;
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
                message = "&aПредмет выставлен на продажу за &e%price% монет (количество: %amount%)";
            }
            
            message = message.replace("%price%", String.format("%.2f", result.getPrice()))
                            .replace("%amount%", String.valueOf(result.getAmount()));
            
            player.sendMessage(StringUtils.colorize(message));
            plugin.getSoundManager().playSellSound(player);
        } else {
            player.sendMessage("§cНе удалось выставить предмет на продажу!");
        }
        
        return true;
    }
    
    private void sendErrorMessage(Player player, String messageKey) {
        String message = plugin.getConfigManager().getMessage(messageKey);
        
        if (message.isEmpty()) {
            switch (messageKey) {
                case "no-item-in-hand":
                    message = "§cВозьмите предмет в руку для продажи!";
                    break;
                case "invalid-price":
                    message = "§cНеверная цена! Укажите число >0.";
                    break;
                case "price-too-high":
                    double maxPrice = plugin.getConfig().getDouble("economy.max-price", 1000000.0);
                    message = "§cЦена слишком высокая! Максимум: " + String.format("%.2f", maxPrice);
                    break;
                case "price-too-low":
                    double minPrice = plugin.getConfig().getDouble("economy.min-price", 1.0);
                    message = "§cЦена слишком низкая! Минимум: " + String.format("%.2f", minPrice);
                    break;
                default:
                    message = "§cОкак, Неизвестная ошибка :(";
            }
        } else {
            message = StringUtils.colorize(message);
        }
        
        player.sendMessage(message);
    }
}