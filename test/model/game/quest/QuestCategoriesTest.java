package model.game.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import data.persistence.DataPath;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import model.account.User;
import org.junit.jupiter.api.Test;

/**
 * The doc splits quests into four general categories, one travel-log page each, and gives their
 * priorities: story/unlock quests are critical, epic challenges are high, and daily and repeatable
 * quests are medium and low. The roster only carried the first three, so the fourth page was
 * always empty.
 *
 * <p>The roster is located through DataPath, the same resolution the game uses, so this reads the
 * exact file it loads rather than a copy.
 */
class QuestCategoriesTest {

  private static List<Quest> roster() throws IOException {
    Path path = DataPath.getInstance().getPath("quests");
    assertNotNull(path, "DataPath has no entry for the quest roster");
    assertTrue(Files.exists(path), "quest roster not found at " + path.toAbsolutePath());
    Type listType = new TypeToken<List<Quest>>() {}.getType();
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return new Gson().fromJson(reader, listType);
    }
  }

  @Test
  void theRosterCoversAllFourCategories() throws IOException {
    Set<String> categories = new LinkedHashSet<>();
    for (Quest quest : roster()) {
      assertNotNull(quest.getCategory(), quest.getTitle() + " has no category");
      categories.add(quest.getCategory());
    }
    assertEquals(Set.of("Daily", "Main", "Epic Challenge", "Repeatable"), categories);
  }

  @Test
  void everyRepeatableQuestIsTrackable() throws IOException {
    List<Quest> repeatable = new ArrayList<>();
    for (Quest quest : roster()) {
      if ("Repeatable".equals(quest.getCategory())) {
        repeatable.add(quest);
      }
    }
    assertFalse(repeatable.isEmpty(), "no Repeatable quests in the roster");

    // A quest whose condition matches neither the event keywords nor the contextual engine can
    // never complete -- it would be dead data on its own page.
    User user = new User("questcat", "hash", "a@b.c", "nick", "male");
    user.seedQuestsIfNeeded(roster());
    for (Quest quest : repeatable) {
      assertTrue(quest.isContextual() || tracksAnEvent(user, quest),
          quest.getTitle() + " cannot be progressed by any event or context condition");
      assertEquals("Low", quest.getPriority(),
          quest.getTitle() + ": the doc puts repeatable quests at the lowest priority");
    }
  }

  /** True when firing some supported event moves this quest's progress off zero. */
  private static boolean tracksAnEvent(User user, Quest template) {
    for (String event : new String[] {"KILL_ZOMBIE", "COLLECT_SUN", "STAGE_CLEAR",
        "MINIGAME_CLEAR", "PLANT_UNLOCKED", "PLANT_PURCHASED"}) {
      user.triggerQuestEvent(event, 1);
    }
    for (Quest owned : user.getQuests()) {
      if (template.getTitle().equals(owned.getTitle())) {
        return owned.getProgressOfQuest() > 0;
      }
    }
    return false;
  }

  @Test
  void anExistingSaveGainsQuestsAddedToTheRosterWithoutLosingProgress() throws IOException {
    List<Quest> all = roster();
    User user = new User("questmigrate", "hash", "a@b.c", "nick", "male");

    // seeded before the Repeatable page existed: everything but that category
    List<Quest> old = new ArrayList<>();
    for (Quest quest : all) {
      if (!"Repeatable".equals(quest.getCategory())) {
        old.add(quest);
      }
    }
    user.seedQuestsIfNeeded(old);
    assertEquals(old.size(), user.getQuests().size());

    Quest first = user.getQuests().get(0);
    first.addProgress(1, 100);
    double progressBefore = first.getProgressOfQuest();
    assertTrue(progressBefore > 0);

    user.seedQuestsIfNeeded(all);

    assertEquals(all.size(), user.getQuests().size(), "the new category was not appended");
    assertEquals(progressBefore, user.getQuests().get(0).getProgressOfQuest(),
        "appending new quests must not reset the ones already in progress");

    // and running it again adds nothing further
    user.seedQuestsIfNeeded(all);
    assertEquals(all.size(), user.getQuests().size(), "quests were duplicated on a second seed");
  }
}
