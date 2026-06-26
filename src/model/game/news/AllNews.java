package model.game.news;

import java.util.List;

public class AllNews {
  private List<news> readedNews;
  private final List<news> unReadedNews;

  public AllNews(List<news> UnReadedNews) {
    this.unReadedNews = UnReadedNews;
  }

  public List<news> getReadedNews() {
    return readedNews;
  }

  public void readedNews() {
    // خبر خوانده نشده رو از خوانده نشده ها میبریم تو خوانده شده ها
  }
}
