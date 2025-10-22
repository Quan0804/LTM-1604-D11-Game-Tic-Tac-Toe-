package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class LobbyFrame extends JFrame {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String username;
    private volatile boolean listening;
    private Thread listenerThread;
    private JList<String> playerList;
    private DefaultListModel<String> playerListModel;
    private JButton challengeButton;
    private JLabel statusLabel;
    private JPanel leaderboardPanel;
    private JTextArea recentGamesArea;
    private Timer refreshTimer;
    
    // Load icons
    private ImageIcon goldMedalIcon;
    private ImageIcon silverMedalIcon;
    private ImageIcon bronzeMedalIcon;
    private ImageIcon medalIcon;
    private ImageIcon playerMedalIcon; // Icon cho người chơi trong danh sách
    
    public LobbyFrame(Socket socket, PrintWriter writer, BufferedReader reader, String username) {
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.username = username;
        
        initializeComponents();
        setupLayout();
        setupEventListeners();
        startListening();
        startRefreshTimer();
        // Yêu cầu dữ liệu ngay khi vào phòng chờ với delay nhỏ
        Timer initialRequestTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (writer != null) {
                        System.out.println("Client gửi request GET_ONLINE_PLAYERS"); // Debug log
                        writer.println("GET_ONLINE_PLAYERS");
                        writer.flush();
                        writer.println("GET_LEADERBOARD");
                        writer.flush();
                        writer.println("GET_RECENT_GAMES");
                        writer.flush();
                    }
                } catch (Exception ex) {
                    System.out.println("Lỗi gửi request: " + ex.getMessage()); // Debug log
                }
            }
        });
        initialRequestTimer.setRepeats(false);
        initialRequestTimer.start();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Sảnh Chờ - Tic Tac Toe");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(800, 600));
    }
    
    private void initializeComponents() {
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerList.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        playerList.setFixedCellHeight(40);
        playerList.setBackground(new Color(250, 250, 250));
        playerList.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Custom renderer cho playerList với icon medal
        playerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                // Thêm icon medal trước tên người chơi
                if (playerMedalIcon != null) {
                    label.setIcon(playerMedalIcon);
                    label.setText(value.toString());
                } else {
                    label.setText("  " + value);
                }
                
                label.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                label.setIconTextGap(8); // Khoảng cách giữa icon và text
                
                if (isSelected) {
                    label.setBackground(new Color(100, 181, 246));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(250, 250, 250));
                    label.setForeground(new Color(33, 33, 33));
                }
                return label;
            }
        });
        
        challengeButton = new JButton("THÁCH ĐẤU");
        challengeButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        challengeButton.setPreferredSize(new Dimension(180, 45));
        challengeButton.setBackground(new Color(76, 175, 80));
        challengeButton.setForeground(Color.WHITE);
        challengeButton.setFocusPainted(false);
        challengeButton.setBorderPainted(false);
        challengeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        challengeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                challengeButton.setBackground(new Color(56, 155, 60));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                challengeButton.setBackground(new Color(76, 175, 80));
            }
        });
        
        statusLabel = new JLabel("Có 0 người chơi sẵn sàng");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(66, 66, 66));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Load medal icons
        try {
            goldMedalIcon = new ImageIcon(getClass().getResource("golden_medal.png"));
            silverMedalIcon = new ImageIcon(getClass().getResource("silver_medal.png"));
            bronzeMedalIcon = new ImageIcon(getClass().getResource("bronze_medal.png"));
            medalIcon = new ImageIcon(getClass().getResource("medal.png"));
            playerMedalIcon = new ImageIcon(getClass().getResource("medal.png"));
            
            // Resize icons to 24x24
            goldMedalIcon = new ImageIcon(goldMedalIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
            silverMedalIcon = new ImageIcon(silverMedalIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
            bronzeMedalIcon = new ImageIcon(bronzeMedalIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
            medalIcon = new ImageIcon(medalIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
            playerMedalIcon = new ImageIcon(playerMedalIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            System.out.println("Không thể load medal icons: " + e.getMessage());
        }
        
        // Create leaderboard panel with BoxLayout for vertical stacking
        leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new BoxLayout(leaderboardPanel, BoxLayout.Y_AXIS));
        leaderboardPanel.setBackground(new Color(255, 253, 240));
        leaderboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        recentGamesArea = new JTextArea(10, 20);
        recentGamesArea.setEditable(false);
        recentGamesArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        recentGamesArea.setBackground(new Color(240, 248, 255));
        recentGamesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Header với gradient effect
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(41, 128, 185);
                Color color2 = new Color(109, 213, 250);
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(900, 80));
        headerPanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("SẢNH CHỜ TIC TAC TOE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        
        JLabel welcomeLabel = new JLabel("Chào mừng, " + username + "!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeLabel.setForeground(new Color(240, 240, 240));
        
        JPanel headerContent = new JPanel(new BorderLayout());
        headerContent.setOpaque(false);
        headerContent.add(titleLabel, BorderLayout.CENTER);
        headerContent.add(welcomeLabel, BorderLayout.SOUTH);
        
        headerPanel.add(headerContent, BorderLayout.CENTER);
        
        // Main content với padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 245, 245));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Left panel - Player list
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel playerListTitle = new JLabel("Người Chơi Online");
        playerListTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        playerListTitle.setForeground(new Color(33, 33, 33));
        playerListTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        leftPanel.add(playerListTitle, BorderLayout.NORTH);
        
        JScrollPane playerScrollPane = new JScrollPane(playerList);
        playerScrollPane.setPreferredSize(new Dimension(280, 350));
        playerScrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        leftPanel.add(playerScrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(challengeButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Right panel - Statistics
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        rightPanel.setBackground(new Color(245, 245, 245));
        
        // Leaderboard
        JPanel leaderboardContainer = new JPanel(new BorderLayout(0, 10));
        leaderboardContainer.setBackground(Color.WHITE);
        leaderboardContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel leaderboardTitle = new JLabel("Bảng Xếp Hạng");
        leaderboardTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        leaderboardTitle.setForeground(new Color(33, 33, 33));
        leaderboardContainer.add(leaderboardTitle, BorderLayout.NORTH);
        
        JScrollPane leaderboardScrollPane = new JScrollPane(leaderboardPanel);
        leaderboardScrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        leaderboardScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        leaderboardScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leaderboardContainer.add(leaderboardScrollPane, BorderLayout.CENTER);
        
        // Recent games
        JPanel recentGamesPanel = new JPanel(new BorderLayout(0, 10));
        recentGamesPanel.setBackground(Color.WHITE);
        recentGamesPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel recentGamesTitle = new JLabel("Lịch Sử Gần Đây");
        recentGamesTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        recentGamesTitle.setForeground(new Color(33, 33, 33));
        recentGamesPanel.add(recentGamesTitle, BorderLayout.NORTH);
        
        JScrollPane recentGamesScrollPane = new JScrollPane(recentGamesArea);
        recentGamesScrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        recentGamesPanel.add(recentGamesScrollPane, BorderLayout.CENTER);
        
        rightPanel.add(leaderboardContainer);
        rightPanel.add(recentGamesPanel);
        
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
        
        // Status panel với style đẹp hơn
        JPanel statusPanel = new JPanel();
        statusPanel.setBackground(new Color(250, 250, 250));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(12, 10, 12, 10)
        ));
        statusPanel.add(statusLabel);
        
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventListeners() {
        challengeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                challengePlayer();
            }
        });
    }
    
    private void challengePlayer() {
        String selectedPlayer = playerList.getSelectedValue();
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người chơi để thách đấu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (selectedPlayer.equals(username)) {
            JOptionPane.showMessageDialog(this, "Bạn không thể thách đấu chính mình!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        writer.println("CHALLENGE:" + selectedPlayer);
    }
    
    private void startListening() {
        listening = true;
        listenerThread = new Thread(() -> {
            try {
                while (listening) {
                    try {
                        if (reader.ready()) {
                            String message = reader.readLine();
                            if (message == null) break;
                            handleServerMessage(message);
                        } else {
                            try { Thread.sleep(30); } catch (InterruptedException ie) { /* ignore */ }
                        }
                    } catch (IOException ioe) {
                        if (listening) ioe.printStackTrace();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        listenerThread.start();
    }

    private void stopListening() {
        listening = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
    
    private void handleServerMessage(String message) {
        System.out.println("Client nhận được message: " + message); // Debug log
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(":");
            String command = parts[0];
            
            switch (command) {
                case "ONLINE_PLAYERS":
                    if (parts.length > 1) {
                        updatePlayerList(parts[1]);
                    } else {
                        updatePlayerList("");
                    }
                    break;
                case "CHALLENGE":
                    if (parts.length > 1) {
                        handleChallenge(parts[1]);
                    }
                    break;
                case "CHALLENGE_SENT":
                    JOptionPane.showMessageDialog(this, "Đã gửi lời thách đấu!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "CHALLENGE_FAILED":
                    JOptionPane.showMessageDialog(this, "Không thể gửi lời thách đấu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    break;
                case "CHALLENGE_DECLINED":
                    if (parts.length > 1) {
                        JOptionPane.showMessageDialog(this, parts[1] + " đã từ chối lời thách đấu!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                    break;
                case "GAME_START":
                    if (parts.length >= 3) {
                        startGame(parts[1], parts[2]);
                    } else {
                        System.out.println("GAME_START message không đủ tham số: " + message);
                    }
                    break;
                case "LEADERBOARD":
                    if (parts.length > 1) {
                        updateLeaderboard(parts[1]);
                    } else {
                        updateLeaderboard("");
                    }
                    break;
                case "RECENT_GAMES":
                    if (parts.length > 1) {
                        updateRecentGames(parts[1]);
                    } else {
                        updateRecentGames("");
                    }
                    break;
            }
        });
    }
    
    private void updatePlayerList(String playersData) {
        System.out.println("Client nhận được danh sách players: " + playersData); // Debug log
        playerListModel.clear();
        if (!playersData.isEmpty()) {
            String[] players = playersData.split(",");
            for (String player : players) {
                if (!player.isEmpty()) {
                    playerListModel.addElement(player);
                    System.out.println("Thêm player vào danh sách: " + player); // Debug log
                }
            }
        }
        int count = playerListModel.size();
        if (count == 0) {
            statusLabel.setText("Không có người chơi nào online");
            statusLabel.setForeground(new Color(150, 150, 150));
        } else if (count == 1) {
            statusLabel.setText("Có 1 người chơi sẵn sàng");
            statusLabel.setForeground(new Color(76, 175, 80));
        } else {
            statusLabel.setText("Có " + count + " người chơi sẵn sàng");
            statusLabel.setForeground(new Color(76, 175, 80));
        }
        System.out.println("Tổng số players hiển thị: " + playerListModel.size()); // Debug log
    }
    
    private void handleChallenge(String challenger) {
        int choice = JOptionPane.showConfirmDialog(this, 
            challenger + " muốn thách đấu với bạn. Bạn có chấp nhận không?", 
            "Lời thách đấu", 
            JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            writer.println("ACCEPT_CHALLENGE:" + challenger);
        } else {
            writer.println("DECLINE_CHALLENGE:" + challenger);
        }
    }
    
    private void startGame(String opponent, String mySymbol) {
        // Dừng timer và thread lắng nghe để tránh tranh chấp luồng đọc với GameFrame
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        stopListening();
        setVisible(false);
        new GameFrame(socket, writer, reader, username, opponent, mySymbol).setVisible(true);
    }
    
    private void updateLeaderboard(String leaderboardData) {
        leaderboardPanel.removeAll();
        
        if (!leaderboardData.isEmpty()) {
            String[] entries = leaderboardData.split(";");
            int rank = 1;
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    String[] parts = entry.split(",");
                    if (parts.length >= 3) {
                        String playerName = parts[0];
                        String wins = parts[1];
                        String losses = parts[2];
                        
                        JPanel entryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
                        entryPanel.setBackground(new Color(255, 253, 240));
                        entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
                        
                        // Add medal icon for all ranks
                        ImageIcon currentMedalIcon = null;
                        if (rank == 1 && goldMedalIcon != null) {
                            currentMedalIcon = goldMedalIcon;
                        } else if (rank == 2 && silverMedalIcon != null) {
                            currentMedalIcon = silverMedalIcon;
                        } else if (rank == 3 && bronzeMedalIcon != null) {
                            currentMedalIcon = bronzeMedalIcon;
                        } else if (rank >= 4 && medalIcon != null) {
                            currentMedalIcon = medalIcon;
                        }
                        
                        if (currentMedalIcon != null) {
                            JLabel medalLabel = new JLabel(currentMedalIcon);
                            entryPanel.add(medalLabel);
                        } else {
                            // Add spacing if icon not available
                            JLabel spacer = new JLabel("    ");
                            entryPanel.add(spacer);
                        }
                        
                        // Rank number
                        JLabel rankLabel = new JLabel(rank + ".");
                        rankLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                        rankLabel.setPreferredSize(new Dimension(25, 20));
                        entryPanel.add(rankLabel);
                        
                        // Player info
                        JLabel infoLabel = new JLabel(playerName + " - " + wins + " thắng, " + losses + " thua");
                        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                        entryPanel.add(infoLabel);
                        
                        leaderboardPanel.add(entryPanel);
                        rank++;
                    }
                }
            }
        }
        
        leaderboardPanel.revalidate();
        leaderboardPanel.repaint();
    }
    
    private void updateRecentGames(String gamesData) {
        recentGamesArea.setText("");
        if (!gamesData.isEmpty()) {
            String[] entries = gamesData.split(";");
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    String[] parts = entry.split(",");
                    if (parts.length >= 3) {
                        String player1 = parts[0];
                        String player2 = parts[1];
                        String winner = parts[2];
                        
                        // Hiển thị người thắng rõ ràng
                        String result = winner + " thắng";
                        recentGamesArea.append(player1 + " vs " + player2 + " - " + result + "\n");
                    }
                }
            }
        }
    }
    
    private void startRefreshTimer() {
        refreshTimer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    System.out.println("Client refresh - gửi request GET_ONLINE_PLAYERS"); // Debug log
                    writer.println("GET_ONLINE_PLAYERS");
                    writer.flush();
                    writer.println("GET_LEADERBOARD");
                    writer.flush();
                    writer.println("GET_RECENT_GAMES");
                    writer.flush();
                } catch (Exception ex) {
                    System.out.println("Lỗi refresh: " + ex.getMessage()); // Debug log
                }
            }
        });
        refreshTimer.start();
    }
    
    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        stopListening();
        super.dispose();
    }
}
