package server;

import database.SQLiteDatabaseManager;

public class GameSession {
    private ClientHandler player1;
    private ClientHandler player2;
    private char[][] board;
    private char currentPlayer;
    private boolean gameEnded;
    private Server server;
    
    public GameSession(ClientHandler player1, ClientHandler player2, Server server) {
        this.player1 = player1;
        this.player2 = player2;
        this.server = server;
        this.board = new char[3][3];
        this.currentPlayer = 'X';
        this.gameEnded = false;
        
        // Khởi tạo board
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
        
        player1.setInGame(true);
        player2.setInGame(true);
        
        System.out.println("Tạo GameSession: " + player1.getUsername() + " vs " + player2.getUsername() + ", lượt đầu: " + currentPlayer); // Debug log
    }
    
    public void makeMove(ClientHandler player, int position) {
        if (gameEnded) return;
        
        int row = position / 3;
        int col = position % 3;
        
        // Kiểm tra lượt chơi
        if ((currentPlayer == 'X' && player != player1) || 
            (currentPlayer == 'O' && player != player2)) {
            player.getWriter().println("INVALID_TURN");
            return;
        }
        
        // Kiểm tra ô đã được đánh chưa
        if (board[row][col] != ' ') {
            player.getWriter().println("INVALID_MOVE");
            return;
        }
        
        // Đánh dấu trên board
        board[row][col] = currentPlayer;
        
        // Gửi move đến cả hai player
        String moveMessage = "MOVE:" + position + ":" + currentPlayer;
        System.out.println("GameSession gửi move: " + moveMessage); // Debug log
        player1.getWriter().println(moveMessage);
        player1.getWriter().flush();
        player2.getWriter().println(moveMessage);
        player2.getWriter().flush();
        
        // Kiểm tra kết thúc game
        if (checkWin()) {
            endGame();
            String winner = (currentPlayer == 'X') ? player1.getUsername() : player2.getUsername();
            
            // Gửi kết quả cho từng người: người thắng nhận WIN, người thua nhận LOSE
            if (winner.equals(player1.getUsername())) {
                player1.getWriter().println("GAME_END:WIN:" + winner);
                player1.getWriter().flush();
                player2.getWriter().println("GAME_END:LOSE:" + winner);
                player2.getWriter().flush();
            } else {
                player1.getWriter().println("GAME_END:LOSE:" + winner);
                player1.getWriter().flush();
                player2.getWriter().println("GAME_END:WIN:" + winner);
                player2.getWriter().flush();
            }
            
            // Lưu kết quả vào database
            SQLiteDatabaseManager db = server.getDatabaseManager();
            db.saveGameResult(player1.getUsername(), player2.getUsername(), winner);
        } else if (checkDraw()) {
            endGame();
            // Gửi đủ 3 phần để client luôn parse an toàn (winner placeholder)
            player1.getWriter().println("GAME_END:DRAW:NONE");
            player1.getWriter().flush();
            player2.getWriter().println("GAME_END:DRAW:NONE");
            player2.getWriter().flush();
        } else {
            // Chuyển lượt
            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
            System.out.println("Chuyển lượt sang: " + currentPlayer); // Debug log
            
            // Gửi thông tin lượt tiếp theo
            String turnMessage = "TURN:" + currentPlayer;
            System.out.println("GameSession gửi turn: " + turnMessage); // Debug log
            player1.getWriter().println(turnMessage);
            player1.getWriter().flush();
            player2.getWriter().println(turnMessage);
            player2.getWriter().flush();
        }
    }
    
    private boolean checkWin() {
        // Kiểm tra hàng ngang
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == currentPlayer && board[i][1] == currentPlayer && board[i][2] == currentPlayer) {
                return true;
            }
        }
        
        // Kiểm tra hàng dọc
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == currentPlayer && board[1][j] == currentPlayer && board[2][j] == currentPlayer) {
                return true;
            }
        }
        
        // Kiểm tra đường chéo
        if (board[0][0] == currentPlayer && board[1][1] == currentPlayer && board[2][2] == currentPlayer) {
            return true;
        }
        if (board[0][2] == currentPlayer && board[1][1] == currentPlayer && board[2][0] == currentPlayer) {
            return true;
        }
        
        return false;
    }
    
    private boolean checkDraw() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
    
    public void endGame() {
        gameEnded = true;
        player1.setInGame(false);
        player2.setInGame(false);
        player1.setCurrentGame(null);
        player2.setCurrentGame(null);
        server.removeGameSession(this);
        // Sau khi kết thúc, đưa hai người chơi trở lại danh sách online
        server.broadcastOnlinePlayers();
    }
    
    public ClientHandler getPlayer1() {
        return player1;
    }
    
    public ClientHandler getPlayer2() {
        return player2;
    }
}
