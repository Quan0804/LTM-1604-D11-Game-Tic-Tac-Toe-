package model;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;

public class Match {
    private ObjectId id;
    private String player1;
    private String player2;
    private String winner;
    private String status;   
    private Date createdAt;
    private Date endedAt;

    // Constructor khi tạo match mới
    public Match(String player1, String player2) {
        this.id = new ObjectId();
        this.player1 = player1;
        this.player2 = player2;
        this.winner = null;
        this.status = "ongoing";
        this.createdAt = new Date();
        this.endedAt = null;
    }

    // Constructor khi load từ DB
    public Match(ObjectId id, String player1, String player2, String winner, String status, Date createdAt, Date endedAt) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.winner = winner;
        this.status = status;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
    }

    // Convert sang Document để lưu vào MongoDB
    public Document toDocument() {
        Document doc = new Document("_id", id)
                .append("player1", player1)
                .append("player2", player2)
                .append("status", status)
                .append("createdAt", createdAt);

        if (winner != null) doc.append("winner", winner);
        if (endedAt != null) doc.append("endedAt", endedAt);

        return doc;
    }

    // Getter / Setter
    public ObjectId getId() {
        return id;
    }

    public void setWinner(String winner) {
        this.winner = winner;
        this.status = "finished";
        this.endedAt = new Date();
    }
}
