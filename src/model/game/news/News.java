package model.game.news;

public class News {
  private final boolean isRead;

  News(boolean isRead) {
    this.isRead = isRead;
  }

  public boolean isRead() {
    return isRead;
  }
}
