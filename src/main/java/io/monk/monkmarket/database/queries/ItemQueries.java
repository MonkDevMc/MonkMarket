package io.monk.monkmarket.database.queries;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.MarketItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class ItemQueries {
    
    private final MonkMarket plugin;
    private final Logger logger;
    
    public ItemQueries(MonkMarket plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public boolean addItem(MarketItem marketItem) {
        String sql = "INSERT INTO market_items (seller_uuid, seller_name, item_material, " +
                    "item_data, item_amount, price_per_unit, total_amount, server_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, marketItem.getSellerUuid().toString());
            stmt.setString(2, marketItem.getSellerName());
            stmt.setString(3, marketItem.getItemMaterial());
            stmt.setString(4, serializeItemStack(marketItem.getItemStack()));
            stmt.setInt(5, marketItem.getItemStack().getAmount());
            stmt.setDouble(6, marketItem.getPricePerUnit());
            stmt.setInt(7, marketItem.getTotalAmount());
            stmt.setString(8, marketItem.getServerId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    marketItem.setId(rs.getInt(1));
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.severe("Ошибка добавления предмета: " + e.getMessage());
        }
        return false;
    }
    
    public List<MarketItem> getMarketItems(int page, int itemsPerPage, String searchQuery) {
        List<MarketItem> items = new ArrayList<>();
        String sql;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            if (searchQuery != null && !searchQuery.isEmpty()) {
                sql = "SELECT * FROM market_items WHERE is_sold = FALSE AND " +
                      "(item_material LIKE ? OR seller_name LIKE ?) " +
                      "ORDER BY created_at DESC LIMIT ? OFFSET ?";
                stmt = plugin.getDatabaseManager().prepareStatement(sql);
                String likeQuery = "%" + searchQuery + "%";
                stmt.setString(1, likeQuery);
                stmt.setString(2, likeQuery);
                stmt.setInt(3, itemsPerPage);
                stmt.setInt(4, (page - 1) * itemsPerPage);
            } else {
                sql = "SELECT * FROM market_items WHERE is_sold = FALSE " +
                      "ORDER BY created_at DESC LIMIT ? OFFSET ?";
                stmt = plugin.getDatabaseManager().prepareStatement(sql);
                stmt.setInt(1, itemsPerPage);
                stmt.setInt(2, (page - 1) * itemsPerPage);
            }
            
            rs = stmt.executeQuery();
            while (rs.next()) {
                MarketItem item = resultSetToMarketItem(rs);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения предметов: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
            }
        }
        return items;
    }
    
    public int getTotalItemsCount(String searchQuery) {
        String sql;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            if (searchQuery != null && !searchQuery.isEmpty()) {
                sql = "SELECT COUNT(*) as count FROM market_items WHERE is_sold = FALSE AND " +
                      "(item_material LIKE ? OR seller_name LIKE ?)";
                stmt = plugin.getDatabaseManager().prepareStatement(sql);
                String likeQuery = "%" + searchQuery + "%";
                stmt.setString(1, likeQuery);
                stmt.setString(2, likeQuery);
            } else {
                sql = "SELECT COUNT(*) as count FROM market_items WHERE is_sold = FALSE";
                stmt = plugin.getDatabaseManager().prepareStatement(sql);
            }
            
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения количества предметов: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
            }
        }
        return 0;
    }
    
    public List<MarketItem> getPlayerItems(UUID playerUuid, int page, int itemsPerPage) {
        List<MarketItem> items = new ArrayList<>();
        String sql = "SELECT * FROM market_items WHERE seller_uuid = ? AND is_sold = FALSE " +
                    "ORDER BY created_at DESC LIMIT ? OFFSET ?";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, itemsPerPage);
            stmt.setInt(3, (page - 1) * itemsPerPage);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MarketItem item = resultSetToMarketItem(rs);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения предметов игрока: " + e.getMessage());
        }
        return items;
    }
    
    public int getPlayerItemsCount(UUID playerUuid) {
        String sql = "SELECT COUNT(*) as count FROM market_items WHERE seller_uuid = ? AND is_sold = FALSE";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения количества предметов игрока: " + e.getMessage());
        }
        return 0;
    }
    
    public MarketItem getItemById(int itemId) {
        String sql = "SELECT * FROM market_items WHERE id = ?";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultSetToMarketItem(rs);
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения предмета по ID: " + e.getMessage());
        }
        return null;
    }
    
    public List<MarketItem> getSimilarItems(String materialName, int excludeId) {
        List<MarketItem> similar = new ArrayList<>();
        String sql = "SELECT * FROM market_items WHERE item_material = ? AND id != ? AND is_sold = FALSE ORDER BY price_per_unit ASC LIMIT 10";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, materialName);
            stmt.setInt(2, excludeId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MarketItem item = resultSetToMarketItem(rs);
                if (item != null) {
                    similar.add(item);
                }
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения похожих предметов: " + e.getMessage());
        }
        return similar;
    }
    
    public boolean updateSoldAmount(int itemId, int newSoldAmount) {
        String sql = "UPDATE market_items SET sold_amount = ? WHERE id = ?";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, newSoldAmount);
            stmt.setInt(2, itemId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            logger.severe("Ошибка обновления sold_amount: " + e.getMessage());
            return false;
        }
    }
    
    public boolean markAsSold(int itemId, UUID buyerUuid) {
        String sql = "UPDATE market_items SET is_sold = TRUE, buyer_uuid = ?, sold_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, buyerUuid != null ? buyerUuid.toString() : null);
            stmt.setInt(2, itemId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            logger.severe("Ошибка отметки предмета как проданного: " + e.getMessage());
            return false;
        }
    }
    
    public boolean removeItem(int itemId) {
        String sql = "DELETE FROM market_items WHERE id = ?";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            logger.severe("Ошибка удаления предмета: " + e.getMessage());
            return false;
        }
    }
    
    public MarketItem resultSetToMarketItem(ResultSet rs) throws SQLException {
        try {
            MarketItem item = new MarketItem();
            item.setId(rs.getInt("id"));
            item.setSellerUuid(UUID.fromString(rs.getString("seller_uuid")));
            item.setSellerName(rs.getString("seller_name"));
            item.setItemMaterial(rs.getString("item_material"));
            
            String itemData = rs.getString("item_data");
            ItemStack itemStack = null;
            if (itemData != null && !itemData.isEmpty() && !itemData.equals("null")) {
                itemStack = deserializeItemStack(itemData);
            }
            
            if (itemStack == null) {
                try {
                    org.bukkit.Material material = org.bukkit.Material.getMaterial(item.getItemMaterial());
                    if (material != null) {
                        itemStack = new ItemStack(material, 1);
                    } else {
                        itemStack = new ItemStack(org.bukkit.Material.STONE, 1);
                    }
                } catch (Exception e) {
                    itemStack = new ItemStack(org.bukkit.Material.STONE, 1);
                }
            }
            
            item.setItemStack(itemStack);
            item.setPricePerUnit(rs.getDouble("price_per_unit"));
            item.setTotalAmount(rs.getInt("total_amount"));
            item.setSoldAmount(rs.getInt("sold_amount"));
            
            try {
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    item.setCreatedAt(new java.util.Date(createdAt.getTime()));
                } else {
                    String createdAtStr = rs.getString("created_at");
                    if (createdAtStr != null && !createdAtStr.isEmpty()) {
                        if (createdAtStr.contains(".")) {
                            createdAtStr = createdAtStr.substring(0, createdAtStr.indexOf('.'));
                        }
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        item.setCreatedAt(sdf.parse(createdAtStr));
                    }
                }
            } catch (Exception e) {
                try {
                    String createdAtStr = rs.getString("created_at");
                    if (createdAtStr != null && !createdAtStr.isEmpty()) {
                        if (createdAtStr.contains(".")) {
                            createdAtStr = createdAtStr.substring(0, createdAtStr.indexOf('.'));
                        }
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        item.setCreatedAt(sdf.parse(createdAtStr));
                    }
                } catch (Exception parseEx) {
                    logger.warning("Не удалось распарсить created_at, ошибка: " + parseEx.getMessage());
                    item.setCreatedAt(null);
                }
            }
            
            item.setServerId(rs.getString("server_id"));
            item.setSold(rs.getBoolean("is_sold"));
            
            try {
                Timestamp soldAt = rs.getTimestamp("sold_at");
                if (soldAt != null) {
                    item.setSoldAt(new java.util.Date(soldAt.getTime()));
                } else {
                    String soldAtStr = rs.getString("sold_at");
                    if (soldAtStr != null && !soldAtStr.isEmpty()) {
                        if (soldAtStr.contains(".")) {
                            soldAtStr = soldAtStr.substring(0, soldAtStr.indexOf('.'));
                        }
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        item.setSoldAt(sdf.parse(soldAtStr));
                    }
                }
            } catch (Exception e) {
                try {
                    String soldAtStr = rs.getString("sold_at");
                    if (soldAtStr != null && !soldAtStr.isEmpty()) {
                        if (soldAtStr.contains(".")) {
                            soldAtStr = soldAtStr.substring(0, soldAtStr.indexOf('.'));
                        }
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        item.setSoldAt(sdf.parse(soldAtStr));
                    }
                } catch (Exception parseEx) {
                    logger.warning("Не удалось распарсить sold_at, ошибка: " + parseEx.getMessage());
                    item.setSoldAt(null);
                }
            }
            
            String buyerUuidStr = rs.getString("buyer_uuid");
            if (buyerUuidStr != null && !buyerUuidStr.isEmpty()) {
                item.setBuyerUuid(UUID.fromString(buyerUuidStr));
            }
            
            return item;
        } catch (Exception e) {
            logger.severe("Ошибка преобразования ResultSet в MarketItem: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String serializeItemStack(ItemStack item) {
        if (item == null) {
            return "";
        }
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            logger.severe("Ошибка сериализации ItemStack: " + e.getMessage());
            return "";
        }
    }
    
    private ItemStack deserializeItemStack(String data) {
        if (data == null || data.isEmpty() || data.equals("null")) {
            return null;
        }
        
        data = data.trim();
        
        if (!isValidBase64(data)) {
            return null;
        }
        
        int padding = data.length() % 4;
        if (padding > 0) {
            StringBuilder sb = new StringBuilder(data);
            for (int i = 0; i < (4 - padding); i++) {
                sb.append('=');
            }
            data = sb.toString();
        }
        
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (IOException | ClassNotFoundException e) {
            logger.warning("Ошибка десериализации ItemStack: " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            logger.warning("Некорректная Base64 строка для ItemStack: " + e.getMessage());
            return null;
        }
    }
    
    private boolean isValidBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        String base64Pattern = "^[A-Za-z0-9+/=]+$";
        if (!str.matches(base64Pattern)) {
            return false;
        }
        
        String noPadding = str.replace("=", "");
        return noPadding.length() % 4 == 0 || str.length() % 4 == 0;
    }
}