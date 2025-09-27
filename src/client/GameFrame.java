package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import client.IconRenderer;


public class GameFrame extends JFrame {
    
    private JButton[][] boardButtons = new JButton[3][3]; 
    private Client client;
    public char playerMark;
    private JLabel messageLabel;
    private boolean isMyTurn = false;

    // ĐỊNH NGHĨA MÀU SẮC
    private static final Color COLOR_X = new Color(0, 180, 150); // Xanh ngọc
    private static final Color COLOR_O = new Color(150, 150, 150); // Xám
    private static final Color HIGHLIGHT_COLOR = new Color(150, 255, 150); // Xanh lá nhạt

    // CÁC THÀNH PHẦN MỚI cho bảng xếp hạng và lịch sử
    private JTable topPlayersTable;
    private DefaultTableModel topPlayersTableModel;
    private JTable recentWinsTable;
    private DefaultTableModel recentWinsTableModel;
    
    // Loại bỏ JDialog resultDialog không cần thiết, dùng JOptionPane

    public GameFrame(Client client) {
        this.client = client;
        setTitle("Game Tic Tac Toe");
        setSize(900, 550); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setBackground(Color.WHITE);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);
        splitPane.setResizeWeight(0.5); 
        splitPane.setDividerSize(5); 
        
        // --- Phần bên trái: Bàn cờ và messageLabel ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);

