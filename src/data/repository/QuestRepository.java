package data.repository;

import java.util.List;
import model.game.quest.Quest;

public class QuestRepository {
  private final List<Quest> quests;

  public QuestRepository(List<Quest> quests) {
    this.quests = quests;
  }

  public List<Quest> getAll() {
    return quests;
  }

  public Quest findQuestByTitle(String title) {
    for (Quest quest : quests) {
      if (quest.title.equalsIgnoreCase(title)) {
        return quest;
      }
    }
    return null;
  }
}
