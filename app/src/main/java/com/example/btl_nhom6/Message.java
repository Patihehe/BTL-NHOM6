package com.example.btl_nhom6;

import com.google.firebase.firestore.PropertyName;

public class Message {
    private String messageId;
    private String senderEmail;
    private String receiverEmail;
    private String content;
    private long timestamp;
    private boolean read; // Trạng thái đã đọc

    public Message() {}

    public Message(String senderEmail, String receiverEmail, String content, long timestamp) {
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.content = content;
        this.timestamp = timestamp;
        this.read = false;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @PropertyName("isRead")
    public boolean isRead() { return read; }
    @PropertyName("isRead")
    public void setRead(boolean read) { this.read = read; }
}
