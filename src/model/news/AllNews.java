package model.news;

import java.util.List;

public class AllNews {
    private List<news> ReadedNews;
    private List<news> UnReadedNews;
    public AllNews(List<news> UnReadedNews) {
        this.UnReadedNews = UnReadedNews;
    }
    public List<news> getReadedNews() {
        return ReadedNews;
    }
    public void ReadedNews() {
        // خبر خوانده نشده رو از خوانده نشده ها میبریم تو خوانده شده ها
    }
}
