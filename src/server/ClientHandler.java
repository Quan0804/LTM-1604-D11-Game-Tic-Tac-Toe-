package server;

import database.SQLiteDatabaseManager;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Server server;
    private String username;
    private boolean loggedIn;
    private boolean inGame;
    private GameSession currentGame;
    // Rematch coordination
    private String lastOpponent;
    private String lastChallenger; // username của người X ở ván trước
    private boolean wantsRematch;
    
    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.loggedIn = false;
        this.inGame = false;
        
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }
    
    private void handleMessage(String message) {
        System.out.println("Server nhận message từ " + username + ": " + message);
        String[] parts = message.split(":");
        String command = parts[0];
        
        System.out.println("Server xử lý command: " + command + ", parts.length=" + parts.length);
        switch (command) {
            case "LOGIN":
                handleLogin(parts[1], parts[2]);
                break;
            case "REGISTER":
                handleRegister(parts[1], parts[2]);
                break;
            case "GET_ONLINE_PLAYERS":
                sendOnlinePlayers();
                break;
            case "CHALLENGE":
                handleChallenge(parts[1]);
                break;
            case "ACCEPT_CHALLENGE":
                handleAcceptChallenge(parts[1]);
                break;
            case "DECLINE_CHALLENGE":
                handleDeclineChallenge(parts[1]);
                break;
            case "REMATCH_REQUEST":
                handleRematchRequest(parts.length > 1 ? parts[1] : "");
                break;
            case "REMATCH_DECLINE":
                handleRematchDecline(parts.length > 1 ? parts[1] : "");
                break;
            case "LEAVE_GAME":
                handleLeaveGame(parts.length > 1 ? parts[1] : "");
                break;
            case "MOVE":
                System.out.println("Server gọi handleMove với position=" + parts[1] + ", player=" + parts[2]);
                handleMove(parts[1], parts[2]);
                break;
            case "GET_LEADERBOARD":
                sendLeaderboard();
                break;
            case "GET_RECENT_GAMES":
                sendRecentGames();
                break;
            case "DISCONNECT":
                disconnect();
                break;
        }
    }
    
    private void handleLogin(String username, String password) {
        SQLiteDatabaseManager db = server.getDatabaseManager();
        if (db.loginUser(username, password)) {
            this.username = username;
            this.loggedIn = true;
            writer.println("LOGIN_SUCCESS");
            writer.flush(); // Đảm bảo LOGIN_SUCCESS được gửi ngay
            System.out.println("User " + username + " đã đăng nhập");
            
            // Gửi danh sách online players cho client vừa đăng nhập
            sendOnlinePlayers();
            
            // Cập nhật danh sách online cho tất cả client khác
            server.broadcastOnlinePlayers();
        } else {
            writer.println("LOGIN_FAILED");
            writer.flush();
        }
    }
    
    private void handleRegister(String username, String password) {
        SQLiteDatabaseManager db = server.getDatabaseManager();
        if (db.registerUser(username, password)) {
            writer.println("REGISTER_SUCCESS");
        } else {
            writer.println("REGISTER_FAILED");
        }
    }
    
    private void sendOnlinePlayers() {
        List<String> onlinePlayers = server.getOnlinePlayers();
        StringBuilder response = new StringBuilder("ONLINE_PLAYERS:");
        for (String player : onlinePlayers) {
            response.append(player).append(",");
        }
        String message = response.toString();
        System.out.println("Server gửi ONLINE_PLAYERS cho " + username + ": " + message); // Debug log
        writer.println(message);
        writer.flush(); // Đảm bảo message được gửi ngay
    }
    
    private void handleChallenge(String targetUsername) {
        System.out.println("Server nhận challenge từ " + username + " tới " + targetUsername);
        ClientHandler targetClient = findClientByUsername(targetUsername);
        if (targetClient != null && !targetClient.isInGame()) {
            System.out.println("Server gửi CHALLENGE tới " + targetUsername);
            targetClient.writer.println("CHALLENGE:" + username);
            targetClient.writer.flush();
            writer.println("CHALLENGE_SENT");
            writer.flush();
        } else {
            System.out.println("Challenge failed - target client không tìm thấy hoặc đang trong game");
            writer.println("CHALLENGE_FAILED");
            writer.flush();
        }
    }
    
    private void handleAcceptChallenge(String challengerUsername) {
        System.out.println("Server nhận ACCEPT_CHALLENGE từ " + username + " cho " + challengerUsername);
        ClientHandler challenger = findClientByUsername(challengerUsername);
        if (challenger != null && !challenger.isInGame()) {
            // Tạo game session mới
            System.out.println("Server tạo GameSession giữa " + challengerUsername + " và " + username);
            GameSession gameSession = new GameSession(challenger, this, server);
            server.addGameSession(gameSession);
            
            challenger.setCurrentGame(gameSession);
            this.setCurrentGame(gameSession);
            // Ghi nhớ thông tin cho rematch
            challenger.lastOpponent = this.username;
            this.lastOpponent = challengerUsername;
            challenger.lastChallenger = challengerUsername;
            this.lastChallenger = challengerUsername;
            challenger.wantsRematch = false;
            this.wantsRematch = false;
            System.out.println("Server đã set currentGame cho cả 2 client");
            
            // Gửi thông tin game start với thông tin về lượt đầu tiên
            // Challenger (người thách đấu) là X, Accepter (người chấp nhận) là O
            String challengerMessage = "GAME_START:" + username + ":X";
            String accepterMessage = "GAME_START:" + challengerUsername + ":O";
            
            System.out.println("Server gửi GAME_START cho challenger (" + challengerUsername + "): " + challengerMessage);
            System.out.println("  -> Challenger " + challengerUsername + " sẽ là X và đánh trước");
            try {
                challenger.writer.println(challengerMessage);
                challenger.writer.flush();
                System.out.println("Server đã gửi GAME_START thành công cho challenger");
            } catch (Exception e) {
                System.out.println("Server lỗi gửi GAME_START cho challenger: " + e.getMessage());
            }
            
            System.out.println("Server gửi GAME_START cho accepter (" + username + "): " + accepterMessage);
            System.out.println("  -> Accepter " + username + " sẽ là O và đánh sau");
            try {
                this.writer.println(accepterMessage);
                this.writer.flush();
                System.out.println("Server đã gửi GAME_START thành công cho accepter");
            } catch (Exception e) {
                System.out.println("Server lỗi gửi GAME_START cho accepter: " + e.getMessage());
            }
            
            // Gửi thông tin lượt đánh đầu tiên (X luôn đi trước)
            try {
                String firstTurnMsg = "TURN:X";
                System.out.println("Server gửi TURN đầu tiên cho cả hai client: " + firstTurnMsg);
                challenger.writer.println(firstTurnMsg);
                challenger.writer.flush();
                this.writer.println(firstTurnMsg);
                this.writer.flush();
            } catch (Exception e) {
                System.out.println("Server lỗi gửi TURN đầu tiên: " + e.getMessage());
            }

            System.out.println("Game bắt đầu giữa " + challengerUsername + " (X - thách đấu) và " + username + " (O - chấp nhận)");
            // Cập nhật lại danh sách online (hai người vừa rời lobby)
            server.broadcastOnlinePlayers();
        }
    }

    private void handleRematchRequest(String opponentName) {
        // Đồng bộ để tránh 2 thread xử lý song song dẫn tới cả hai cùng nhận WAITING
        synchronized (server) {
            this.wantsRematch = true;
            ClientHandler opponent = findClientByUsername(opponentName);
            if (opponent != null) {
                System.out.println("=== REMATCH_REQUEST ===");
                System.out.println("Từ: " + this.username + " → Tới: " + opponentName);
                System.out.println("opponent.wantsRematch = " + opponent.wantsRematch);
                System.out.println("opponent.lastOpponent = " + opponent.lastOpponent);
                System.out.println("this.username = " + this.username);
                
                // Nếu đối thủ cũng đã muốn rematch và đúng cặp đối thủ trước đó => bắt đầu ngay
                if (opponent.wantsRematch && opponent.lastOpponent != null && opponent.lastOpponent.equals(this.username)) {
                    System.out.println(">>> CẢ HAI ĐÃ ĐỒNG Ý! Bắt đầu rematch...");
                    // Giữ nguyên vai X theo lastChallenger
                    ClientHandler challenger = findClientByUsername(this.lastChallenger);
                    ClientHandler accepter = challenger == null ? opponent : (challenger == this ? opponent : this);
                    if (challenger == null) {
                        // fallback: người gửi hiện tại là X
                        challenger = this;
                        accepter = opponent;
                    }
                    System.out.println("Challenger (X): " + challenger.getUsername() + ", Accepter (O): " + accepter.getUsername());
                    
                    GameSession gameSession = new GameSession(challenger, accepter, server);
                    server.addGameSession(gameSession);
                    challenger.setCurrentGame(gameSession);
                    accepter.setCurrentGame(gameSession);
                    // Ghi nhớ thông tin đối thủ và người X cho lần rematch tiếp theo
                    challenger.lastOpponent = accepter.getUsername();
                    accepter.lastOpponent = challenger.getUsername();
                    challenger.lastChallenger = challenger.getUsername();
                    accepter.lastChallenger = challenger.getUsername();
                    // Reset rematch flags
                    this.wantsRematch = false;
                    opponent.wantsRematch = false;
                    
                    // Thông báo bắt đầu rematch (client sẽ reset UI)
                    System.out.println("Gửi REMATCH_START cho challenger: " + challenger.getUsername());
                    challenger.getWriter().println("REMATCH_START:" + accepter.getUsername() + ":X");
                    challenger.getWriter().flush();
                    
                    System.out.println("Gửi REMATCH_START cho accepter: " + accepter.getUsername());
                    accepter.getWriter().println("REMATCH_START:" + challenger.getUsername() + ":O");
                    accepter.getWriter().flush();
                    
                    // Gửi turn đầu tiên
                    System.out.println("Gửi TURN:X cho cả hai");
                    challenger.getWriter().println("TURN:X");
                    challenger.getWriter().flush();
                    accepter.getWriter().println("TURN:X");
                    accepter.getWriter().flush();
                    
                    // Cập nhật lobby
                    server.broadcastOnlinePlayers();
                    System.out.println("=== REMATCH HOÀN TẤT ===");
                } else {
                    System.out.println(">>> Chưa đủ 2 bên đồng ý");
                    // Chưa đủ 2 bên đồng ý => CHỈ gửi offer nếu đối thủ chưa bấm rematch
                    if (!opponent.wantsRematch) {
                        System.out.println("Gửi REMATCH_OFFER tới " + opponent.getUsername() + " từ " + this.username);
                        opponent.getWriter().println("REMATCH_OFFER:" + this.username);
                        opponent.getWriter().flush();
                    } else {
                        System.out.println("Đối thủ đã muốn rematch nhưng chưa khớp điều kiện lastOpponent");
                    }
                    this.getWriter().println("REMATCH_WAITING");
                    this.getWriter().flush();
                }
            }
        }
    }
    
    private void handleRematchDecline(String opponentName) {
        this.wantsRematch = false;
        ClientHandler opponent = findClientByUsername(opponentName);
        if (opponent != null) {
            opponent.getWriter().println("REMATCH_DECLINED:" + this.username);
        }
    }
    
    private void handleLeaveGame(String opponentName) {
        ClientHandler opponent = findClientByUsername(opponentName);
        if (opponent != null) {
            opponent.getWriter().println("OPPONENT_LEFT");
        }
        // Reset trạng thái của người này
        this.inGame = false;
        this.currentGame = null;
        this.wantsRematch = false;
    }
    
    private void handleDeclineChallenge(String challengerUsername) {
        ClientHandler challenger = findClientByUsername(challengerUsername);
        if (challenger != null) {
            challenger.writer.println("CHALLENGE_DECLINED:" + username);
        }
    }
    
    private void handleMove(String position, String player) {
        System.out.println("ClientHandler handleMove: position=" + position + ", player=" + player + ", currentGame=" + (currentGame != null ? "NOT NULL" : "NULL")); // Debug log
        if (currentGame != null) {
            System.out.println("ClientHandler gọi currentGame.makeMove()"); // Debug log
            currentGame.makeMove(this, Integer.parseInt(position));
        } else {
            System.out.println("ERROR: currentGame is NULL! Cannot make move."); // Debug log
        }
    }
    
    private void sendLeaderboard() {
        SQLiteDatabaseManager db = server.getDatabaseManager();
        String leaderboard = db.getLeaderboard();
        writer.println("LEADERBOARD:" + leaderboard);
    }
    
    private void sendRecentGames() {
        SQLiteDatabaseManager db = server.getDatabaseManager();
        String recentGames = db.getRecentGames();
        writer.println("RECENT_GAMES:" + recentGames);
    }
    
    private ClientHandler findClientByUsername(String username) {
        for (ClientHandler client : server.getClients()) {
            if (client.getUsername() != null && client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }
    
    public void disconnect() {
        try {
            if (currentGame != null) {
                currentGame.endGame();
            }
            server.removeClient(this);
            socket.close();
            System.out.println("Client " + username + " đã ngắt kết nối");
            server.broadcastOnlinePlayers();
            wantsRematch = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Getters và Setters
    public String getUsername() {
        return username;
    }
    
    public boolean isLoggedIn() {
        return loggedIn;
    }
    
    public boolean isInGame() {
        return inGame;
    }
    
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }
    
    public void setCurrentGame(GameSession game) {
        this.currentGame = game;
        this.inGame = (game != null);
    }
    
    public PrintWriter getWriter() {
        return writer;
    }
}
