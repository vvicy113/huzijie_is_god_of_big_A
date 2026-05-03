package com.stock.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SQLite 数据库管理器。单例，管理连接和建表。
 * 数据库文件存储在项目根目录 data/stocks.db。
 */
public class DatabaseManager {

    private static volatile DatabaseManager instance;

    private final String url;
    private final String dbPath;

    private DatabaseManager() {
        this.dbPath = ProjectPaths.resolve("data/stocks.db").toString();
        File dbFile = new File(dbPath);
        File parent = dbFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                    instance.initialize();
                }
            }
        }
        return instance;
    }

    private void initialize() {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("PRAGMA journal_mode=WAL")) {
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement("PRAGMA synchronous=NORMAL")) {
                ps.execute();
            }

            String sql = """
                    CREATE TABLE IF NOT EXISTS daily_kline (
                        stock_code TEXT NOT NULL,
                        date       TEXT NOT NULL,
                        open       REAL,
                        high       REAL,
                        low        REAL,
                        close      REAL,
                        volume     REAL,
                        amount     REAL,
                        PRIMARY KEY (stock_code, date)
                    ) WITHOUT ROWID
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.execute();
            }

            String idxSql = "CREATE INDEX IF NOT EXISTS idx_kline_stock ON daily_kline(stock_code)";
            try (PreparedStatement ps = conn.prepareStatement(idxSql)) {
                ps.execute();
            }

        } catch (SQLException e) {
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public String getDbPath() {
        return dbPath;
    }

    public static void setBulkImportMode(Connection conn) {
        try {
            conn.prepareStatement("PRAGMA synchronous=OFF").execute();
            conn.prepareStatement("PRAGMA cache_size=1000000").execute();
        } catch (SQLException e) {
            throw new RuntimeException("设置导入模式失败", e);
        }
    }

    public static void setNormalMode(Connection conn) {
        try {
            conn.prepareStatement("PRAGMA synchronous=NORMAL").execute();
            conn.prepareStatement("PRAGMA cache_size=-2000").execute();
            conn.prepareStatement("PRAGMA optimize").execute();
        } catch (SQLException e) {
            throw new RuntimeException("恢复普通模式失败", e);
        }
    }
}
