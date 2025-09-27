package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 12345;
    
    // Hằng số MỚI: Số lượng người chơi tối đa được hiển thị trong bảng xếp hạng
    public static final int TOP_PLAYERS_LIMIT = 10; 
    
    public static List<ClientHandler> clientHandlers = new ArrayList<>();
    private static Game game = new Game();
    private static int readyPlayers = 0; 
    private static final Object lock = new Object(); 

    // Biến để theo dõi người chơi nào đi trước trong vòng tiếp theo (luân phiên)
    private static char nextStartingPlayer = 'X'; 
    
    // BIẾN MỚI: Theo dõi số lượng người chơi đã gửi yêu cầu RESTART
    private static volatile int restartCount = 0; 

    public static void main(String[] args) {
        ServerSocket serverSocket = null; 
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);
            
            // Server luôn sẵn sàng nhận 2 kết nối, nhưng game chỉ bắt đầu khi 2 người đăng nhập
            while (clientHandlers.size() < 2) { 
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                ClientHandler clientHandler = new ClientHandler(socket, game);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
            System.out.println("Max clients reached (2). Waiting for both players to log in...");
            
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Phương thức được ClientHandler gọi sau khi người chơi đăng nhập thành công.
     * Khi đủ 2 người chơi đăng nhập, game sẽ bắt đầu.
     */
    public static void playerReady() {
        synchronized (lock) {
            readyPlayers++;
            if (readyPlayers == 2) {
                setupNewGame();
            }
        }
    }

    /**
     * Thiết lập vai trò (X/O) cho 2 client và bắt đầu vòng chơi đầu tiên.
     */
    private static void setupNewGame() {
        System.out.println("Two players are ready. Setting up roles and starting game.");
        
        // BƯỚC 1: Thiết lập lượt đi trong Game Object
        game.setCurrentPlayer(nextStartingPlayer); 
        
        // BƯỚC 2: Gửi lệnh START và NEW_ROUND cho từng Client
        if (clientHandlers.size() >= 2) {
            // Client 1 (người đi trước ban đầu)
            ClientHandler clientX = clientHandlers.get(0);
            clientX.sendMessage("START X"); 
            // Gửi NEW_ROUND để bắt đầu lượt đi
            clientX.sendMessage("NEW_ROUND " + nextStartingPlayer); 
            
            // Client 2
            ClientHandler clientO = clientHandlers.get(1);
            clientO.sendMessage("START O"); 
            // Gửi NEW_ROUND để bắt đầu lượt đi
            clientO.sendMessage("NEW_ROUND " + nextStartingPlayer); 
        }
        
        // BƯỚC 3: Chuẩn bị cho vòng tiếp theo (luân phiên)
        nextStartingPlayer = (nextStartingPlayer == 'X') ? 'O' : 'X';
    }
    
    /**
     * Thông báo cho tất cả client rằng một vòng chơi mới bắt đầu.
     */
    public static void notifyNewRound() {
        System.out.println("New round started! " + nextStartingPlayer + " goes first.");
        
        broadcastMessage("NEW_ROUND " + nextStartingPlayer, null); 
        
        // Cập nhật người đi đầu cho vòng tiếp theo
        nextStartingPlayer = (nextStartingPlayer == 'X') ? 'O' : 'X';
    }


     /**
      * Đặt lại trạng thái trò chơi về mặc định và thông báo vòng mới.
      * Phương thức này được gọi từ handleRestartRequest().
      */
     public static void resetGame() {
        game.reset(); // Chỉ reset bàn cờ
        
        // Đặt người đi hiện tại của game bằng người đi đầu vòng này
        game.setCurrentPlayer(nextStartingPlayer); 
        
        // LUÔN YÊU CẦU CẬP NHẬT STATS VÀ GỬI NEW_ROUND KHI CHƠI LẠI
        broadcastStats(); 
        notifyNewRound(); 
     }

    /**
     * Xử lý đồng bộ yêu cầu chơi lại (RESTART) từ ClientHandler.
     */
    public static synchronized void handleRestartRequest() {
        restartCount++;
        
        int requiredPlayers = clientHandlers.size(); 
        
        if (restartCount == requiredPlayers) {
            // Cả hai người chơi đều muốn chơi lại
            System.out.println("Both players requested restart. Starting new round.");
            
            resetGame(); 
            
            restartCount = 0;
        }
    }


    /**
     * Gửi tin nhắn tới tất cả client (trừ client ngoại lệ).
     */
    public static void broadcastMessage(String message, ClientHandler excludeUser) {
        for (ClientHandler aClient : clientHandlers) {
            if (aClient != excludeUser) {
                aClient.sendMessage(message);
            }
        }
    }
    
    /**
     * Yêu cầu tất cả ClientHandler gửi lại dữ liệu thống kê.
     */
    public static void broadcastStats() {
         for (ClientHandler aClient : clientHandlers) {
            aClient.sendGameStats(); 
        }
    }
}