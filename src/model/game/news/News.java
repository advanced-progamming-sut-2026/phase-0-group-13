package model.game.news;

public class News {
  private String type;
  private String targetId;
  private String title;
  private String message;
  private long timestamp;
  private boolean isRead;

  public News() {
  }

  public News(String type, String targetId, String message) {
    this(type, targetId, titleFor(type), message);
  }

  public News(String type, String targetId, String title, String message) {
    this.type = type;
    this.targetId = targetId;
    this.title = title;
    this.message = message;
    this.timestamp = System.currentTimeMillis();
    this.isRead = false;
  }

  /**
   * The headline shown above the body.
   *
   * <p>Falls back to a label derived from the type, so news items saved before this field existed
   * still read properly instead of showing a blank headline.
   */
  public String getTitle() {
    return title == null || title.isBlank() ? titleFor(type) : title;
  }

  private static String titleFor(String type) {
    if (type == null) {
      return "News";
    }
    return switch (type) {
      case "plant" -> "New Plant";
      case "zombie" -> "Zombie Spotted";
      case "stage" -> "New Stage";
      case "quest" -> "Quest Complete";
      default -> "News";
    };
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
