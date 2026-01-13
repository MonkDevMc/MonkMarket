package io.monk.monkmarket.database.models;

import java.util.UUID;
import java.util.Date;

public class PlayerData {
    private int id;
    private UUID playerUuid;
    private String playerName;
    private double totalSpent;
    private double totalEarned;
    private int itemsSold;
    private int itemsBought;
    private Date firstJoin;
    private Date lastSeen;
    private boolean isBanned;
    private String banReason;
    
    public PlayerData() {}
    
    public PlayerData(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.totalSpent = 0;
        this.totalEarned = 0;
        this.itemsSold = 0;
        this.itemsBought = 0;
        this.isBanned = false;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public UUID getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }
    public void addTotalSpent(double amount) { this.totalSpent += amount; }
    
    public double getTotalEarned() { return totalEarned; }
    public void setTotalEarned(double totalEarned) { this.totalEarned = totalEarned; }
    public void addTotalEarned(double amount) { this.totalEarned += amount; }
    
    public int getItemsSold() { return itemsSold; }
    public void setItemsSold(int itemsSold) { this.itemsSold = itemsSold; }
    public void incrementItemsSold() { this.itemsSold++; }
    public void incrementItemsSold(int amount) { this.itemsSold += amount; }
    
    public int getItemsBought() { return itemsBought; }
    public void setItemsBought(int itemsBought) { this.itemsBought = itemsBought; }
    public void incrementItemsBought() { this.itemsBought++; }
    public void incrementItemsBought(int amount) { this.itemsBought += amount; }
    
    public Date getFirstJoin() { return firstJoin; }
    public void setFirstJoin(Date firstJoin) { this.firstJoin = firstJoin; }
    
    public Date getLastSeen() { return lastSeen; }
    public void setLastSeen(Date lastSeen) { this.lastSeen = lastSeen; }
    
    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean banned) { isBanned = banned; }
    
    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }
}