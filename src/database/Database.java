package database;

import java.sql.*;

public class Database {
    private static final String DB_PATH = System.getProperty("user.dir") + "/data/play_ttt.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;
    
    static {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found!");
            e.printStackTrace();
        }
        initDatabase();
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
    
    private static void initDatabase() {
        try (Connection conn = getConnection(); 
             Statement stmt = conn.createStatement()) {
            
            // Tạo bảng users
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    username TEXT UNIQUE NOT NULL," +
                "    password TEXT NOT NULL," +
                "    wins INTEGER NOT NULL DEFAULT 0," +
                "    losses INTEGER NOT NULL DEFAULT 0," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            // Tạo bảng games (lịch sử trận đấu)
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS games (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    player1 TEXT NOT NULL," +
                "    player2 TEXT NOT NULL," +
                "    winner TEXT," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            System.out.println("Database initialized at: " + DB_PATH);
            
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

}
