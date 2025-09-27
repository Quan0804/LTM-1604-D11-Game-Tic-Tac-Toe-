package database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MongoDBConnector {
    // Thay đổi tên DB và URI cho khớp với cấu hình của bạn
    private final String uri = "mongodb://localhost:27017";
    private final String dbName = "tictactoe_db";
    private final String usersCollectionName = "users";
    private final String historyCollectionName = "history"; // Collection mới cho lịch sử

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> usersCollection;
    private MongoCollection<Document> historyCollection; // Khai báo collection lịch sử

    public MongoDBConnector() {
        try {
            mongoClient = MongoClients.create(uri);
            database = mongoClient.getDatabase(dbName);
            usersCollection = database.getCollection(usersCollectionName);
            historyCollection = database.getCollection(historyCollectionName); // Khởi tạo collection lịch sử

            // Đảm bảo chỉ mục (index) cho việc truy vấn nhanh hơn
            usersCollection.createIndex(new Document("username", 1));
        } catch (Exception e) {
            System.err.println("LỖI KẾT NỐI MONGODB! Vui lòng kiểm tra Server và URI.");
            e.printStackTrace();
        }
    }

    // --- LOGIC ĐĂNG KÝ VÀ XÁC THỰC (Đã sửa) ---

    public boolean registerUser(String username, String password) {
        if (usersCollection.find(new Document("username", username)).first() != null) {
            return false; // Người dùng đã tồn tại
        }
        Document newUser = new Document("username", username)
                .append("password", password)
                .append("wins", 0); // THÊM TRƯỜNG WINS MỚI
        usersCollection.insertOne(newUser);
        return true;
    }

    public boolean validateUser(String username, String password) {
        Document user = usersCollection.find(
                new Document("username", username).append("password", password)
        ).first();
        return user != null;
    }

    // --- LOGIC MỚI: CẬP NHẬT VÀ TRUY VẤN THỐNG KÊ ---

    /**
     * Cập nhật số trận thắng (wins) cho người chơi.
     */
    public void recordWin(String username) {
        usersCollection.updateOne(
                new Document("username", username),
                new Document("$inc", new Document("wins", 1)) // Tăng giá trị 'wins' lên 1
        );
        // Ghi lại lịch sử chiến thắng
        recordHistory(username, "WIN");
    }
    
    /**
     * Ghi lại lịch sử kết thúc ván đấu (Thắng, Thua, Hòa).
     */
    public void recordHistory(String username, String result) {
        Document historyEntry = new Document("username", username)
                .append("result", result)
                .append("timestamp", System.currentTimeMillis());
        historyCollection.insertOne(historyEntry);
    }
    
    /**
     * Lấy Top người chơi có số trận thắng cao nhất.
     * @param limit Số lượng người chơi muốn lấy.
     * @return List các Document chứa {username, wins}.
     */
    public List<Document> getTopPlayers(int limit) {
        // Truy vấn từ usersCollection, sắp xếp giảm dần theo 'wins'
        return usersCollection.find()
               .sort(Sorts.descending("wins"))
               .limit(limit)
               .into(new ArrayList<>());
    }
    
    /**
     * Lấy lịch sử kết quả gần đây nhất.
     * @param limit Số lượng lịch sử muốn lấy.
     * @return List các Document chứa {username, result, timestamp}.
     */
    public List<Document> getRecentHistory(int limit) {
        // Truy vấn từ historyCollection, sắp xếp giảm dần theo 'timestamp'
        return historyCollection.find()
                .sort(Sorts.descending("timestamp"))
                .limit(limit)
                .into(new ArrayList<>());
    }
}