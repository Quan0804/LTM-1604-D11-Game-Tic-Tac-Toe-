package mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class LogClient {
    private final MongoCollection<Document> collection;

    public LogClient() {
        MongoDatabase db = MongoDBConnection.getDatabase();
        this.collection = db.getCollection("logs"); // collection name
    }

    public void logConnection(String clientName, String status) {
        Document log = new Document("client", clientName)
                .append("status", status)
                .append("time", new java.util.Date());
        collection.insertOne(log);
        System.out.println("Log inserted for client: " + clientName);
    }
}