        JPanel boardDisplayPanel = new JPanel(new GridLayout(3, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(150, 150, 150)); 
                g2d.setStroke(new BasicStroke(4)); 
                int width = getWidth();
                int height = getHeight();

                // VẼ LƯỚI BÀN CỜ
                g2d.drawLine(width / 3, 0, width / 3, height);
                g2d.drawLine(2 * width / 3, 0, 2 * width / 3, height);
                g2d.drawLine(0, height / 3, width, height / 3);
                g2d.drawLine(0, 2 * height / 3, width, 2 * height / 3);
            }
        };
        boardDisplayPanel.setBackground(Color.WHITE); 

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j] = new JButton("");
                boardButtons[i][j].setFont(new Font("Arial", Font.BOLD, 100));
                boardButtons[i][j].setFocusPainted(false);
                boardButtons[i][j].setBorderPainted(false);
                boardButtons[i][j].setContentAreaFilled(false);
                boardButtons[i][j].setEnabled(false);
                
                final int row = i;
                final int col = j;
                final JButton button = boardButtons[i][j];

                button.addActionListener(e -> {
                    if (isMyTurn && button.getText().isEmpty()) { 
                        client.sendMessage("MOVE " + row + " " + col + " " + playerMark);
                    } else if (!isMyTurn) {
                        setMessage("Vui lòng chờ đến lượt của bạn.");
                    }
                });
                boardDisplayPanel.add(boardButtons[i][j]);
            }
        }
        leftPanel.add(boardDisplayPanel, BorderLayout.CENTER); 

        messageLabel = new JLabel("Chờ người chơi khác...");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 20)); 
        messageLabel.setOpaque(true); 
        messageLabel.setBackground(Color.WHITE);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        leftPanel.add(messageLabel, BorderLayout.SOUTH); 
        splitPane.setLeftComponent(leftPanel);

        // --- Phần bên phải: Bảng xếp hạng và Lịch sử ---
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        rightPanel.setBackground(Color.WHITE);

        // Renderer để căn giữa nội dung ô
        DefaultTableCellRenderer centerCellRenderer = new DefaultTableCellRenderer();
        centerCellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Renderer để căn giữa tiêu đề bảng (Header)
        DefaultTableCellRenderer centerHeaderRenderer = new DefaultTableCellRenderer();
        centerHeaderRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        
        // Bảng Top 3 Người chơi thắng nhiều nhất
        JLabel topPlayersTitle = new JLabel("Top Người chơi thắng nhiều nhất");
        topPlayersTitle.setFont(new Font("Arial", Font.BOLD, 16));
        topPlayersTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(topPlayersTitle);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10))); 

        String[] topPlayersColumns = {"Top", "Tên", "WIN"};
        topPlayersTableModel = new DefaultTableModel(topPlayersColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
            @Override
            public Class<?> getColumnClass(int column) {
                return Object.class; 
            }
        };
        topPlayersTable = new JTable(topPlayersTableModel);
        topPlayersTable.setRowHeight(30); 
        topPlayersTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        topPlayersTable.setFont(new Font("Arial", Font.PLAIN, 12));
        
        topPlayersTable.setDefaultRenderer(Object.class, new IconRenderer());
        
        // ÁP DỤNG CĂN GIỮA HEADER CHO TẤT CẢ CỘT
        topPlayersTable.getTableHeader().setDefaultRenderer(centerHeaderRenderer);
        
        // Căn giữa cột 'Tên'
        topPlayersTable.getColumnModel().getColumn(1).setCellRenderer(centerCellRenderer); 
        
        // Căn giữa cột 'WIN'
        topPlayersTable.getColumnModel().getColumn(2).setCellRenderer(centerCellRenderer); 
        
        // Đặt độ rộng cột để cân đối
        topPlayersTable.getColumnModel().getColumn(0).setPreferredWidth(50); // Top (Icon)
        topPlayersTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Tên
        topPlayersTable.getColumnModel().getColumn(2).setPreferredWidth(70); // WIN
        
        JScrollPane topPlayersScrollPane = new JScrollPane(topPlayersTable);
        
        // *** FIX KHOẢNG TRỐNG: Đặt kích thước ban đầu linh hoạt/nhỏ hơn để logic update có thể điều chỉnh
        topPlayersScrollPane.setMaximumSize(new Dimension(300, 300)); 
        // Đặt PreferredSize ban đầu chỉ khoảng 4 hàng
        topPlayersScrollPane.setPreferredSize(new Dimension(300, topPlayersTable.getRowHeight() * 4)); 
        
        topPlayersScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(topPlayersScrollPane);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Bảng Lịch sử chiến thắng gần đây
        JLabel recentWinsTitle = new JLabel("Lịch sử chơi gần đây");
        recentWinsTitle.setFont(new Font("Arial", Font.BOLD, 16));
        recentWinsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(recentWinsTitle);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        String[] recentWinsColumns = {"Tên", "Kết quả"};
        recentWinsTableModel = new DefaultTableModel(recentWinsColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };
        recentWinsTable = new JTable(recentWinsTableModel);
        recentWinsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        recentWinsTable.setFont(new Font("Arial", Font.PLAIN, 12));
        recentWinsTable.setRowHeight(25);
        
        // ÁP DỤNG CĂN GIỮA HEADER CHO TẤT CẢ CỘT
        recentWinsTable.getTableHeader().setDefaultRenderer(centerHeaderRenderer);

        // ÁP DỤNG CĂN GIỮA NỘI DUNG 
        recentWinsTable.getColumnModel().getColumn(0).setCellRenderer(centerCellRenderer); 
        recentWinsTable.getColumnModel().getColumn(1).setCellRenderer(centerCellRenderer); 
        
        // Đặt độ rộng cột để cân đối
        recentWinsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        recentWinsTable.getColumnModel().getColumn(1).setPreferredWidth(100);

        JScrollPane recentWinsScrollPane = new JScrollPane(recentWinsTable);
        // *** FIX KHOẢNG TRỐNG: Đặt kích thước ban đầu linh hoạt/nhỏ hơn để logic update có thể điều chỉnh
        recentWinsScrollPane.setMaximumSize(new Dimension(300, 250));
        recentWinsScrollPane.setPreferredSize(new Dimension(300, recentWinsTable.getRowHeight() * 4)); 
        
        recentWinsScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT); 
        rightPanel.add(recentWinsScrollPane);

        splitPane.setRightComponent(rightPanel);
        
        setContentPane(splitPane); 
        setVisible(true);
    }

    public void setPlayer(char mark) {
        this.playerMark = mark;
        setTitle("Game Tic Tac Toe - Player " + mark);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
    
    public void setTurn(boolean isMyTurn) {
        this.isMyTurn = isMyTurn;
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                JButton button = boardButtons[i][j];
                button.setEnabled(isMyTurn && button.getText().isEmpty());
            }
        }
        
        if (isMyTurn) {
            setMessage("ĐẾN LƯỢT BẠN (Player " + playerMark + ")");
            messageLabel.setForeground(new Color(0, 100, 200)); 
        } else {
            setMessage("LƯỢT ĐỐI THỦ (Player " + ((playerMark == 'X') ? 'O' : 'X') + ")");
            messageLabel.setForeground(Color.RED); 
        }
    }

    public void updateBoard(int row, int col, char mark) {
        boardButtons[row][col].setText(String.valueOf(mark));
        
        if (mark == 'X') {
            boardButtons[row][col].setForeground(COLOR_X); 
        } else {
            boardButtons[row][col].setForeground(COLOR_O); 
        }
        
        boardButtons[row][col].setEnabled(false);
    }
    
    public void highlightWinner(int[] winCoords) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j].setEnabled(false);
            }
        }
        
        for (int i = 0; i < 6; i += 2) {
            int row = winCoords[i];
            int col = winCoords[i + 1];
            
            boardButtons[row][col].setBackground(HIGHLIGHT_COLOR); 
            boardButtons[row][col].setOpaque(true); 
            boardButtons[row][col].setContentAreaFilled(true); 
        }
    }
    
    public void showResult(String message) {
        JOptionPane.showMessageDialog(this, message, "Kết quả", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void showRestartOption(String result) {
        Object[] options = {"Chơi Lại", "Thoát"};
        
        int choice = JOptionPane.showOptionDialog(this,
            result + "\nBạn có muốn chơi lại không?",
            "Ván Đấu Kết Thúc",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        if (choice == JOptionPane.YES_OPTION) {
            client.sendMessage("RESTART");
        } else {
            System.exit(0);
        }
    }

    public void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j].setText("");
                boardButtons[i][j].setEnabled(false); 
                boardButtons[i][j].setForeground(Color.BLACK); 
                boardButtons[i][j].setBackground(Color.WHITE);
                boardButtons[i][j].setOpaque(false);
                boardButtons[i][j].setContentAreaFilled(false);
            }
        }
    }

    public void updateTopPlayers(Object[][] playerData) {
        topPlayersTableModel.setRowCount(0);
        for (Object[] row : playerData) {
            topPlayersTableModel.addRow(row);
        }
        
        // FIX: Logic tính toán chiều cao chính xác
        int tableHeight = topPlayersTable.getRowHeight() * topPlayersTable.getRowCount();
        tableHeight += topPlayersTable.getTableHeader().getPreferredSize().height;
        tableHeight += 3;
        
        JScrollPane scrollPane = (JScrollPane) topPlayersTable.getParent().getParent();
        
        // Áp dụng chiều cao tính toán
        scrollPane.setPreferredSize(new Dimension(300, tableHeight));
        
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    public void updateRecentWins(Object[][] historyData) {
        recentWinsTableModel.setRowCount(0);
        for (Object[] row : historyData) {
            recentWinsTableModel.addRow(row);
        }
        
        // FIX: Logic tính toán chiều cao chính xác
        int tableHeight = recentWinsTable.getRowHeight() * recentWinsTable.getRowCount();
        tableHeight += recentWinsTable.getTableHeader().getPreferredSize().height;
        tableHeight += 3;
        
        // Đảm bảo chiều cao không vượt quá giới hạn tối đa (MaximumSize)
        int maxHeight = 250;
        if (tableHeight > maxHeight) {
             tableHeight = maxHeight;
        }

        JScrollPane scrollPane = (JScrollPane) recentWinsTable.getParent().getParent();
        // Áp dụng chiều cao tính toán (hoặc chiều cao tối đa)
        scrollPane.setPreferredSize(new Dimension(300, tableHeight));
        
        scrollPane.revalidate();
        scrollPane.repaint();
    }
}