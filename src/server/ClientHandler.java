package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import database.MongoDBConnector;
import org.bson.Document;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Game game;
    private MongoDBConnector dbConnector;
    
    // BIẾN: Lưu trữ tên người dùng đã đăng nhập
    private String username; 
    
    public ClientHandler(Socket socket, Game game) {
        this.socket = socket;
        this.game = game;
        // Đảm bảo bạn đã khởi tạo MongoDBConnector
        this.dbConnector = new MongoDBConnector(); 

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Lỗi khi thiết lập stream cho client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tìm tên người dùng của đối thủ trong danh sách clientHandlers.
     */
    private String findOpponentUsername() {
        for (ClientHandler handler : Server.clientHandlers) {
            // Đảm bảo không phải client hiện tại và đã đăng nhập
            if (handler != this && handler.username != null) {
                return handler.username;
            }
        }
        return null; // Không tìm thấy đối thủ
    }

    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) { 
                String[] parts = line.split(" ");
                String command = parts[0];

                if (command.equals("LOGIN")) {
                    if (parts.length < 3) {
                        sendMessage("LOGIN_FAIL");
                        continue;
                    }
                    if (dbConnector.validateUser(parts[1], parts[2])) {
                        this.username = parts[1];
                        sendMessage("LOGIN_SUCCESS");
                        
                        // Server.playerReady() sẽ gọi setupNewGame, Client sẽ gọi GET_STATS sau khi GameFrame mở
                        Server.playerReady(); 
                    } else {
                        sendMessage("LOGIN_FAIL");
                    }
                } else if (command.equals("REGISTER")) {
                    if (parts.length < 3) {
                        sendMessage("REGISTER_FAIL");
                        continue;
                    }
                    if (dbConnector.registerUser(parts[1], parts[2])) {
                        sendMessage("REGISTER_SUCCESS");
                    } else {
                        sendMessage("REGISTER_FAIL");
                    }
                } else if (command.equals("MOVE")) {
                    if (parts.length < 4) {
                        sendMessage("INVALID_MOVE");
                        continue;
                    }
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    char player = parts[3].charAt(0);

                    game.setLatestMove(row, col); 

                    // game.makeMove() sẽ kiểm tra: 1. Ô trống 2. Đúng lượt (player == game.currentPlayer)
                    if (game.makeMove(row, col, player)) {
                        Server.broadcastMessage("VALID_MOVE " + row + " " + col + " " + player, null);
                        
                        if (game.checkWinner()) {
                            String opponentUsername = findOpponentUsername();
                            
                            // 1. Ghi nhận chiến thắng (WIN) và lịch sử (LOSE)
                            dbConnector.recordWin(username); // Người gửi MOVE thắng
                            if (opponentUsername != null) {
                                dbConnector.recordHistory(opponentUsername, "LOSE"); // Đối thủ thua
                            }
                            
                            // 2. Thông báo kết quả
                            String winLine = game.getWinningLine();
                            Server.broadcastMessage("WINNER " + player + " " + winLine, null);
                            
                            // 3. Yêu cầu cập nhật thống kê
                            Server.broadcastStats();
                            
                        } else if (game.isBoardFull()) {
                            String opponentUsername = findOpponentUsername();
                            
                            // 1. Ghi nhận hòa (TIE) cho cả hai người chơi
                            dbConnector.recordHistory(username, "TIE"); 
                            if (opponentUsername != null) {
                                dbConnector.recordHistory(opponentUsername, "TIE");
                            }

                            // 2. Thông báo kết quả
                            Server.broadcastMessage("TIE", null);

                            // 3. Yêu cầu cập nhật thống kê
                            Server.broadcastStats();
                        }
                    } else {
                        // Trả về INVALID_MOVE nếu: Sai lượt, ô đã đánh, hoặc tọa độ không hợp lệ.
                        sendMessage("INVALID_MOVE");
                    }
                } else if (command.equals("RESTART")) {
                    // Gọi logic xử lý RESTART từ Server (đảm bảo đồng bộ)
                    Server.handleRestartRequest(); 
                } 
                
                // THÊM LỆNH XỬ LÝ GET_STATS
                else if (command.equals("GET_STATS")) {
                    sendGameStats();
                }
            }
        } catch (IOException e) {
            System.out.println("A client disconnected or an error occurred: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // --- PHƯƠNG THỨC: GỬI THỐNG KÊ GAME ĐẾN CLIENT (ĐÃ FIX ĐỊNH DẠNG) ---
    public void sendGameStats() {
        // 1. Lấy Top Players
        List<Document> topPlayers = dbConnector.getTopPlayers(Server.TOP_PLAYERS_LIMIT);
        
        StringBuilder topPlayersData = new StringBuilder();
        for (int i = 0; i < topPlayers.size(); i++) {
            Document doc = topPlayers.get(i);
            
            // Lấy số trận thắng một cách an toàn
            Integer wins = doc.getInteger("wins");
            int winsValue = (wins != null) ? wins : 0;
            
            // FIX QUAN TRỌNG: Gửi rank (số thứ tự) cùng với tên và số trận thắng.
            // Rank là (i + 1)
            int rank = i + 1; 
            
            // Định dạng MỚI: <Rank> <Tên> <Số trận thắng>
            topPlayersData.append(rank).append(" ").append(doc.getString("username")).append(" ").append(winsValue).append(" ");
        }
        // Tin nhắn gửi đi: TOP_PLAYERS <số lượng> <Rank1> <Tên1> <Win1> <Rank2> <Tên2> <Win2> ...
        sendMessage("TOP_PLAYERS " + topPlayers.size() + " " + topPlayersData.toString().trim());

        // 2. Lấy Lịch sử Chiến thắng
        List<Document> recentHistory = dbConnector.getRecentHistory(10);
        StringBuilder historyData = new StringBuilder();
        for (Document doc : recentHistory) {
            // Định dạng: <Tên> <Kết quả> (Giữ nguyên)
            historyData.append(doc.getString("username")).append(" ").append(doc.getString("result")).append(" ");
        }
        sendMessage("RECENT_WINS " + recentHistory.size() + " " + historyData.toString().trim());
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        } else {
             System.out.println("Warning: Cannot send message, output stream is null.");
        }
    }
}