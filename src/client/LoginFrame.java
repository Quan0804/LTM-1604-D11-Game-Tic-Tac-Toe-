package client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener; 

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private Client client;

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    
    // ĐỊNH NGHĨA ĐƯỜNG DẪN ĐẾN FILE LOGO
    private static final String LOGO_PATH = "/client/logo.png"; 

    public LoginFrame() {
        // Thiết lập tiêu đề và kích thước cửa sổ
        setTitle("Tic Tac Toe Client"); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Sử dụng GridBagLayout để căn chỉnh tốt hơn
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30)); 
        GridBagConstraints gbc = new GridBagConstraints();
        
        // --- Tùy chỉnh Font và Màu sắc ---
        Font labelFont = new Font("Arial", Font.PLAIN, 14);
        
        // --- 1. Logo (thay thế Tiêu đề cũ) ---
        JLabel logoLabel;
        
        // Kích thước mong muốn cho logo
        final int TARGET_WIDTH = 300;
        final int TARGET_HEIGHT = 100;
        
        try {
            InputStream is = LoginFrame.class.getResourceAsStream(LOGO_PATH);
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                
                Image scaledImg = img.getScaledInstance(TARGET_WIDTH, TARGET_HEIGHT, Image.SCALE_SMOOTH); 
                
                logoLabel = new JLabel(new ImageIcon(scaledImg));
                logoLabel.setPreferredSize(new Dimension(TARGET_WIDTH, TARGET_HEIGHT));

            } else {
                System.err.println("LỖI: Không tìm thấy file logo tại " + LOGO_PATH);
                logoLabel = new JLabel("Game Tic Tac Toe"); 
                logoLabel.setFont(new Font("Arial", Font.BOLD, 26));
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi tải logo: " + e.getMessage());
            e.printStackTrace();
            logoLabel = new JLabel("Game Tic Tac Toe"); 
            logoLabel.setFont(new Font("Arial", Font.BOLD, 26));
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Chiếm 2 cột
        gbc.anchor = GridBagConstraints.CENTER; // Căn giữa
        gbc.insets = new Insets(0, 0, 30, 0); // Khoảng cách dưới 30px
        mainPanel.add(logoLabel, gbc); 
        
        // --- 2. Tên đăng nhập ---
        // Đặt Label ở hàng 1, chiếm 2 cột
        gbc.gridwidth = 2; 
        gbc.insets = new Insets(10, 0, 5, 0); 
        
        gbc.gridy = 1;
        gbc.gridx = 0;
        // *** FIX CĂN LỀ: Thay đổi anchor sang WEST (Căn trái) ***
        gbc.anchor = GridBagConstraints.WEST; 
        gbc.fill = GridBagConstraints.NONE;
        JLabel userLabel = new JLabel("Tên đăng nhập:");
        userLabel.setFont(labelFont);
        mainPanel.add(userLabel, gbc);

        // Đặt Field ở hàng 2, chiếm 2 cột
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Điền đầy chiều ngang
        gbc.weightx = 1.0; // Ưu tiên mở rộng
        gbc.insets = new Insets(0, 0, 10, 0); 
        // Giữ anchor WEST cho ô nhập liệu để nó bắt đầu từ lề trái
        gbc.anchor = GridBagConstraints.WEST; 
        usernameField = new JTextField(15);
        mainPanel.add(usernameField, gbc);

        // --- 3. Mật khẩu ---
        // Đặt Label ở hàng 3, chiếm 2 cột
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0; 
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 0, 5, 0); 
        // *** FIX CĂN LỀ: Thay đổi anchor sang WEST (Căn trái) ***
        gbc.anchor = GridBagConstraints.WEST; 
        JLabel passwordLabel = new JLabel("Mật khẩu:");
        passwordLabel.setFont(labelFont);
        mainPanel.add(passwordLabel, gbc);

        // Đặt Field ở hàng 4, chiếm 2 cột
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0); 
        // Giữ anchor WEST cho ô nhập liệu
        gbc.anchor = GridBagConstraints.WEST; 
        passwordField = new JPasswordField(15);
        mainPanel.add(passwordField, gbc);

        // --- 4. Nút bấm ---
        gbc.gridy = 5; // Hàng 5
        gbc.weighty = 0; 
        gbc.gridwidth = 1; // Đặt lại thành 1 để các nút nằm cạnh nhau
        gbc.insets = new Insets(0, 0, 10, 10); 
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        loginButton = new JButton("Đăng nhập");
        mainPanel.add(loginButton, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 10, 0);
        registerButton = new JButton("Đăng ký");
        mainPanel.add(registerButton, gbc);

        // --- 5. Status Label ---
        gbc.gridy = 6; // Hàng 6
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; 
        gbc.insets = new Insets(5, 0, 0, 0); 
        
        statusLabel = new JLabel("Chưa kết nối");
        statusLabel.setForeground(Color.GRAY);
        mainPanel.add(statusLabel, gbc);

        // --- Xử lý sự kiện ---
        loginButton.addActionListener(e -> attemptLogin());
        registerButton.addActionListener(e -> attemptRegister());

        setContentPane(mainPanel);
        // Đặt lại kích thước sau khi thêm component để đảm bảo phù hợp
        pack();
        // Đặt lại vị trí trung tâm
        setLocationRelativeTo(null); 
        setVisible(true);
    }
    
    /**
     * Phương thức cố gắng thiết lập kết nối Client.
     */
    private boolean connectToServer() {
        if (client == null) {
            try {
                statusLabel.setForeground(Color.ORANGE);
                statusLabel.setText("Đang kết nối tới server...");
                
                Client newClient = new Client(SERVER_ADDRESS, SERVER_PORT, this); 
                newClient.startListening();
                client = newClient; 
                
                statusLabel.setForeground(Color.BLUE);
                statusLabel.setText("Đã kết nối. Đang chờ phản hồi.");
                return true;
            } catch (IOException ex) {
                showError("Không thể kết nối đến server: " + ex.getMessage());
                client = null;
                return false;
            }
        }
        return true; 
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty() || username.contains(" ") || password.contains(" ")) {
            showError("Tên đăng nhập và mật khẩu không hợp lệ.");
            return; 
        }

        if (connectToServer()) {
            client.sendMessage("LOGIN " + username + " " + password);
            statusLabel.setForeground(Color.BLUE);
            statusLabel.setText("Đang gửi yêu cầu đăng nhập...");
        }
    }

    private void attemptRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty() || username.contains(" ") || password.contains(" ")) {
            showError("Tên đăng nhập và mật khẩu không hợp lệ.");
            return; 
        }
        
        if (connectToServer()) {
            client.sendMessage("REGISTER " + username + " " + password);
            statusLabel.setForeground(Color.BLUE);
            statusLabel.setText("Đang gửi yêu cầu đăng ký...");
        }
    }

    // Hiển thị lỗi trên statusLabel
    public void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }

    // Hiển thị thông báo thành công trên statusLabel
    public void showMessage(String message) {
        statusLabel.setForeground(new Color(0, 150, 0)); 
        statusLabel.setText(message);
    }

    // Constructor mới để Client truyền vào GameFrame nếu đăng nhập thành công
    public LoginFrame(Client client) {
        // Hàm tạo này thường được dùng để hiển thị lại LoginFrame sau khi Client bị ngắt kết nối
        this(); // Gọi constructor mặc định
        this.client = client;
        showMessage("Đã ngắt kết nối. Vui lòng đăng nhập lại.");
    }


    public static void main(String[] args) {
        // Áp dụng Look and Feel của hệ thống để giao diện đẹp hơn
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // bỏ qua lỗi
        }
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}