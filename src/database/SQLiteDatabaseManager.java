package database;

import java.sql.*;

public class SQLiteDatabaseManager {
    
    public SQLiteDatabaseManager() {
        // Force load Database class to initialize DB
        try {
            Database.getConnection().close();
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
    
    // Đăng ký người dùng mới
    public boolean registerUser(String username, String password) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users(username, password) VALUES(?, ?)")) {
            
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                return false; // Username đã tồn tại
            }
            e.printStackTrace();
            return false;
        }
    }
    
    // Đăng nhập
    public boolean loginUser(String username, String password) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM users WHERE username = ? AND password = ?")) {
            
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next();
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Lưu kết quả trận đấu
    public void saveGameResult(String player1, String player2, String winner) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Lưu trận đấu
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO games(player1, player2, winner) VALUES(?, ?, ?)")) {
                    ps.setString(1, player1);
                    ps.setString(2, player2);
                    ps.setString(3, winner); // null nếu hòa
                    ps.executeUpdate();
                }
                
                // Cập nhật thống kê nếu có người thắng
                if (winner != null && !winner.isEmpty()) {
                    // Tăng wins cho người thắng
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET wins = wins + 1 WHERE username = ?")) {
                        ps.setString(1, winner);
                        ps.executeUpdate();
                    }
                    
                    // Tăng losses cho người thua
                    String loser = winner.equals(player1) ? player2 : player1;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET losses = losses + 1 WHERE username = ?")) {
                        ps.setString(1, loser);
                        ps.executeUpdate();
                    }
                }
                
                conn.commit();
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Lấy bảng xếp hạng
    public String getLeaderboard() {
        StringBuilder sb = new StringBuilder();
        
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT username, wins, losses FROM users ORDER BY wins DESC, losses ASC LIMIT 10")) {
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString("username");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                
                if (sb.length() > 0) {
                    sb.append(";");
                }
                sb.append(username).append(",").append(wins).append(",").append(losses);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return sb.toString();
    }
    
    // Lấy lịch sử trận đấu gần đây
    public String getRecentGames() {
        StringBuilder sb = new StringBuilder();
        
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT player1, player2, winner FROM games ORDER BY created_at DESC LIMIT 50")) {
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String player1 = rs.getString("player1");
                String player2 = rs.getString("player2");
                String winner = rs.getString("winner");
                
                if (sb.length() > 0) {
                    sb.append(";");
                }
                sb.append(player1).append(",").append(player2).append(",");
                if (winner == null || winner.isEmpty()) {
                    sb.append("Hòa");
                } else {
                    sb.append(winner);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return sb.toString();
    }
}
