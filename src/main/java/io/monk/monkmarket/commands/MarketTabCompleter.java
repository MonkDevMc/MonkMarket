package io.monk.monkmarket.commands;

import io.monk.monkmarket.MonkMarket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import java.util.*;

public class MarketTabCompleter implements TabCompleter {
    
    private final MonkMarket plugin;
    private final List<String> mainCommands = Arrays.asList("sell", "search", "my", "help");
    private final List<String> adminCommands = Arrays.asList("reload", "stats", "clearcache", "paynow", "dbdelete");
    
    public MarketTabCompleter(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> availableCommands = new ArrayList<>(mainCommands);
            
            if (player.hasPermission("monkmarket.admin")) {
                availableCommands.add("admin");
            }
            
            StringUtil.copyPartialMatches(input, availableCommands, completions);
            Collections.sort(completions);
        }
        
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("sell")) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && !item.getType().isAir()) {
                    completions.add("<цена>");
                    completions.add("<количество>");
                }
            }
            
            else if (subCommand.equals("search")) {
                String input = args[1].toLowerCase();
                if (input.length() >= 1) {
                    completions.addAll(getItemSuggestions(input));
                } else {
                    completions.add("<название предмета>");
                }
            }
            
            else if (subCommand.equals("admin")) {
                if (player.hasPermission("monkmarket.admin")) {
                    String input = args[1].toLowerCase();
                    StringUtil.copyPartialMatches(input, adminCommands, completions);
                    Collections.sort(completions);
                }
            }
        }
        
        else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("sell")) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && !item.getType().isAir()) {
                    int maxAmount = item.getAmount();
                    completions.add(String.valueOf(maxAmount));
                    completions.add("<цена>");
                }
            }
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
        
        if (suggestions.size() > 10) {
            suggestions = suggestions.subList(0, 10);
        }
        
        return suggestions;
    }
}