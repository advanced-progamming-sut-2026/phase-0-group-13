package model.game.news;

import java.util.ArrayList;
import java.util.List;

public class AllNews {
  private List<News> readNews;
  private List<News> unreadNews;

  public AllNews() {
    this.readNews = new ArrayList<>();
    this.unreadNews = new ArrayList<>();
  }

  public AllNews(List<News> unreadNews) {
    this.readNews = new ArrayList<>();
    this.unreadNews = unreadNews != null ? unreadNews : new ArrayList<>();
  }

  public void addNews(News news) {
    if (news == null) return;
    if (unreadNews == null) unreadNews = new ArrayList<>();
    unreadNews.add(0, news);
  }

  public void markAsRead(News news) {
    if (news == null || unreadNews == null || !unreadNews.contains(news)) return;
    news.markAsRead();
    unreadNews.remove(news);
    if (readNews == null) readNews = new ArrayList<>();
    readNews.add(0, news);
  }

  public void markAllAsRead() {
    if (unreadNews == null) return;
    for (News news : new ArrayList<>(unreadNews)) {
      markAsRead(news);
    }
  }

  public List<News> getReadNews() {
    return readNews != null ? readNews : new ArrayList<>();
  }

  public List<News> getUnreadNews() {
    return unreadNews != null ? unreadNews : new ArrayList<>();
  }

  public int getUnreadCount() {
    return getUnreadNews().size();
  }
}