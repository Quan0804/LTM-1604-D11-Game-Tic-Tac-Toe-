package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.awt.Window;

public class GameFrame extends JFrame {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String username;
    private String opponent;
    private JButton[] buttons;
    private JLabel statusLabel;
    private JLabel currentPlayerLabel;
    private char currentPlayer; // Lượt hiện tại trong game (X hoặc O)
    private char mySymbol; // Symbol của chính mình (X hoặc O)
    private boolean myTurn;
    private boolean gameEnded;
    private boolean rematchRequested; // đã gửi REMATCH_REQUEST và đang chờ
    private boolean receivedRematchOffer; // đã nhận REMATCH_OFFER từ đối thủ
    private JDialog rematchDialog; // hộp thoại rematch không chặn EDT
    
    public GameFrame(Socket socket, PrintWriter writer, BufferedReader reader, String username, String opponent, String mySymbolStr) {
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.username = username;
        this.opponent = opponent;
        this.mySymbol = mySymbolStr.charAt(0);
        this.currentPlayer = 'X';
        this.myTurn = (this.mySymbol == 'X'); // X đi trước
        this.gameEnded = false;
        this.rematchRequested = false;
        this.receivedRematchOffer = false;
        
        System.out.println("GameFrame khởi tạo: username=" + username + ", opponent=" + opponent + ", mySymbol=" + this.mySymbol); // Debug log
        
        initializeComponents();
        setupLayout();
        setupEventListeners();
        startListening();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Game Tic Tac Toe");
        setSize(500, 600);
        setLocationRelativeTo(null);
        setResizable(false);
    }
    
    private void initializeComponents() {
        buttons = new JButton[9];
        for (int i = 0; i < 9; i++) {
            buttons[i] = new JButton("");
            buttons[i].setFont(new Font("Arial", Font.BOLD, 60));
            buttons[i].setPreferredSize(new Dimension(120, 120));
            buttons[i].setBackground(Color.WHITE);
            buttons[i].setFocusPainted(false);
            buttons[i].setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 2));
            buttons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            buttons[i].setEnabled(myTurn); // Enable buttons nếu là lượt của mình
            buttons[i].setOpaque(true); // Đảm bảo background hiển thị
            
