package tcp;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private JButton[][] buttons = new JButton[3][3];
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private char mySymbol;
    private char currentTurn;

    public Client(String serverAddress, int port) {
        setTitle("Tic Tac Toe");
        setSize(300, 300);
        setLayout(new GridLayout(3, 3));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // tạo 9 button (3x3)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton("");
                final int row = i, col = j;
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 40));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setBackground(Color.WHITE);

                buttons[i][j].addActionListener(e -> {
                    if (mySymbol == currentTurn) {
                        out.println("MOVE " + row + " " + col);
                    } else {
                        JOptionPane.showMessageDialog(this, "Chưa tới lượt bạn!");
                    }
                });
                add(buttons[i][j]);
            }
        }

        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread nhận dữ liệu từ server
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        if (msg.startsWith("WELCOME")) {
                            mySymbol = msg.charAt(8);
                            setTitle("Tic Tac Toe - Bạn là " + mySymbol);
                        } else if (msg.startsWith("BOARD")) {
                            // đọc đủ 3 dòng bàn cờ
                            StringBuilder boardData = new StringBuilder();
                            for (int k = 0; k < 3; k++) {
                                boardData.append(in.readLine()).append("\n");
                            }
                            updateBoard(boardData.toString());
                        } else if (msg.startsWith("TURN")) {
                            currentTurn = msg.charAt(5);
                            setTitle("Tic Tac Toe - Bạn là " + mySymbol + " | Lượt: " + currentTurn);
                        } else if (msg.equals("INVALID_MOVE")) {
                            JOptionPane.showMessageDialog(this, "Ô này đã được đánh!");
                        } else if (msg.equals("NOT_YOUR_TURN")) {
                            JOptionPane.showMessageDialog(this, "Chưa tới lượt bạn!");
                        } else if (msg.startsWith("WINNER")) {
                            char winner = msg.charAt(7);
                            JOptionPane.showMessageDialog(this,
                                    "Người thắng: " + winner,
                                    "Kết thúc game", JOptionPane.INFORMATION_MESSAGE);
                        } else if (msg.equals("DRAW")) {
                            JOptionPane.showMessageDialog(this,
                                    "Hòa rồi!",
                                    "Kết thúc game", JOptionPane.INFORMATION_MESSAGE);
                        } else if (msg.equals("RESET")) {
                            resetBoardUI();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }

        setVisible(true);
    }

    // Cập nhật giao diện bàn cờ từ server
    private void updateBoard(String data) {
        String[] rows = data.split("\n");
        for (int i = 0; i < 3; i++) {
            String[] cols = rows[i].split("\\|");
            for (int j = 0; j < 3; j++) {
                String cell = cols[j].trim();
                buttons[i][j].setText(cell.equals(" ") ? "" : cell);

                // Xanh cho X, Đỏ cho O
                if (cell.equals("X")) {
                    buttons[i][j].setForeground(Color.BLUE);
                } else if (cell.equals("O")) {
                    buttons[i][j].setForeground(Color.RED);
                }
            }
        }
    }

    // Reset giao diện khi server gửi RESET
    private void resetBoardUI() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    buttons[i][j].setText("");
                    buttons[i][j].setBackground(Color.WHITE);
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client("127.0.0.1", 5000));
    }
}
