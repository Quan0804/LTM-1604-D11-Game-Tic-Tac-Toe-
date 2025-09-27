package server;

public class Game {
    private char[][] board = new char[3][3];
    // Sửa: Biến currentPlayer sẽ được ClientHandler sử dụng để kiểm tra, 
    // và được Server.notifyNewRound() đặt lại thông qua lệnh MOVE thành công.
    private char currentPlayer = 'X'; 
    
    // Biến lưu trữ tọa độ 3 ô chiến thắng (ví dụ: "0 0 0 1 0 2")
    private String winningLine = null; 
    
    // Lưu trữ nước đi gần nhất để kiểm tra người thắng
    private int latestMoveRow = -1;
    private int latestMoveCol = -1;

    public Game() {
        reset(); 
    }

    /**
     * Phương thức reset bàn cờ. 
     * LƯU Ý QUAN TRỌNG: KHÔNG ĐẶT LẠI currentPlayer VỀ 'X' CỐ ĐỊNH Ở ĐÂY.
     * Lượt đi đầu tiên của vòng mới được điều khiển bởi Server.nextStartingPlayer 
     * và được thiết lập khi Server gửi lệnh NEW_ROUND.
     */
    public synchronized void reset() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = '-'; // Đặt lại về trạng thái trống
            }
        }
        // SỬA LỖI QUAN TRỌNG: XÓA DÒNG NÀY ĐỂ CHO PHÉP SERVER ĐẶT LƯỢT ĐI ĐẦU TIÊN THEO LOGIC LUÂN PHIÊN.
        // currentPlayer = 'X'; 
        
        winningLine = null; // Reset đường thắng
        latestMoveRow = -1; // Reset nước đi cuối cùng
        latestMoveCol = -1;
    }

    // Phương thức setter cho nước đi cuối cùng
    public void setLatestMove(int row, int col) {
        this.latestMoveRow = row;
        this.latestMoveCol = col;
    }
    
    // PHƯƠNG THỨC MỚI: Cho phép Server thiết lập currentPlayer khi bắt đầu vòng mới
    public void setCurrentPlayer(char player) {
        this.currentPlayer = player;
    }

    public synchronized boolean makeMove(int row, int col, char player) {
        // Kiểm tra hợp lệ: tọa độ, ô trống, và ĐÚNG LƯỢT CHƠI
        if (row >= 0 && row < 3 && col >= 0 && col < 3 && board[row][col] == '-' && player == currentPlayer) {
            board[row][col] = player;
            // Chuyển lượt đi
            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
            return true;
        }
        return false;
    }

    /**
     * Phương thức tìm đường thắng, trả về true nếu có người thắng.
     * Cập nhật biến winningLine với tọa độ.
     */
    public boolean checkWinner() {
        // Chỉ kiểm tra nếu có nước đi mới được thực hiện
        if (latestMoveRow == -1 || latestMoveCol == -1) return false;
        
        // Lấy quân cờ vừa đi 
        char player = board[latestMoveRow][latestMoveCol]; 
        
        // Kiểm tra Hàng (Row)
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) {
                // Tạo chuỗi tọa độ r1 c1 r2 c2 r3 c3
                winningLine = i + " 0 " + i + " 1 " + i + " 2";
                return true;
            }
        }
        // Kiểm tra Cột (Column)
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == player && board[1][j] == player && board[2][j] == player) {
                // Tạo chuỗi tọa độ r1 c1 r2 c2 r3 c3
                winningLine = "0 " + j + " 1 " + j + " 2 " + j;
                return true;
            }
        }
        // Kiểm tra Đường chéo chính (\)
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            // Tạo chuỗi tọa độ r1 c1 r2 c2 r3 c3
            winningLine = "0 0 1 1 2 2";
            return true;
        }
        // Kiểm tra Đường chéo phụ (/)
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            // Tạo chuỗi tọa độ r1 c1 r2 c2 r3 c3
            winningLine = "0 2 1 1 2 0";
            return true;
        }

        return false;
    }

    // Phương thức getter cho đường thắng
    public String getWinningLine() {
        return winningLine;
    }

    public boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == '-') {
                    return false;
                }
            }
        }
        return true;
    }
}