package io.monk.monkmarket.commands;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.gui.MarketGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;

public class SearchCommand implements CommandExecutor, TabCompleter {
    
    private final MonkMarket plugin;
    
    public SearchCommand(MonkMarket plugin) {
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
            player.sendMessage("§cИспользование: §e/search <название предмета>");
            return true;
        }
        
        StringBuilder queryBuilder = new StringBuilder();
        for (String arg : args) {
            queryBuilder.append(arg).append(" ");
        }
        String query = queryBuilder.toString().trim();
        
        plugin.getMarketManager().setPlayerSearch(player.getUniqueId(), query);
        MarketGUI.openMainMenu(player, 1);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions.addAll(getItemSuggestions(input));
        }
        
        return completions;
    }
    
    private List<String> getItemSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();
        Map<String, String> translations = plugin.getConfigManager().getItemTranslations();
        
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String russianName = entry.getValue().toLowerCase();
            
            if (russianName.startsWith(input)) {
                suggestions.add(entry.getValue());
            }
        }
        
        if (suggestions.isEmpty()) {
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                String russianName = entry.getValue().toLowerCase();
                
                if (russianName.contains(input)) {
                    suggestions.add(entry.getValue());
                }
            }
        }
        
        Collections.sort(suggestions);
        
        if (suggestions.size() > 20) {
            suggestions = suggestions.subList(0, 20);
        }
        
        return suggestions;
    }
}