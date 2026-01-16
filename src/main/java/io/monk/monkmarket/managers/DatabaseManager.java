package io.monk.monkmarket.managers;

import io.monk.monkmarket.MonkMarket;
import io.monk.monkmarket.database.DatabaseType;
import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {
    
    private final MonkMarket plugin;
    private final Logger logger;
    private Connection connection;
    private DatabaseType databaseType;
    private boolean autoReconnect;
    
    public DatabaseManager(MonkMarket plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.autoReconnect = true;
    }
    
    public boolean connect() {
        String type = plugin.getConfigManager().getDatabaseType();
        
        try {
            if (type.equalsIgnoreCase("MYSQL")) {
                databaseType = DatabaseType.MYSQL;
                return connectMySQL();
            } else if (type.equalsIgnoreCase("H2")) {
                databaseType = DatabaseType.H2;
                return connectH2();
            } else {
                databaseType = DatabaseType.SQLITE;
                return connectSQLite();
            }
        } catch (Exception e) {
            logger.severe("Ошибка подключения к базе данных: " + e.getMessage());
            return false;
        }
    }
    
    private boolean connectMySQL() throws SQLException, ClassNotFoundException {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "monkmarket");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");
        boolean useSSL = plugin.getConfig().getBoolean("database.mysql.use-ssl", false);
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + 
                    "?useSSL=" + useSSL + "&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8" +
                    "&socketTimeout=30000&connectTimeout=5000";
        
        connection = DriverManager.getConnection(url, username, password);
        logger.info("Подключение к MySQL установлено");
        return true;
    }
    
    private boolean connectSQLite() throws SQLException, ClassNotFoundException {
        String filename = plugin.getConfig().getString("database.sqlite.filename", "market.db");
        String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/" + filename;
        
        Class.forName("org.sqlite.JDBC");
        
        Connection conn = DriverManager.getConnection(url);
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA busy_timeout=5000");
        }
        
        connection = conn;
        logger.info("База данных SQLite создана: " + filename);
        return true;
    }
    
    private boolean connectH2() throws SQLException, ClassNotFoundException {
        String filename = plugin.getConfig().getString("database.h2.filename", "market");
        String url = "jdbc:h2:file:" + plugin.getDataFolder().getAbsolutePath() + "/" + filename;
        
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(url);
        logger.info("База данных H2 создана: " + filename);
        return true;
    }
    
    public void createTables() {
        try (Statement stmt = getValidConnection().createStatement()) {
            String itemsTable = "CREATE TABLE IF NOT EXISTS market_items (" +
                    "id INTEGER PRIMARY KEY" + (databaseType == DatabaseType.MYSQL ? " AUTO_INCREMENT" : " AUTOINCREMENT") + ", " +
                    "seller_uuid VARCHAR(36) NOT NULL, " +
                    "seller_name VARCHAR(32) NOT NULL, " +
                    "item_material VARCHAR(64) NOT NULL, " +
                    "item_data TEXT, " +
                    "item_amount INTEGER NOT NULL DEFAULT 1, " +
                    "price_per_unit DOUBLE NOT NULL, " +
                    "total_amount INTEGER NOT NULL, " +
                    "sold_amount INTEGER NOT NULL DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "server_id VARCHAR(50) NOT NULL, " +
                    "is_sold BOOLEAN DEFAULT FALSE, " +
                    "buyer_uuid VARCHAR(36), " +
                    "sold_at TIMESTAMP" +
                    ")";
            
            stmt.execute(itemsTable);
            
            String transactionsTable = "CREATE TABLE IF NOT EXISTS market_transactions (" +
                    "id INTEGER PRIMARY KEY" + (databaseType == DatabaseType.MYSQL ? " AUTO_INCREMENT" : " AUTOINCREMENT") + ", " +
                    "buyer_uuid VARCHAR(36) NOT NULL, " +
                    "buyer_name VARCHAR(32) NOT NULL, " +
                    "seller_uuid VARCHAR(36) NOT NULL, " +
                    "seller_name VARCHAR(32) NOT NULL, " +
                    "item_material VARCHAR(64) NOT NULL, " +
                    "item_amount INTEGER NOT NULL, " +
                    "total_price DOUBLE NOT NULL, " +
                    "price_per_unit DOUBLE NOT NULL, " +
                    "transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "server_id VARCHAR(50) NOT NULL, " +
                    "is_paid BOOLEAN DEFAULT FALSE, " +
                    "paid_at TIMESTAMP" +
                    ")";
            
            stmt.execute(transactionsTable);
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_market_items_seller ON market_items(seller_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_market_items_sold ON market_items(is_sold)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_market_items_material ON market_items(item_material)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_seller ON market_transactions(seller_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_paid ON market_transactions(is_paid)");
            
            logger.info("Таблицы базы данных созданы");
            
        } catch (SQLException e) {
            logger.severe("Ошибка создания таблиц: " + e.getMessage());
        }
    }
    
    private Connection getValidConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                if (autoReconnect) {
                    logger.warning("Соединение с БД закрыто, переподключаемся...");
                    connect();
                } else {
                    throw new SQLException("Соединение с БД закрыто");
                }
            }
            
            if (databaseType == DatabaseType.MYSQL && !connection.isValid(5)) {
                logger.warning("Соединение с MySQL невалидно, переподключаемся...");
                disconnect();
                connect();
            }
            
        } catch (Exception e) {
            logger.severe("Ошибка восстановления соединения: " + e.getMessage());
            throw new SQLException("Не удалось восстановить соединение с БД");
        }
        
        return connection;
    }
    
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return getValidConnection().prepareStatement(sql);
    }
    
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return getValidConnection().prepareStatement(sql, autoGeneratedKeys);
    }
    
    public ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = getValidConnection().createStatement();
        return stmt.executeQuery(sql);
    }
    
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = getValidConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }
    
    public void disconnect() {
        autoReconnect = false;
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Подключение к базе данных закрыто");
            }
        } catch (SQLException e) {
            logger.warning("Ошибка при закрытии БД: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
    
    public Connection getConnection() {
        try {
            return getValidConnection();
        } catch (SQLException e) {
            return null;
        }
    }
    
    public boolean checkAndReconnect() {
        try {
            getValidConnection();
            return true;
        } catch (SQLException e) {
            logger.severe("Не удалось восстановить соединение с БД: " + e.getMessage());
            return false;
        }
    }
}