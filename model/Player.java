package model;

import org.bson.Document;
import java.util.Date;

public class Player {
    private String id;
    private String name;
    private int rank;
    private Date createdAt;

    public Player(String id, String name, int rank) {
        this.id = id;
        this.name = name;
        this.rank = rank;
        this.createdAt = new Date();
    }

    public Document toDocument() {
        return new Document("_id", id)
                .append("name", name)
                .append("rank", rank)
                .append("createdAt", createdAt);
    }
}
