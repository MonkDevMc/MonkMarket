package io.monk.monkmarket.database.queries;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.models.Transaction;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class TransactionQueries {
    
    private final MonkMarket plugin;
    private final Logger logger;
    
    public TransactionQueries(MonkMarket plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public boolean addTransaction(Transaction transaction) {
        String sql = "INSERT INTO market_transactions (buyer_uuid, buyer_name, seller_uuid, " +
                    "seller_name, item_material, item_amount, total_price, price_per_unit, " +
                    "server_id, is_paid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, transaction.getBuyerUuid().toString());
            stmt.setString(2, transaction.getBuyerName());
            stmt.setString(3, transaction.getSellerUuid().toString());
            stmt.setString(4, transaction.getSellerName());
            stmt.setString(5, transaction.getItemMaterial());
            stmt.setInt(6, transaction.getItemAmount());
            stmt.setDouble(7, transaction.getTotalPrice());
            stmt.setDouble(8, transaction.getPricePerUnit());
            stmt.setString(9, transaction.getServerId());
            stmt.setBoolean(10, transaction.isPaid());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    transaction.setId(rs.getInt(1));
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.severe("Ошибка добавления транзакции: " + e.getMessage());
        }
        return false;
    }
    
    public List<Transaction> getUnpaidTransactions() {
        List<Transaction> transactions = new ArrayList<Transaction>();
        String sql = "SELECT * FROM market_transactions WHERE is_paid = FALSE ORDER BY transaction_time ASC";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(resultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения неплаченых транзакций: " + e.getMessage());
        }
        return transactions;
    }
    
    public boolean markAsPaid(int transactionId) {
        String sql = "UPDATE market_transactions SET is_paid = TRUE, paid_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Ошибка отметки транзакции как оплаченной: " + e.getMessage());
        }
        return false;
    }
    
    public List<Transaction> getSellerTransactions(UUID sellerUuid, java.util.Date startDate, java.util.Date endDate) {
        List<Transaction> transactions = new ArrayList<Transaction>();
        String sql = "SELECT * FROM market_transactions WHERE seller_uuid = ? " +
                    "AND transaction_time BETWEEN ? AND ? ORDER BY transaction_time DESC";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, sellerUuid.toString());
            stmt.setTimestamp(2, new Timestamp(startDate.getTime()));
            stmt.setTimestamp(3, new Timestamp(endDate.getTime()));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(resultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения транзакций продавца: " + e.getMessage());
        }
        return transactions;
    }
    
    public double getTotalEarnings(UUID sellerUuid) {
        String sql = "SELECT SUM(total_price) as total FROM market_transactions WHERE seller_uuid = ? AND is_paid = TRUE";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, sellerUuid.toString());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения общей суммы заработка: " + e.getMessage());
        }
        return 0.0;
    }
    
    public Map<String, Object> getMarketStatistics() {
        Map<String, Object> stats = new HashMap<String, Object>();
        String sql = "SELECT " +
                    "COUNT(*) as total_items, " +
                    "SUM(CASE WHEN is_sold = FALSE THEN 1 ELSE 0 END) as active_items, " +
                    "SUM(CASE WHEN DATE(created_at) = DATE('now') THEN 1 ELSE 0 END) as sold_today, " +
                    "COALESCE(SUM(total_price), 0) as total_turnover " +
                    "FROM (SELECT m.*, t.total_price FROM market_items m " +
                    "LEFT JOIN market_transactions t ON m.id = t.id) as combined";
        
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.put("total_items", rs.getInt("total_items"));
                stats.put("active_items", rs.getInt("active_items"));
                stats.put("sold_today", rs.getInt("sold_today"));
                stats.put("total_turnover", rs.getDouble("total_turnover"));
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения статистики рынка: " + e.getMessage());
        }
        return stats;
    }
    
    private Transaction resultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getInt("id"));
        transaction.setBuyerUuid(UUID.fromString(rs.getString("buyer_uuid")));
        transaction.setBuyerName(rs.getString("buyer_name"));
        transaction.setSellerUuid(UUID.fromString(rs.getString("seller_uuid")));
        transaction.setSellerName(rs.getString("seller_name"));
        transaction.setItemMaterial(rs.getString("item_material"));
        transaction.setItemAmount(rs.getInt("item_amount"));
        transaction.setTotalPrice(rs.getDouble("total_price"));
        transaction.setPricePerUnit(rs.getDouble("price_per_unit"));
        
        try {
            Timestamp transactionTime = rs.getTimestamp("transaction_time");
            if (transactionTime != null) {
                transaction.setTransactionTime(new java.util.Date(transactionTime.getTime()));
            }
        } catch (SQLException e) {
            transaction.setTransactionTime(null);
        }
        
        transaction.setServerId(rs.getString("server_id"));
        transaction.setPaid(rs.getBoolean("is_paid"));
        
        try {
            Timestamp paidAt = rs.getTimestamp("paid_at");
            if (paidAt != null) {
                transaction.setPaidAt(new java.util.Date(paidAt.getTime()));
            }
        } catch (SQLException e) {
            transaction.setPaidAt(null);
        }
        
        return transaction;
    }
}