            // Hover effect
            final int index = i;
            buttons[i].addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (buttons[index].isEnabled() && buttons[index].getText().isEmpty()) {
                        buttons[index].setBackground(new Color(236, 240, 241));
                    }
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (buttons[index].getText().isEmpty()) {
                        buttons[index].setBackground(Color.WHITE);
                    }
                }
            });
        }
        
        // Hiển thị trạng thái dựa trên symbol
        if (myTurn) {
            statusLabel = new JLabel("Đến lượt bạn!");
            statusLabel.setForeground(new Color(46, 204, 113));
            currentPlayerLabel = new JLabel("Lượt của: " + username + " (" + mySymbol + ")");
        } else {
            statusLabel = new JLabel("Chờ " + opponent + " đánh...");
            statusLabel.setForeground(new Color(231, 76, 60));
            currentPlayerLabel = new JLabel("Lượt của: " + opponent);
        }
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        currentPlayerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        currentPlayerLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(0, 0));
        
        // Header panel with gradient
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(41, 128, 185), w, 0, new Color(109, 213, 250));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(getWidth(), 80));
        headerPanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("TIC TAC TOE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Player info panel
        JPanel playerInfoPanel = new JPanel();
        playerInfoPanel.setOpaque(false);
        playerInfoPanel.setLayout(new BoxLayout(playerInfoPanel, BoxLayout.Y_AXIS));
        
        JLabel myLabel = new JLabel(username + " (" + mySymbol + ")");
        myLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        myLabel.setForeground(Color.WHITE);
        myLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel vsLabel = new JLabel("vs");
        vsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        vsLabel.setForeground(new Color(236, 240, 241));
        vsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel oppLabel = new JLabel(opponent + " (" + (mySymbol == 'X' ? "O" : "X") + ")");
        oppLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        oppLabel.setForeground(Color.WHITE);
        oppLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        playerInfoPanel.add(Box.createVerticalStrut(10));
        playerInfoPanel.add(myLabel);
        playerInfoPanel.add(Box.createVerticalStrut(3));
        playerInfoPanel.add(vsLabel);
        playerInfoPanel.add(Box.createVerticalStrut(3));
        playerInfoPanel.add(oppLabel);
        
        headerPanel.add(playerInfoPanel, BorderLayout.EAST);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        
        // Board panel with better spacing
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 8, 8));
        boardPanel.setBackground(new Color(236, 240, 241));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        for (int i = 0; i < 9; i++) {
            boardPanel.add(buttons[i]);
        }
        
        // Status panel with modern design
        JPanel statusPanel = new JPanel();
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(189, 195, 199)),
            BorderFactory.createEmptyBorder(15, 10, 15, 10)
        ));
        
        currentPlayerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        statusPanel.add(currentPlayerLabel);
        statusPanel.add(Box.createVerticalStrut(8));
        statusPanel.add(statusLabel);
        
        add(headerPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventListeners() {
        for (int i = 0; i < 9; i++) {
            final int position = i;
            buttons[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    makeMove(position);
                }
            });
        }
    }
    
    private void makeMove(int position) {
        if (!myTurn || gameEnded || !buttons[position].getText().isEmpty()) {
            return;
        }
        
        String moveMessage = "MOVE:" + position + ":" + mySymbol;
        System.out.println("GameFrame gửi move: " + moveMessage); // Debug log
        writer.println(moveMessage);
        writer.flush();
		
		// Ngay sau khi gửi nước đi, tạm thời khóa lượt của mình
		myTurn = false;
		currentPlayer = (mySymbol == 'X') ? 'O' : 'X';
		currentPlayerLabel.setText("Lượt của: " + opponent);
		statusLabel.setText("Chờ " + opponent + " đánh...");
		for (int i = 0; i < 9; i++) {
			buttons[i].setEnabled(false);
		}
    }
    
    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    try {
                        if (reader.ready()) {
                            String message = reader.readLine();
                            if (message == null) break;
                            handleServerMessage(message);
                        } else {
                            try { Thread.sleep(30); } catch (InterruptedException ie) { /* ignore */ }
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        listenerThread.start();
    }
    
    private void handleServerMessage(String message) {
        System.out.println("GameFrame nhận được message: " + message); // Debug log
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(":");
            String command = parts[0];
            
            System.out.println("GameFrame xử lý command: " + command + ", parts.length=" + parts.length); // Debug log
            
            // Debug log chi tiết cho GAME_START
            if ("GAME_START".equals(command)) {
                System.out.println("GameFrame nhận GAME_START message!");
                System.out.println("  - parts[0] (command): " + parts[0]);
                System.out.println("  - parts[1] (opponent): " + (parts.length > 1 ? parts[1] : "MISSING"));
                System.out.println("  - parts[2] (mySymbol): " + (parts.length > 2 ? parts[2] : "MISSING"));
            }
            
            switch (command) {
                case "MOVE":
                    handleMove(Integer.parseInt(parts[1]), parts[2].charAt(0));
                    break;
                case "TURN":
                    handleTurn(parts[1].charAt(0));
                    break;
                case "GAME_END":
                    handleGameEnd(parts[1], parts[2]);
                    break;
                case "REMATCH_OFFER":
                    handleRematchOffer(parts[1]);
                    break;
                case "REMATCH_START":
                    // Server báo rematch bắt đầu: parts[1]=opponent, parts[2]=mySymbol
                    System.out.println("Client nhận REMATCH_START - Bắt đầu reset bàn cờ");
                    
                    // Đánh dấu đang rematch để tránh mở popup mới
                    gameEnded = false;
                    rematchRequested = false; // Reset flag ngay lập tức
                    receivedRematchOffer = false; // Reset flag offer
                    
                    // Đóng NGAY dialog rematch nếu có
                    if (rematchDialog != null) {
                        try {
                            rematchDialog.setVisible(false);
                            rematchDialog.dispose();
                        } catch (Exception ignore) {}
                        rematchDialog = null;
                    }
                    
                    // Đóng MỌI JOptionPane và JDialog đang mở
                    Window[] windows = Window.getWindows();
                    for (Window window : windows) {
                        if (window instanceof JDialog) {
                            JDialog dialog = (JDialog) window;
                            if (dialog.isVisible() && !dialog.equals(GameFrame.this)) {
                                String title = dialog.getTitle();
                                if (title != null && (title.contains("Kết thúc ván") || 
                                    title.contains("Lời mời đấu lại") || 
                                    title.contains("Kết quả"))) {
                                    System.out.println("Đóng dialog: " + title);
                                    try {
                                        dialog.setVisible(false);
                                        dialog.dispose();
                                    } catch (Exception e) { /* ignore */ }
                                }
                            }
                        }
                    }
                    
                    // Reset bàn cờ ngay
                    resetForRematch(parts[1], parts[2].charAt(0));
                    System.out.println("Client đã reset bàn cờ xong, sẵn sàng chơi");
                    break;
                case "REMATCH_WAITING":
                    statusLabel.setText("Đã gửi lời mời đấu lại. Đang chờ đối thủ...");
                    statusLabel.setForeground(new Color(243, 156, 18));
                    break;
                case "REMATCH_DECLINED":
                    // Đóng tất cả dialog cũ trước
                    if (rematchDialog != null) {
                        try {
                            rematchDialog.setVisible(false);
                            rematchDialog.dispose();
                        } catch (Exception e) { /* ignore */ }
                        rematchDialog = null;
                    }
                    
                    // Hiện popup thông báo với nút "Về sảnh"
                    Object[] options = {"Về sảnh"};
                    JOptionPane.showOptionDialog(this,
                        parts[1] + " đã từ chối đấu lại.",
                        "Đấu lại",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[0]);
                    
                    dispose();
                    new LobbyFrame(socket, writer, reader, username).setVisible(true);
                    break;
                case "OPPONENT_LEFT":
                    // Đóng tất cả dialog cũ trước
                    if (rematchDialog != null) {
                        try {
                            rematchDialog.setVisible(false);
                            rematchDialog.dispose();
                        } catch (Exception e) { /* ignore */ }
                        rematchDialog = null;
                    }
                    
                    // Hiện popup thông báo với nút "Về sảnh"
                    Object[] optionsLeft = {"Về sảnh"};
                    JOptionPane.showOptionDialog(this,
                        "Đối thủ đã rời khỏi trận đấu.",
                        "Thông báo",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        optionsLeft,
                        optionsLeft[0]);
                    
                    dispose();
                    new LobbyFrame(socket, writer, reader, username).setVisible(true);
                    break;
                case "INVALID_TURN":
                    JOptionPane.showMessageDialog(this, "Không phải lượt của bạn!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                    break;
                case "INVALID_MOVE":
                    JOptionPane.showMessageDialog(this, "Nước đi không hợp lệ!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                    break;
                default:
                    System.out.println("GameFrame không nhận diện được command: " + command); // Debug log
                    break;
            }
        });
    }
    
    
    private void handleMove(int position, char player) {
        System.out.println("GameFrame handleMove: position=" + position + ", player=" + player); // Debug log
        
        // Set text trước
        buttons[position].setText(String.valueOf(player));
        
        // Set màu dựa vào player
        if (player == 'X') {
            System.out.println("Setting X color: RED (192, 57, 43)");
            buttons[position].setForeground(new Color(192, 57, 43)); // Đỏ đậm
            buttons[position].setBackground(new Color(250, 219, 216)); // Background đỏ nhạt
        } else if (player == 'O') {
            System.out.println("Setting O color: NAVY (44, 62, 80)");
            buttons[position].setForeground(new Color(44, 62, 80)); // Xanh navy
            buttons[position].setBackground(new Color(214, 219, 223)); // Background xám xanh nhạt
        }
        
        // Disable button và force update
        buttons[position].setEnabled(false);
        buttons[position].revalidate();
        buttons[position].repaint();
    }
    
    private void handleTurn(char nextPlayer) {
        System.out.println("GameFrame handleTurn: nextPlayer=" + nextPlayer + ", mySymbol=" + this.mySymbol); // Debug log
        currentPlayer = nextPlayer;
        myTurn = (currentPlayer == this.mySymbol);
        
        System.out.println("GameFrame cập nhật lượt: currentPlayer=" + currentPlayer + ", myTurn=" + myTurn); // Debug log
        
        if (myTurn) {
            currentPlayerLabel.setText("Lượt của: " + username);
            statusLabel.setText("Đến lượt bạn!");
        } else {
            currentPlayerLabel.setText("Lượt của: " + opponent);
            statusLabel.setText("Chờ " + opponent + " đánh...");
        }
        
        // Enable/disable buttons dựa trên lượt
        for (int i = 0; i < 9; i++) {
            buttons[i].setEnabled(myTurn && buttons[i].getText().isEmpty());
        }
    }
    
    private void handleGameEnd(String result, String winner) {
        gameEnded = true;
        myTurn = false;
        
        // Vô hiệu hóa tất cả buttons
        for (JButton button : buttons) {
            button.setEnabled(false);
        }
        
        switch (result) {
            case "WIN":
                if (winner.equals(username)) {
                    statusLabel.setText("Bạn đã thắng!");
                    statusLabel.setForeground(new Color(46, 204, 113));
                    JOptionPane.showMessageDialog(this, "Chúc mừng! Bạn đã thắng!", "Kết quả", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    statusLabel.setText("Bạn đã thua!");
                    statusLabel.setForeground(new Color(231, 76, 60));
                    JOptionPane.showMessageDialog(this, "Bạn đã thua! " + winner + " thắng!", "Kết quả", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            case "LOSE":
                statusLabel.setText("Bạn đã thua!");
                statusLabel.setForeground(new Color(231, 76, 60));
                JOptionPane.showMessageDialog(this, "Bạn đã thua! " + winner + " thắng!", "Kết quả", JOptionPane.INFORMATION_MESSAGE);
                break;
            case "DRAW":
                statusLabel.setText("Hòa!");
                statusLabel.setForeground(new Color(243, 156, 18));
                JOptionPane.showMessageDialog(this, "Game hòa!", "Kết quả", JOptionPane.INFORMATION_MESSAGE);
                break;
        }
        
        // Chờ một chút để xem có nhận REMATCH_OFFER không
        new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException e) { /* ignore */ }
            // Kiểm tra gameEnded vẫn còn true (chưa rematch) và chưa nhận offer
            if (gameEnded && !receivedRematchOffer && !rematchRequested) {
                askRematchOrExit();
            }
        }).start();
    }

    private void handleRematchOffer(String fromUser) {
        receivedRematchOffer = true; // Đánh dấu đã nhận offer
        // Nếu mình đã gửi lời mời trước đó, tự động đồng ý (tránh popup lặp khi cả hai cùng bấm)
        if (rematchRequested) {
            System.out.println("Client đã gửi yêu cầu trước đó, tự động chấp nhận từ " + fromUser);
            
            // Đóng dialog ngay lập tức
            if (rematchDialog != null) {
                try {
                    rematchDialog.setVisible(false);
                    rematchDialog.dispose();
                } catch (Exception e) { /* ignore */ }
                rematchDialog = null;
            }
            
            writer.println("REMATCH_REQUEST:" + fromUser);
            writer.flush();
            statusLabel.setText("Cả hai đã đồng ý. Đang reset bàn cờ...");
            statusLabel.setForeground(new Color(46, 204, 113));
            return;
        }
        
        // Đóng TOÀN BỘ dialog rematch cũ trước khi hiện dialog mới
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof JDialog) {
                JDialog dialog = (JDialog) window;
                // Đóng tất cả dialog có title liên quan đến rematch
                if (dialog.isVisible() && 
                    (dialog.getTitle().contains("Kết thúc ván") || 
                     dialog.getTitle().contains("Lời mời đấu lại"))) {
                    System.out.println("Đóng dialog cũ: " + dialog.getTitle());
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }
        }
        
        if (rematchDialog != null) {
            try {
                rematchDialog.setVisible(false);
                rematchDialog.dispose();
            } catch (Exception e) { /* ignore */ }
            rematchDialog = null;
        }
        
        // Chờ một chút để đảm bảo dialog cũ đã đóng hoàn toàn
        try { Thread.sleep(200); } catch (InterruptedException e) { /* ignore */ }
        
        // Chưa gửi yêu cầu → hiện popup hỏi "Đồng ý" hay "Từ chối và về sảnh"
        SwingUtilities.invokeLater(() -> {
            Object[] options = {"Đồng ý", "Từ chối và về sảnh"};
            int choice = JOptionPane.showOptionDialog(GameFrame.this,
                fromUser + " muốn đấu lại. Bạn có đồng ý không?",
                "Lời mời đấu lại",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
            if (choice == JOptionPane.YES_OPTION || choice == 0) {
                // Đồng ý: gửi REMATCH_REQUEST
                writer.println("REMATCH_REQUEST:" + fromUser);
                writer.flush();
                rematchRequested = true;
                statusLabel.setText("Đã đồng ý đấu lại. Đang chờ...");
                statusLabel.setForeground(new Color(243, 156, 18));
            } else if (choice == JOptionPane.NO_OPTION || choice == 1) {
                // Từ chối và quay về sảnh
                writer.println("REMATCH_DECLINE:" + fromUser);
                writer.flush();
                dispose();
                new LobbyFrame(socket, writer, reader, username).setVisible(true);
            }
        });
    }

    private void askRematchOrExit() {
        // Chạy trong thread riêng để không chặn việc nhận message từ server
        new Thread(() -> {
            // Tạo JOptionPane không chặn
            final JOptionPane pane = new JOptionPane(
                "Bạn có muốn đấu lại?",
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                new Object[]{"Đấu lại", "Về sảnh"},
                "Đấu lại");
            
            rematchDialog = pane.createDialog(GameFrame.this, "Kết thúc ván");
            rematchDialog.setModal(true);
            rematchDialog.setVisible(true);
            
            Object selectedValue = pane.getValue();
            
            SwingUtilities.invokeLater(() -> {
                // Kiểm tra xem dialog có bị đóng bởi REMATCH_OFFER không
                if (selectedValue == null || selectedValue == JOptionPane.UNINITIALIZED_VALUE) {
                    return; // Dialog bị đóng, không làm gì
                }
                
                if ("Đấu lại".equals(selectedValue)) {
                    writer.println("REMATCH_REQUEST:" + opponent);
                    writer.flush();
                    rematchRequested = true;
                    statusLabel.setText("Đã gửi lời mời đấu lại. Đang chờ đối thủ...");
                    statusLabel.setForeground(new Color(243, 156, 18));
                } else if ("Về sảnh".equals(selectedValue)) {
                    // Gửi thông báo cho đối thủ là mình đã rời
                    writer.println("LEAVE_GAME:" + opponent);
                    writer.flush();
                    dispose();
                    new LobbyFrame(socket, writer, reader, username).setVisible(true);
                }
            });
        }).start();
    }

    private void resetForRematch(String opponentName, char mySymbolParam) {
        this.opponent = opponentName;
        this.mySymbol = mySymbolParam;
        this.currentPlayer = 'X';
        this.myTurn = (mySymbolParam == 'X');
        this.gameEnded = false;
        this.rematchRequested = false;
        this.receivedRematchOffer = false;
        
        // Đóng hộp thoại rematch nếu đang mở
        if (rematchDialog != null) {
            rematchDialog.setVisible(false);
            rematchDialog.dispose();
            rematchDialog = null;
        }
        
        // Reset tất cả buttons về trạng thái ban đầu
        for (JButton b : buttons) {
            b.setText("");
            b.setEnabled(myTurn);
            b.setBackground(Color.WHITE); // Reset background về trắng
            b.setForeground(Color.BLACK); // Reset foreground về đen
            b.repaint(); // Force repaint
        }
        
        // Cập nhật status labels
        if (myTurn) {
            currentPlayerLabel.setText("Lượt của: " + username + " (" + mySymbol + ")");
            statusLabel.setText("Đến lượt bạn!");
            statusLabel.setForeground(new Color(46, 204, 113));
        } else {
            currentPlayerLabel.setText("Lượt của: " + opponent);
            statusLabel.setText("Chờ " + opponent + " đánh...");
            statusLabel.setForeground(new Color(231, 76, 60));
        }
    }
}
