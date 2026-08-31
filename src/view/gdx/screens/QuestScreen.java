package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
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
import view.gdx.ui.LayeredDrawable;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical Travel Log, the screen behind the Phase 1 Quest Menu.
 *
 * <p>Quest.claimReward already refuses an unfinished or claimed quest, so the claim rules are not
 * repeated here; the button state is only there to show which quests are claimable.
 *
 * <p>Mini-games, the other half of the Phase 1 Travel Log, live on {@link MiniGamesScreen}.
 */
public final class QuestScreen extends MenuScreen {

  private static final Color CHIP = new Color(0f, 0f, 0f, 0.16f);
  /** Top padding on an epic card, enough to clear the panel's blue header band. */
  private static final float EPIC_HEADER_CLEARANCE = 44f;

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
        list.add(questCard(user, quest)).growX().padBottom(8f).row();
      }
    }

    ScrollPane scroll = new ScrollPane(list, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    content.add(scroll).grow().row();

    Table footer = new Table();
    footer.defaults().pad(6f).width(220f).height(60f);
    footer.add(button("Mini-Games", UiSkinProvider.BUTTON_GREEN,
        () -> go(new MiniGamesScreen(game))));
    footer.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    content.add(footer).padTop(8f).row();
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

  /**
   * One quest, as a card.
   *
   * <p>The old row was four stacked "label: value" lines with an oversized grey button under them,
   * which made the least useful thing on the row the most prominent and ran two quests together
   * with nothing between them. This gives each quest its own surface, puts the title first, states
   * the goal once, and shows how far along it is as a bar -- the quests already carry progress and
   * a target, and nothing was drawing them.
   */
  private Table questCard(User user, Quest quest) {
    boolean claimable = quest.isCompleted() && !quest.isRewardClaimed();

    Table card = new Table();
    card.setBackground(questPanel(quest, claimable));
    // The epic panel carries a blue header band across its top; content starts under it rather
    // than on it, or the goal line ends up as dark text on a dark blue stripe.
    card.pad(isEpic(quest) ? EPIC_HEADER_CLEARANCE : 14f, 18f, 14f, 18f);
    card.top().left();

    Table heading = new Table();
    heading.add(new Label(quest.getTitle(), skin, UiSkinProvider.LABEL_MEDIUM)).left();
    heading.add(chip(categoryOf(quest))).padLeft(10f).left().expandX();
    card.add(heading).growX().colspan(2).padBottom(2f).row();

    Label goal = new Label(quest.getDescription() == null ? "-" : quest.getDescription(),
        skin, "secondary");
    goal.setWrap(true);
    card.add(goal).left().width(620f).colspan(2).padBottom(6f).row();

    card.add(progress(quest)).left().width(620f).height(22f);
    card.add(action(user, quest, claimable)).right().width(200f).height(52f).padLeft(16f).row();

    Table footer = new Table();
    footer.add(new Label("reward", skin, "secondary")).padRight(8f);
    footer.add(rewardChip(quest));
    card.add(footer).left().colspan(2).padTop(6f).row();
    return card;
  }

  /**
   * The Travel Log's own panel for this quest's state.
   *
   * <p>Epic challenges get the blue-headed panel the game reserves for them, everything else the
   * plain one, and a finished quest wears the green completion frame over whichever it is -- which
   * is why the two are layered rather than swapped: the frames are hollow outlines meant to go on
   * top of a panel, not to be one.
   */
  private Drawable questPanel(Quest quest, boolean claimable) {
    boolean epic = isEpic(quest);
    Drawable base = skin.getDrawable(
        epic ? UiSkinProvider.QUEST_PANEL_EPIC : UiSkinProvider.QUEST_PANEL);
    if (!quest.isCompleted() && !claimable) {
      return base;
    }
    return new LayeredDrawable(base, skin.getDrawable(
        epic ? UiSkinProvider.QUEST_PANEL_EPIC_DONE : UiSkinProvider.QUEST_PANEL_DONE));
  }

  private static boolean isEpic(Quest quest) {
    return categoryOf(quest).toLowerCase().contains("epic");
  }

  /** The bar, plus the same words the terminal build prints, so the state is never ambiguous. */
  private Table progress(Quest quest) {
    Table holder = new Table();
    holder.left();

    // A quest only learns its target the first time it is progressed, so an untouched one reports
    // zero. Treating that as a range of one keeps the bar on every card and reads correctly for
    // both kinds of quest: empty for a counter nobody has started, and empty for a one-shot
    // condition nobody has met yet.
    float target = Math.max((float) quest.getQuestTarget(), 1f);
    float done = quest.isCompleted() ? target
        : Math.min((float) quest.getProgressOfQuest(), target);

    ProgressBar bar = new ProgressBar(0f, target, 1f, false, skin,
        quest.isCompleted() ? "xp_green" : "xp_yellow");
    bar.setValue(done);
    bar.setAnimateDuration(0f);
    holder.add(bar).width(360f).height(20f).padRight(12f);
    holder.add(new Label(describeStatus(quest), skin, "secondary"));
    return holder;
  }

  /** A small pill for the quest's category, so the list is scannable when the filter is All. */
  private Table chip(String text) {
    Table chip = new Table();
    chip.setBackground(skin.newDrawable(UiSkinProvider.WHITE_PIXEL, CHIP));
    chip.pad(2f, 10f, 2f, 10f);
    chip.add(new Label(text, skin, "secondary"));
    return chip;
  }

  /** The reward, with the currency icon when the reward is one. */
  private Table rewardChip(Quest quest) {
    Table reward = new Table();
    String text = quest.getRewardType() == null ? "-" : quest.getRewardType();
    String lower = text.toLowerCase();
    String icon = lower.contains("gem") || lower.contains("diamond")
        ? UiSkinProvider.QUEST_GEM_ICON
        : lower.contains("coin") ? UiSkinProvider.QUEST_COIN_ICON : null;
    if (icon != null) {
      reward.add(new Image(skin.getDrawable(icon))).size(30f).padRight(6f);
    }
    reward.add(new Label(text, skin, UiSkinProvider.LABEL_MEDIUM));
    return reward;
  }

  private static String categoryOf(Quest quest) {
    return quest.getCategory() == null || quest.getCategory().isBlank()
        ? "general" : quest.getCategory();
  }

  /** Claim when there is something to claim, otherwise a quiet statement of where it stands. */
  private Actor action(User user, Quest quest, boolean claimable) {
    if (claimable) {
      return button("Claim Reward", UiSkinProvider.BUTTON_GREEN, () -> claim(user, quest));
    }
    // The green style has no disabled drawable, so a disabled green button still looks live.
    TextButton disabled = new TextButton(
        quest.isRewardClaimed() ? "Claimed" : "In progress",
        skin, UiSkinProvider.BUTTON_BROWN);
    disabled.setDisabled(true);
    disabled.getColor().a = 0.5f;
    return disabled;
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
    runAsync(
        () -> {
          UserManager.getInstance().updateCurrentUserGameState();
          return null;
        },
        ignored -> toast("Reward claimed for \"" + quest.getTitle() + "\"!"),
        e -> toast(e.getMessage()));
    refresh();
  }
}
