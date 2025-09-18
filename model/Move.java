package model;

import org.bson.Document;
import java.util.Date;

public class Move {
    private String matchId;
    private String playerId;
    private String move;
    private int turn;
    private Date timestamp;

    public Move(String matchId, String playerId, String move, int turn) {
        this.matchId = matchId;
        this.playerId = playerId;
        this.move = move;
        this.turn = turn;
        this.timestamp = new Date();
    }

    public Document toDocument() {
        return new Document("matchId", matchId)
                .append("playerId", playerId)
                .append("move", move)
                .append("turn", turn)
                .append("timestamp", timestamp);
    }
}
