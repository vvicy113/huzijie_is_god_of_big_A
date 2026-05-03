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

    private static final String DB_PATH = "data/stocks.db";
    private static volatile DatabaseManager instance;

    private final String url;

    private DatabaseManager() {
        File dbFile = new File(DB_PATH);
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
            // WAL 模式：读写并发更好
            try (PreparedStatement ps = conn.prepareStatement("PRAGMA journal_mode=WAL")) {
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement("PRAGMA synchronous=NORMAL")) {
                ps.execute();
            }

            // 建表
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

            // 日K索引
            String idxDaily = "CREATE INDEX IF NOT EXISTS idx_kline_stock ON daily_kline(stock_code)";
            try (PreparedStatement ps = conn.prepareStatement(idxDaily)) {
                ps.execute();
            }

            // 分时数据表
            String minuteSql = """
                    CREATE TABLE IF NOT EXISTS minute_kline (
                        stock_code TEXT NOT NULL,
                        date       TEXT NOT NULL,
                        time       TEXT NOT NULL,
                        open       REAL,
                        high       REAL,
                        low        REAL,
                        close      REAL,
                        volume     REAL,
                        amount     REAL,
                        PRIMARY KEY (stock_code, date, time)
                    ) WITHOUT ROWID
                    """;
            try (PreparedStatement ps = conn.prepareStatement(minuteSql)) {
                ps.execute();
            }

            // 分时索引
            String idxMinute = "CREATE INDEX IF NOT EXISTS idx_minute_stock ON minute_kline(stock_code)";
            try (PreparedStatement ps = conn.prepareStatement(idxMinute)) {
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
        return DB_PATH;
    }

    /**
     * 设置快速导入模式（关闭同步和日志），返回之前的设置以便恢复。
     */
    public void setBulkImportMode() {
        try (Connection conn = getConnection()) {
            conn.prepareStatement("PRAGMA synchronous=OFF").execute();
            conn.prepareStatement("PRAGMA journal_mode=OFF").execute();
            conn.prepareStatement("PRAGMA cache_size=1000000").execute();
            conn.prepareStatement("PRAGMA mmap_size=268435456").execute();
        } catch (SQLException e) {
            throw new RuntimeException("设置导入模式失败", e);
        }
    }

    /**
     * 恢复正常运行模式。
     */
    public void setNormalMode() {
        try (Connection conn = getConnection()) {
            conn.prepareStatement("PRAGMA synchronous=NORMAL").execute();
            conn.prepareStatement("PRAGMA journal_mode=WAL").execute();
            conn.prepareStatement("PRAGMA cache_size=-2000").execute();
            conn.prepareStatement("PRAGMA mmap_size=0").execute();
            conn.prepareStatement("PRAGMA optimize").execute();
        } catch (SQLException e) {
            throw new RuntimeException("恢复普通模式失败", e);
        }
    }
}
