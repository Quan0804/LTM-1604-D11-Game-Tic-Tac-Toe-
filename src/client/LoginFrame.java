package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private ImageIcon logoIcon;
    
    public LoginFrame() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Tic Tac Toe - Đăng Nhập");
        setSize(450, 550);
        setLocationRelativeTo(null);
        setResizable(false);
    }
    
    private void initializeComponents() {
        // Load logo
        try {
            logoIcon = new ImageIcon(getClass().getResource("logo.png"));
            // Resize logo to 240x180 - rộng hơn theo chiều ngang
            logoIcon = new ImageIcon(logoIcon.getImage().getScaledInstance(360, 180, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            System.out.println("Không thể load logo: " + e.getMessage());
        }
        
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(300, 40));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(300, 40));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        loginButton = new JButton("ĐĂNG NHẬP");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(120, 40));
        loginButton.setBackground(new Color(41, 128, 185));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        registerButton = new JButton("ĐĂNG KÝ");
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerButton.setPreferredSize(new Dimension(120, 40));
        registerButton.setBackground(new Color(46, 204, 113));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effects
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(new Color(31, 108, 165));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(new Color(41, 128, 185));
            }
        });
        
        registerButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(new Color(36, 184, 93));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(new Color(46, 204, 113));
            }
        });
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Header với gradient và logo
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
        headerPanel.setPreferredSize(new Dimension(450, 230));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        // Logo
        if (logoIcon != null) {
            JLabel logoLabel = new JLabel(logoIcon);
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            logoLabel.setBorder(BorderFactory.createEmptyBorder(25, 0, 25, 0));
            headerPanel.add(logoLabel);
        }
        
        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        
        // Username field container
        JPanel usernameContainer = new JPanel();
        usernameContainer.setLayout(new BoxLayout(usernameContainer, BoxLayout.Y_AXIS));
        usernameContainer.setBackground(Color.WHITE);
        usernameContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameContainer.setMaximumSize(new Dimension(300, 70));
        
        JLabel usernameLabel = new JLabel("Tên đăng nhập");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameContainer.add(usernameLabel);
        usernameContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(300, 40));
        usernameContainer.add(usernameField);
        
        formPanel.add(usernameContainer);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Password field container
        JPanel passwordContainer = new JPanel();
        passwordContainer.setLayout(new BoxLayout(passwordContainer, BoxLayout.Y_AXIS));
        passwordContainer.setBackground(Color.WHITE);
        passwordContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordContainer.setMaximumSize(new Dimension(300, 70));
        
        JLabel passwordLabel = new JLabel("Mật khẩu");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordContainer.add(passwordLabel);
        passwordContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(300, 40));
        passwordContainer.add(passwordField);
        
        formPanel.add(passwordContainer);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.setMaximumSize(new Dimension(300, 50));
        formPanel.add(buttonPanel);
        
        add(headerPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
    }
    
    private void setupEventListeners() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
        
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                register();
            }
        });
        
        // Enter key để đăng nhập
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
    }
    
    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            connectToServer();
            writer.println("LOGIN:" + username + ":" + password);
            writer.flush();
            
            String response = reader.readLine();
            if ("LOGIN_SUCCESS".equals(response)) {
                // Chuyển đến LobbyFrame - KHÔNG đóng socket
                SwingUtilities.invokeLater(() -> {
                    setVisible(false);
                    new LobbyFrame(socket, writer, reader, username).setVisible(true);
                    dispose(); // Giải phóng tài nguyên của LoginFrame nhưng giữ socket
                });
            } else {
                JOptionPane.showMessageDialog(this, "Đăng nhập thất bại! Kiểm tra lại tên đăng nhập và mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                closeConnection(); // Đóng kết nối thất bại
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            connectToServer();
            writer.println("REGISTER:" + username + ":" + password);
            writer.flush();
            
            String response = reader.readLine();
            if ("REGISTER_SUCCESS".equals(response)) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công! Bạn có thể đăng nhập ngay.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                closeConnection(); // Đóng kết nối để người dùng đăng nhập lại
            } else {
                JOptionPane.showMessageDialog(this, "Đăng ký thất bại! Tên đăng nhập có thể đã tồn tại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                closeConnection();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void connectToServer() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket("localhost", 12345);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
    }
    
    private void closeConnection() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
            socket = null;
            writer = null;
            reader = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
