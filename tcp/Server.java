package tcp;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static char[][] board = new char[3][3];
    private static List<PrintWriter> clients = new ArrayList<>();
    private static char currentPlayer = 'X'; // X luôn đi trước
    private static int playerCount = 0;

    public static void main(String[] args) throws Exception {
        resetBoard();

        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server đang chạy...");

        while (true) {
            Socket socket = serverSocket.accept();
            playerCount++;
            char playerSymbol = (playerCount == 1) ? 'X' : 'O';

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            clients.add(out);

            out.println("WELCOME " + playerSymbol);
            sendBoard();
            out.println("TURN " + currentPlayer);

            new Thread(new ClientHandler(socket, out, playerSymbol)).start();
        }
    }

    // Reset bàn cờ
    private static void resetBoard() {
        for (int i = 0; i < 3; i++) {
            Arrays.fill(board[i], ' ');
        }
        currentPlayer = 'X'; // reset lại cho X đi trước
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

    // Gửi message cho tất cả client
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

    // Kết thúc game
    private static void endGame(char winner) {
        if (winner == ' ') {
            broadcast("DRAW"); // thông báo hòa
        } else {
            broadcast("WINNER " + winner); // thông báo thắng
        }

        // Reset lại game
        resetBoard();
        broadcast("RESET"); 
        sendBoard();
        broadcast("TURN " + currentPlayer);
    }

    // Thread xử lý từng client
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
                e.printStackTrace();
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
                                out.println("NOT_YOUR_TURN"); // chưa tới lượt
                                continue;
                            }
                            if (board[row][col] != ' ') {
                                out.println("INVALID_MOVE"); // ô đã được đánh
                                continue;
                            }

                            // cập nhật nước đi
                            board[row][col] = playerSymbol;

                            // kiểm tra thắng/thua/hòa
                            if (checkWin(playerSymbol)) {
                                sendBoard();
                                endGame(playerSymbol);
                                continue;
                            } else if (isFull()) {
                                sendBoard();
                                endGame(' ');
                                continue;
                            }

                            // đổi lượt
                            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
                            sendBoard();
                            broadcast("TURN " + currentPlayer);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
