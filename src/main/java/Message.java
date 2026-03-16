import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        JOIN,
        JOIN_FAIL,
        JOIN_SUCCESS,
        GAME_START,
        MOVE,
        GAME_END,
        CHAT,
        REMATCH,
        QUIT
    }

    private MessageType type;
    private String sender;
    private String content;
    private GameSession gameState;
    
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    // Add this new constructor for messages with game state
    public Message(MessageType type, String sender, String content, GameSession gameState) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.gameState = gameState;
    }

    public void setGameState(GameSession gameState) {
        this.gameState = gameState;
    }

    public String toString() {
        return "Type: " + type + ", Sender: " + sender + ", Content: " + content;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public GameSession getGameState() {
        return gameState;
    }
}