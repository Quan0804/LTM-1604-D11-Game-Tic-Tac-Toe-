package server;

import database.SQLiteDatabaseManager;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private SQLiteDatabaseManager databaseManager;
    private List<ClientHandler> clients;
    private List<GameSession> gameSessions;
    private boolean running;
    
    public Server() {
        this.databaseManager = new SQLiteDatabaseManager();
        this.clients = new ArrayList<>();
        this.gameSessions = new ArrayList<>();
        this.running = false;
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Server đang chạy trên port " + PORT);
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client mới kết nối: " + clientSocket.getInetAddress());
                
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                System.out.println("Đã thêm client vào danh sách. Tổng số clients: " + clients.size()); // Debug log
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public SQLiteDatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public List<ClientHandler> getClients() {
        return clients;
    }
    
    public List<GameSession> getGameSessions() {
        return gameSessions;
    }
    
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Đã remove client: " + client.getUsername() + ". Tổng số clients còn lại: " + clients.size()); // Debug log
    }
    
    public void addGameSession(GameSession session) {
        gameSessions.add(session);
    }
    
    public void removeGameSession(GameSession session) {
        gameSessions.remove(session);
    }
    
    public List<String> getOnlinePlayers() {
        List<String> onlinePlayers = new ArrayList<>();
        System.out.println("Server.getOnlinePlayers() - Tổng số clients: " + clients.size()); // Debug log
        for (ClientHandler client : clients) {
            System.out.println("Client: " + client.getUsername() + ", loggedIn: " + client.isLoggedIn() + ", inGame: " + client.isInGame()); // Debug log
            if (client.isLoggedIn() && !client.isInGame()) {
                onlinePlayers.add(client.getUsername());
            }
        }
        System.out.println("Danh sách online players trả về: " + onlinePlayers); // Debug log
        return onlinePlayers;
    }
    
    // Gửi danh sách người chơi online hiện tại tới tất cả client
    public void broadcastOnlinePlayers() {
        List<String> onlinePlayers = getOnlinePlayers();
        StringBuilder response = new StringBuilder("ONLINE_PLAYERS:");
        for (String player : onlinePlayers) {
            response.append(player).append(",");
        }
        String msg = response.toString();
        System.out.println("Server broadcast ONLINE_PLAYERS: " + msg + " tới " + clients.size() + " clients"); // Debug log
        System.out.println("Danh sách online players: " + onlinePlayers); // Debug log
        
        for (ClientHandler client : clients) {
            try {
                if (client.isLoggedIn()) {
                    client.getWriter().println(msg);
                    client.getWriter().flush(); // Đảm bảo message được gửi ngay
                    System.out.println("Đã gửi tới client: " + client.getUsername()); // Debug log
                }
            } catch (Exception e) {
                System.out.println("Lỗi gửi tới client " + client.getUsername() + ": " + e.getMessage()); // Debug log
            }
        }
    }
    
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
