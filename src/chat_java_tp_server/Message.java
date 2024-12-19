package chat_java_tp_server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private int idMessage;
    private int idSend;
    private int idReceive;
    private LocalDateTime createdAt;
    private String content;

    // Constructeur avec les paramètres nécessaires
    public Message( ) {  
    } 
    
    // Constructeur avec les paramètres nécessaires
    public Message( int idSend, int idReceive, String content) { 
        this.idSend = idSend;
        this.idReceive = idReceive;
        this.content = content; 
    } 

    
    // Getters et Setters
    public int getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public int getIdSend() {
        return idSend;
    }

    public void setIdSend(int idSend) {
        this.idSend = idSend;
    }

    public int getIdReceive() {
        return idReceive;
    }

    public void setIdReceive(int idReceive) {
        this.idReceive = idReceive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    } 
    
    @Override
    public String toString() {
        return "Message{" +
                "idMessage=" + idMessage +
                ", idSend=" + idSend +
                ", idReceive=" + idReceive +
                ", createdAt=" + createdAt +
                ", content='" + content + '\'' +
                '}';
    }
}
