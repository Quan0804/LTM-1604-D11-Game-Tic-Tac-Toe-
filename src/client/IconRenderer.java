package client;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Image; // Import cần thiết cho Image Scaling

/**
 * Renderer tùy chỉnh để hiển thị ImageIcon trong JTable dựa trên số thứ tự (rank, dạng String).
 * Đã fix lỗi hiển thị bằng cách thêm logic Scale (thu nhỏ) ảnh.
 */
public class IconRenderer extends DefaultTableCellRenderer {

    private ImageIcon goldIcon;
    private ImageIcon silverIcon;
    private ImageIcon bronzeIcon;
    private ImageIcon genericIcon; 
    
    // Kích thước cố định mong muốn cho Icon
    private static final int ICON_SIZE = 24; 
    private static final int ROW_HEIGHT_PADDING = 6; // Padding cho chiều cao hàng

    // Đường dẫn tuyệt đối từ gốc Classpath (src)
    private static final String GOLD_ICON_PATH = "/client/golden_medal.png"; 
    private static final String SILVER_ICON_PATH = "/client/silver_medal.png";
    private static final String BRONZE_ICON_PATH = "/client/bronze_medal.png";
    private static final String GENERIC_ICON_PATH = "/client/medal.png"; 

    public IconRenderer() {
        setHorizontalAlignment(JLabel.CENTER);

        try {
            // Tải icon bằng ClassLoader
            goldIcon = loadIcon(GOLD_ICON_PATH);
            silverIcon = loadIcon(SILVER_ICON_PATH);
            bronzeIcon = loadIcon(BRONZE_ICON_PATH);
            genericIcon = loadIcon(GENERIC_ICON_PATH);

        } catch (Exception e) {
            System.err.println("Lỗi khi khởi tạo IconRenderer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tải và thu nhỏ (scale) ImageIcon.
     */
    private ImageIcon loadIcon(String path) throws IOException {
        // Sử dụng getResourceAsStream() với đường dẫn tuyệt đối
        InputStream is = IconRenderer.class.getResourceAsStream(path); 
        
        if (is != null) {
            BufferedImage img = ImageIO.read(is);
            
            // FIX: Thu nhỏ (Scale) hình ảnh về kích thước cố định
            Image scaledImg = img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH); 
            
            return new ImageIcon(scaledImg); 
        } else {
             // In ra lỗi chi tiết để debug
             System.err.println("LỖI KHÔNG TÌM THẤY ICON: " + path + ". Đảm bảo file nằm trong thư mục client.");
             return null;
        }
    }


    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, 
        boolean hasFocus, int row, int column) 
    {
        // 1. Gọi lớp cha để xử lý nền/màu chữ
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        setText(""); 
        setIcon(null);

        // --- Logic Chuyển Rank (String) thành Icon ---
        if (value instanceof String) {
            try {
                int rank = Integer.parseInt((String) value);
                ImageIcon icon = null;
                
                switch (rank) {
                    case 1:
                        icon = goldIcon;
                        break;
                    case 2:
                        icon = silverIcon;
                        break;
                    case 3:
                        icon = bronzeIcon;
                        break;
                    default:
                        icon = genericIcon; 
                        break;
                }
                
                if (icon != null) {
                    setIcon(icon);
                    setHorizontalAlignment(JLabel.CENTER);
                    
                    // 2. Điều chỉnh chiều cao hàng nếu cần (dựa trên ICON_SIZE đã cố định)
                    int requiredHeight = ICON_SIZE + ROW_HEIGHT_PADDING;
                    if (requiredHeight > table.getRowHeight(row)) {
                        // Chỉ set row height nếu cần thiết (có thể gây lỗi performance nếu set liên tục)
                        table.setRowHeight(row, requiredHeight); 
                    }
                } else {
                    // Fallback: nếu icon null, hiển thị số thứ tự
                    setText(String.valueOf(rank));
                    setHorizontalAlignment(JLabel.CENTER);
                }
                
            } catch (NumberFormatException e) {
                // Nếu không phải số, hiển thị văn bản gốc
                setText((String) value);
                setHorizontalAlignment(JLabel.LEFT);
            }
        } 
        // 3. Giữ lại logic xử lý ImageIcon trực tiếp (cho trường hợp GameFrame gửi nhầm đối tượng)
        else if (value instanceof ImageIcon) {
            ImageIcon icon = (ImageIcon) value;
            setIcon(icon);
            setHorizontalAlignment(JLabel.CENTER);
            if (icon.getIconHeight() > 0 && icon.getIconHeight() + 6 > table.getRowHeight(row)) {
                table.setRowHeight(row, icon.getIconHeight() + 6); 
            }
        }
        else {
            setText(value != null ? value.toString() : "");
            setHorizontalAlignment(JLabel.LEFT);
        }
        
        return this;
    }
}