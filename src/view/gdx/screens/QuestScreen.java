package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import model.account.User;
import model.game.quest.Quest;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical Travel Log, the screen behind the Phase 1 Quest Menu.
 *
 * <p>Quest.claimReward already refuses an unfinished or claimed quest, so the claim rules are not
 * repeated here; the button state is only there to show which quests are claimable.
 *
 * <p>Mini-games, the other half of the Phase 1 Travel Log, are still terminal-only.
 */
public final class QuestScreen extends MenuScreen {

  private static final String ALL = "All";

  private Table content;
  private String activeCategory = ALL;

  public QuestScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Travel Log";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  protected void buildContent(Table content) {
    this.content = content;
    refresh();
  }

  private void refresh() {
    content.clear();

    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      Table panel = panel();
      panel.add(new Label("error: no user logged in", skin, UiSkinProvider.LABEL_MEDIUM)).row();
      panel.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget()))).width(220f);
      content.add(panel);
      return;
    }

    // Same seeding QuestMenuController does, so a fresh account has its quests.
    if (GameDataManager.questRepository != null) {
      user.seedQuestsIfNeeded(GameDataManager.questRepository.getAll());
    }

    content.add(buildFilters(user)).row();

    Table list = panel();
    list.top();
    List<Quest> quests = visibleQuests(user);
    if (quests.isEmpty()) {
      list.add(new Label(user.getQuests().isEmpty()
              ? "No quests are available yet."
              : "No quests found for \"" + activeCategory + "\".",
              skin, UiSkinProvider.LABEL_MEDIUM)).left().row();
    } else {
      for (Quest quest : quests) {
        list.add(questRow(user, quest)).growX().padBottom(10f).row();
      }
    }

    ScrollPane scroll = new ScrollPane(list, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    content.add(scroll).grow().row();

    content.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .width(220f)
        .padTop(8f)
        .row();
  }

  private Table buildFilters(User user) {
    Table filters = new Table();
    filters.defaults().pad(4f).width(190f);
    for (String category : categories(user)) {
      String style = category.equals(activeCategory)
              ? UiSkinProvider.BUTTON_GREEN : UiSkinProvider.BUTTON_BROWN;
      filters.add(button(category, style, () -> {
        activeCategory = category;
        refresh();
      }));
    }
    return filters;
  }

  private List<String> categories(User user) {
    Set<String> found = new LinkedHashSet<>();
    found.add(ALL);
    for (Quest quest : user.getQuests()) {
      if (quest.getCategory() != null && !quest.getCategory().isBlank()) {
        found.add(quest.getCategory());
      }
    }
    return new ArrayList<>(found);
  }

  private List<Quest> visibleQuests(User user) {
    List<Quest> quests = new ArrayList<>();
    for (Quest quest : user.getQuests()) {
      if (ALL.equals(activeCategory)
              || (quest.getCategory() != null
              && quest.getCategory().toLowerCase().contains(activeCategory.toLowerCase()))) {
        quests.add(quest);
      }
    }
    // Same order the terminal Travel Log prints in.
    quests.sort(Comparator.comparingInt(QuestScreen::priorityRank));
    return quests;
  }

  private static int priorityRank(Quest quest) {
    if (quest.getPriority() == null) {
      return 5;
    }
    String priority = quest.getPriority().toLowerCase();
    if (priority.contains("critical")) {
      return 1;
    }
    if (priority.contains("high")) {
      return 2;
    }
    if (priority.contains("medium")) {
      return 3;
    }
    if (priority.contains("low")) {
      return 4;
    }
    return 5;
  }

  private Table questRow(User user, Quest quest) {
    Table row = new Table();
    row.left();

    String heading = "[" + (quest.getPriority() != null ? quest.getPriority() : "-") + "]  "
            + quest.getTitle()
            + "   (" + (quest.getCategory() != null ? quest.getCategory() : "general") + ")";
    row.add(new Label(heading, skin, UiSkinProvider.LABEL_MEDIUM)).left().colspan(2).row();

    detail(row, "goal", quest.getCondition());
    detail(row, "reward", quest.getRewardType());
    detail(row, "status", describeStatus(quest));

    boolean claimable = quest.isCompleted() && !quest.isRewardClaimed();
    if (claimable) {
      row.add(button("Claim Reward", UiSkinProvider.BUTTON_GREEN, () -> claim(user, quest)))
          .colspan(2)
          .width(240f)
          .padTop(6f)
          .row();
    } else {
      // The green style has no disabled drawable, so a disabled green button still looks live.
      TextButton disabled = new TextButton(
              quest.isRewardClaimed() ? "Reward Claimed" : "Not Completed",
              skin, UiSkinProvider.BUTTON_BROWN);
      disabled.setDisabled(true);
      row.add(disabled).colspan(2).width(240f).padTop(6f).row();
    }
    return row;
  }

  private void detail(Table row, String label, String value) {
    row.add(new Label(label, skin, "secondary")).right().padRight(12f).top();
    Label text = new Label(value == null ? "-" : value, skin);
    text.setWrap(true);
    row.add(text).left().width(520f).row();
  }

  private String describeStatus(Quest quest) {
    if (quest.isCompleted() && quest.isRewardClaimed()) {
      return "completed - reward claimed";
    }
    if (quest.isCompleted()) {
      return "COMPLETED - ready to claim";
    }
    if (quest.getQuestTarget() > 0) {
      return String.format("in progress (%d/%d)",
              (int) quest.getProgressOfQuest(), (int) quest.getQuestTarget());
    }
    return "not started";
  }

  private void claim(User user, Quest quest) {
    quest.claimReward(user);
    if (!quest.isRewardClaimed()) {
      // Model refused it, so don't redraw silently.
      toast("error: that quest cannot be claimed right now");
      refresh();
      return;
    }
    try {
      UserManager.getInstance().updateCurrentUserGameState();
      toast("Reward claimed for \"" + quest.getTitle() + "\"!");
    } catch (Exception e) {
      toast(e.getMessage());
    }
    refresh();
  }
}
