package io.monk.monkmarket.commands;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.gui.MarketGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyCommand implements CommandExecutor {
    
    private final MonkMarket plugin;
    
    public MyCommand(MonkMarket plugin) {
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
        
        MarketGUI.openMyItemsMenu(player, 1);
        return true;
    }
}