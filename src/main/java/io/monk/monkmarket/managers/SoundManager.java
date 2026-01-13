package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {
    
    private final MonkMarket plugin;
    
    public SoundManager(MonkMarket plugin) {
        this.plugin = plugin;
    }
    
    public void playPurchaseSound(Player player) {
        if (!plugin.getConfigManager().isSoundEnabled("purchase")) {
            return;
        }
        
        String soundName = plugin.getConfigManager().getSound("purchase");
        float volume = plugin.getConfigManager().getSoundVolume("purchase");
        float pitch = plugin.getConfigManager().getSoundPitch("purchase");
        
        Sound sound = getSoundByName(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
    
    public void playSellSound(Player player) {
        if (!plugin.getConfigManager().isSoundEnabled("sell")) {
            return;
        }
        
        String soundName = plugin.getConfigManager().getSound("sell");
        float volume = plugin.getConfigManager().getSoundVolume("sell");
        float pitch = plugin.getConfigManager().getSoundPitch("sell");
        
        Sound sound = getSoundByName(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
    
    public void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    private Sound getSoundByName(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неизвестный звук: " + soundName);
            return Sound.ENTITY_PLAYER_LEVELUP;
        }
    }
}