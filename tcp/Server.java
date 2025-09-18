package tcp;

import java.io.*;
import java.net.*;
import java.util.*;

import com.mongodb.client.*;
import mongodb.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

public class Server {
    private static char[][] board = new char[3][3];
    private static List<PrintWriter> clients = new ArrayList<>();
    private static char currentPlayer = 'X';
    private static int playerCount = 0;

    // MongoDB
    private static MongoDatabase db = MongoDBConnection.getDatabase();
    private static MongoCollection<Document> playersCol = db.getCollection("players");
    private static MongoCollection<Document> matchesCol = db.getCollection("matches");
    private static MongoCollection<Document> movesCol = db.getCollection("moves");

    // Match hiện tại
    private static ObjectId currentMatchId = null;

    public static void main(String[] args) {
        resetBoard();

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("✅ Server đang chạy trên port 5000...");

            while (true) {
                Socket socket = serverSocket.accept();
                playerCount++;
                char playerSymbol = (playerCount == 1) ? 'X' : 'O';

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                clients.add(out);

                // Gửi thông tin player về client
                out.println("WELCOME " + playerSymbol);
                sendBoard();
                out.println("TURN " + currentPlayer);

                savePlayer(playerSymbol);

                // Khi có đủ 2 player thì tạo match mới
                if (playerCount == 2 && currentMatchId == null) {
                    currentMatchId = createMatch('X', 'O');
                    System.out.println("🎮 Trận mới bắt đầu: " + currentMatchId.toHexString());
                }

                new Thread(new ClientHandler(socket, out, playerSymbol)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi khởi động server: " + e.getMessage());
        }
    }

    // Reset bàn cờ
    private static void resetBoard() {
        for (int i = 0; i < 3; i++) {
            Arrays.fill(board[i], ' ');
        }
        currentPlayer = 'X';
    }

    // Gửi bàn cờ cho tất cả client
    private static void sendBoard() {
        for (PrintWriter client : clients) {
            client.println("BOARD");
            for (int i = 0; i < 3; i++) {
                client.println(board[i][0] + " | " + board[i][1] + " | " + board[i][2]);
            }
        }
    }

    // Gửi thông báo cho tất cả client
    private static void broadcast(String msg) {
        for (PrintWriter client : clients) {
            client.println(msg);
        }
    }

    // Kiểm tra thắng
    private static boolean checkWin(char symbol) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) return true;
            if (board[0][i] == symbol && board[1][i] == symbol && board[2][i] == symbol) return true;
        }
        if (board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) return true;
        if (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) return true;
        return false;
    }

    // Kiểm tra hòa
    private static boolean isFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') return false;
            }
        }
        return true;
    }

    // Kết thúc trận
    private static void endGame(char winner) {
        if (winner == ' ') {
            broadcast("DRAW");
            endMatch(currentMatchId, ' ');
            System.out.println("🤝 Trận hòa!");
        } else {
            broadcast("WINNER " + winner);
            endMatch(currentMatchId, winner);
            System.out.println("🏆 Người thắng: " + winner);
        }

        // Reset game cho ván mới
        resetBoard();
        currentMatchId = null;
        broadcast("RESET");
        sendBoard();
        broadcast("TURN " + currentPlayer);
        playerCount = 0;
        clients.clear();
    }

    // Lưu player vào DB
    private static void savePlayer(char symbol) {
        Document player = new Document("symbol", String.valueOf(symbol))
                .append("joinedAt", new Date());
        playersCol.insertOne(player);
        System.out.println("👤 Player " + symbol + " đã tham gia.");
    }

    // Tạo trận mới
    private static ObjectId createMatch(char player1, char player2) {
        Document match = new Document("player1", String.valueOf(player1))
                .append("player2", String.valueOf(player2))
                .append("status", "ongoing")
                .append("createdAt", new Date());
        matchesCol.insertOne(match);
        return match.getObjectId("_id");
    }

    // Lưu nước đi
    private static void saveMove(ObjectId matchId, char player, int row, int col) {
        Document move = new Document("matchId", matchId)
                .append("player", String.valueOf(player))
                .append("row", row)
                .append("col", col)
                .append("time", new Date());
        movesCol.insertOne(move);
        System.out.println("✅ Lưu move: " + player + " -> (" + row + "," + col + ")");
    }

    // Cập nhật kết quả trận trong DB
    private static void endMatch(ObjectId matchId, char winner) {
        matchesCol.updateOne(
            new Document("_id", matchId),
            new Document("$set", new Document("status", "finished")
                    .append("winner", (winner == ' ' ? "draw" : String.valueOf(winner)))
                    .append("endedAt", new Date()))
        );
    }

    // Thread xử lý client
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private char playerSymbol;

        public ClientHandler(Socket socket, PrintWriter out, char playerSymbol) {
            this.socket = socket;
            this.out = out;
            this.playerSymbol = playerSymbol;
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.err.println("❌ Lỗi khi tạo luồng đọc cho player " + playerSymbol);
            }
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("MOVE")) {
                        String[] parts = line.split(" ");
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);

                        synchronized (board) {
                            if (currentPlayer != playerSymbol) {
                                out.println("NOT_YOUR_TURN");
                                continue;
                            }
                            if (board[row][col] != ' ') {
                                out.println("INVALID_MOVE");
                                continue;
                            }

                            // Ghi nước đi vào board
                            board[row][col] = playerSymbol;

                            // Lưu move vào MongoDB
                            if (currentMatchId != null) {
                                saveMove(currentMatchId, playerSymbol, row, col);
                            }

                            // Check thắng/thua/hòa
                            if (checkWin(playerSymbol)) {
                                sendBoard();
                                endGame(playerSymbol);
                                continue;
                            } else if (isFull()) {
                                sendBoard();
                                endGame(' ');
                                continue;
                            }

                            // Chuyển lượt
                            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
                            sendBoard();
                            broadcast("TURN " + currentPlayer);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Client " + playerSymbol + " bị ngắt kết nối");
            }
        }
    }
}
