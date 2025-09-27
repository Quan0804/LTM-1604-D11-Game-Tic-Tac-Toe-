package client;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.Image; 

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GameFrame gameFrame;
    private LoginFrame loginFrame; 
    
    private char myMark = ' ';

    public Client(String address, int port, LoginFrame loginFrame) throws IOException {
        this.loginFrame = loginFrame;
        socket = new Socket(address, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void startListening() {
        new Thread(() -> {
            try {
                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    processMessage(fromServer);
                }
            } catch (IOException e) {
                System.out.println("Kết nối đến Server bị mất.");
                e.printStackTrace();
            }
        }).start();
    }

    private void processMessage(String message) {
        String[] parts = message.split(" ");
        String command = parts[0];

        if (command.equals("LOGIN_SUCCESS")) {
            // Đăng nhập thành công: Đóng LoginFrame và mở GameFrame
            SwingUtilities.invokeLater(() -> {
                loginFrame.dispose();
                gameFrame = new GameFrame(this); 
                
                // Gửi yêu cầu GET_STATS sau khi GameFrame được tạo
                sendMessage("GET_STATS"); 
            });
        } else if (command.equals("LOGIN_FAIL")) {
            SwingUtilities.invokeLater(() -> {
                loginFrame.showError("Đăng nhập thất bại. Vui lòng thử lại.");
            });
        } else if (command.equals("REGISTER_SUCCESS")) {
            SwingUtilities.invokeLater(() -> {
                loginFrame.showMessage("Đăng ký thành công. Vui lòng đăng nhập.");
            });
        } else if (command.equals("REGISTER_FAIL")) {
            SwingUtilities.invokeLater(() -> {
                loginFrame.showError("Đăng ký thất bại. Tên người dùng có thể đã tồn tại.");
            });
        } else if (command.equals("START")) {
            myMark = parts[1].charAt(0);
            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    gameFrame.setPlayer(myMark);
                }
            });
            
        } else if (command.equals("VALID_MOVE")) {
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            char player = parts[3].charAt(0); 
            
            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    gameFrame.updateBoard(row, col, player);
                    char nextPlayer = (player == 'X') ? 'O' : 'X';
                    gameFrame.setTurn(myMark == nextPlayer); 
                }
            });
            
        } else if (command.equals("WINNER")) {
            char winner = parts[1].charAt(0);
            String resultMessage = "Người chơi " + winner + " thắng!";
            
            int[] winCoords = new int[6];
            try {
                for (int i = 0; i < 6; i++) {
                    winCoords[i] = Integer.parseInt(parts[i + 2]);
                }
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                System.err.println("Lỗi phân tích tọa độ đường thắng: " + e.getMessage());
            }

            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    gameFrame.setTurn(false); 
                    gameFrame.highlightWinner(winCoords); 
                    gameFrame.showResult(resultMessage);
                    gameFrame.showRestartOption(resultMessage);
                }
            });
            
        } else if (command.equals("TIE")) {
            String resultMessage = "Hòa!";

            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    gameFrame.setTurn(false); 
                    gameFrame.showResult(resultMessage);
                    gameFrame.showRestartOption(resultMessage);
                }
            });
            
        } else if (command.equals("NEW_ROUND")) { 
            char startingPlayer = parts[1].charAt(0);
            
            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    gameFrame.resetBoard(); 
                    gameFrame.setTurn(myMark == startingPlayer); 
                }
            });
            
        } else if (command.equals("TOP_PLAYERS")) { 
            if (gameFrame == null) return;

            try {
                int count = Integer.parseInt(parts[1]);
                List<Object[]> rows = new ArrayList<>();
                
                for (int i = 0; i < count; i++) {
                    // Đọc Rank (parts[2 + i * 3])
                    String rankStr = parts[2 + i * 3]; 
                    // Đọc Tên (parts[3 + i * 3])
                    String username = parts[3 + i * 3];
                    // Đọc Win (parts[4 + i * 3])
                    String winsStr = parts[4 + i * 3]; 
                    
                    if (winsStr.equalsIgnoreCase("null")) {
                        winsStr = "0"; 
                    }
                    int wins = Integer.parseInt(winsStr); 
                    
                    // Gửi rankStr (String) vào cột 0. IconRenderer sẽ chuyển String này thành Icon.
                    rows.add(new Object[]{rankStr, username, wins});
                }
                
                SwingUtilities.invokeLater(() -> {
                    gameFrame.updateTopPlayers(rows.toArray(new Object[0][]));
                });
                
            } catch (Exception e) {
                System.err.println("Lỗi phân tích dữ liệu TOP_PLAYERS: " + e.getMessage());
            }
            
        } else if (command.equals("RECENT_WINS")) {
            if (gameFrame == null) return;
            
            try {
                int count = Integer.parseInt(parts[1]);
                List<Object[]> rows = new ArrayList<>();
                
                for (int i = 0; i < count; i++) {
                    // Đọc Tên (parts[2 + i * 2])
                    String username = parts[2 + i * 2];
                    // Đọc Kết quả (parts[3 + i * 2])
                    String result = parts[3 + i * 2];
                    rows.add(new Object[]{username, result});
                }
                
                SwingUtilities.invokeLater(() -> {
                    gameFrame.updateRecentWins(rows.toArray(new Object[0][]));
                });
            } catch (Exception e) {
                System.err.println("Lỗi phân tích dữ liệu RECENT_WINS: " + e.getMessage());
            }
        }
    }
    
    // ĐÃ LOẠI BỎ: Phương thức createImageIcon không cần thiết vì IconRenderer đã xử lý.
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}