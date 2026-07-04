package model.game.news;

public class News {
  private String type; // "plant" و "zombie" و"stage"
  private String targetId;
  private String message;
  private long timestamp;
  private boolean isRead;

  public News() {
    // واسه ی جیسون
  }

  public News(String type, String targetId, String message) {
    this.type = type;
    this.targetId = targetId;
    this.message = message;
    this.timestamp = System.currentTimeMillis();
    this.isRead = false;
  }

  public String getType() {
    return type;
  }

  public String getTargetId() {
    return targetId;
  }

  public String getMessage() {
    return message;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public boolean isRead() {
    return isRead;
  }

  public void markAsRead() {
    this.isRead = true;
  }
}