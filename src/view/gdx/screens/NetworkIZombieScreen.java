package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import model.enums.MatchRole;
import model.enums.MiniGameType;
import model.game.minigame.arcade.IZombieAction;
import model.game.minigame.arcade.IZombieEngine;
import model.game.minigame.arcade.IZombieEngine.PlantSpec;
import model.game.minigame.arcade.IZombieEngine.ZombieSpec;
import model.game.minigame.arcade.IZombieMatch;
import model.game.minigame.arcade.IZombieMatch.Snapshot;
import network.client.ClientSession;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.Payloads;
import view.gdx.animation.AnimationLibrary;
import view.gdx.core.PvzGdxGame;
import view.gdx.render.ArcadeRenderer;
import view.gdx.ui.HudArt;
import view.gdx.ui.SeedCard;
import view.gdx.ui.UiSkinProvider;

public final class NetworkIZombieScreen extends SnapshotIZombieScreen {

  private static final float REACTION_SECONDS = 4f;
  private static final float REACTION_ICON_FILL = 0.8f;
  private static final float STICKER_ROW_FILL = 1.3f;

  /** Three of each, as the doc asks. The emoji are the game's own icons; see {@link #emojiArt}. */
  private static final String[] TEXT_REACTIONS = {"Nice one!", "Is that all?", "Good game!"};
  private static final String[] EMOJI_REACTIONS = {"brain", "sun", "plantfood"};

  private static final Sticker[] STICKER_REACTIONS = {
      new Sticker("Chomp!", AnimationLibrary.PLANTS, "chomper", "bite"),
      new Sticker("Smash!", AnimationLibrary.ZOMBIES, "zombietutorialgargantuar", "smash_left"),
      new Sticker("Spin!", AnimationLibrary.ZOMBIES, "zombiedarkjugglerdefault", "spin"),
  };

  private record Sticker(String label, String kind, String rig, String clip) {}

  private final ClientSession session = ClientSession.getInstance();
  private final HudArt hudArt = new HudArt();
  private final Payloads.MatchFound match;
  private final MatchRole role;
  private final List<SeedCard> cards = new ArrayList<>();
  private final List<String> keys = new ArrayList<>();
  private final List<Integer> costs = new ArrayList<>();
  private final List<Boolean> drawable = new ArrayList<>();
  private final Consumer<NetworkMessage> serverEvents = this::onServerEvent;

  private volatile Snapshot snapshot;
  private volatile Payloads.MatchEnded outcome;

  private String chosen;
  private Label reactionLabel;
  private TextureRegion reactionIcon;
  private Sticker reactionSticker;
  private Object reactionPlaybackKey;
  private float reactionLeft;
  private boolean outcomeRecorded;

  public NetworkIZombieScreen(PvzGdxGame game, Payloads.MatchFound match) {
    super(game, MiniGameType.I_ZOMBIE, match.level());
    this.match = match;
    this.role = match.role();
  }

  @Override
  public void show() {
    super.show();
    session.onEvent(serverEvents);
  }

  private void onServerEvent(NetworkMessage message) {
    if (message.type() == MessageType.MATCH_STATE_UPDATE) {
      Payloads.MatchStateUpdate update = message.payloadAs(Payloads.MatchStateUpdate.class);
      if (update != null && match.matchId().equals(update.matchId())) {
        snapshot = update.state();
      }
    } else if (message.type() == MessageType.MATCH_ENDED) {
      Payloads.MatchEnded ended = message.payloadAs(Payloads.MatchEnded.class);
      if (ended != null && match.matchId().equals(ended.matchId())) {
        outcome = ended;
      }
    } else if (message.type() == MessageType.REACTION_EVENT) {
      Payloads.ReactionEvent reaction = message.payloadAs(Payloads.ReactionEvent.class);
      if (reaction != null) {
        Gdx.app.postRunnable(() -> showReaction(reaction));
      }
    }
  }

  @Override
  protected String title() {
    return "I, Zombie  -  vs " + match.opponent();
  }

  @Override
  protected String statusLine() {
    Snapshot state = snapshot;
    if (state == null) {
      return "waiting for the server's first board...";
    }
    String purse = role == MatchRole.ZOMBIES
        ? "zombie-sun " + state.zombieSun()
        : "sun " + state.plantSun();
    return "you are the " + (role == MatchRole.ZOMBIES ? "zombies" : "plants") + "   -   " + purse
        + "   -   brains " + state.brainsRemaining() + "/" + IZombieEngine.BRAINS
        + "   -   " + clock(state.ticksRemaining()) + " left"
        + (chosen == null ? "   -   pick something to place" : "   -   placing " + chosen);
  }

  @Override
  protected void buildPicker(Table picker, Skin skin) {
    cards.clear();
    keys.clear();
    costs.clear();
    drawable.clear();
    picker.add(new Label(role == MatchRole.ZOMBIES ? "deploy" : "plant", skin, "secondary"))
        .padRight(10f);
    if (role == MatchRole.ZOMBIES) {
      for (ZombieSpec spec : IZombieEngine.zombieTypesFor(match.level())) {
        addCard(picker, skin, spec.name, spec.cost, art.zombiePortrait(spec.name));
      }
    } else {
      for (PlantSpec spec : IZombieEngine.availablePlantTypes()) {
        addCard(picker, skin, spec.name, spec.cost, art.plantPortrait(spec.name));
      }
    }
    picker.add(reactions(skin)).padLeft(18f);
    paintCards();
  }

  private void addCard(Table picker, Skin skin, String name, int cost, TextureRegion portrait) {
    SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, name, name, portrait, hudArt,
        this::choose);
    card.withCost(cost);
    picker.add(card).width(114f).height(128f).padRight(4f);
    cards.add(card);
    keys.add(name);
    costs.add(cost);
    drawable.add(portrait != null);
  }

  private Table reactions(Skin skin) {
    Table bar = new Table();
    Table words = new Table();
    for (String text : TEXT_REACTIONS) {
      TextButton button = new TextButton(text, skin, UiSkinProvider.BUTTON_BROWN);
      button.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          send(Payloads.ReactionKind.TEXT, text);
        }
      });
      words.add(button).width(150f).height(38f).padBottom(3f).row();
    }
    bar.add(words).padRight(8f);

    Table icons = new Table();
    // The skin styles its buttons as TextButtons, so a plain Button has to borrow their
    // drawables rather than ask for a style name that was never registered under that type.
    TextButton.TextButtonStyle brown =
        skin.get(UiSkinProvider.BUTTON_BROWN, TextButton.TextButtonStyle.class);
    Button.ButtonStyle iconStyle = new Button.ButtonStyle(brown.up, brown.down, brown.checked);
    for (String emoji : EMOJI_REACTIONS) {
      Button button = new Button(iconStyle);
      TextureRegion art = emojiArt(emoji);
      if (art != null) {
        button.add(new Image(art)).size(30f);
      } else {
        button.add(new Label(emoji, skin, "secondary"));
      }
      button.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          send(Payloads.ReactionKind.EMOJI, emoji);
        }
      });
      icons.add(button).size(46f).padBottom(3f).row();
    }
    bar.add(icons).padRight(8f);

    Table stickers = new Table();
    for (Sticker sticker : STICKER_REACTIONS) {
      TextButton button = new TextButton(sticker.label(), skin, UiSkinProvider.BUTTON_PURPLE);
      button.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          send(Payloads.ReactionKind.STICKER, sticker.label());
        }
      });
      stickers.add(button).width(110f).height(38f).padBottom(3f).row();
    }
    bar.add(stickers).padRight(10f);

    reactionLabel = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    reactionLabel.setWrap(true);
    bar.add(reactionLabel).width(230f);
    return bar;
  }

  private TextureRegion emojiArt(String emoji) {
    return hudArt.find(emoji);
  }

  private void send(Payloads.ReactionKind kind, String value) {
    String matchId = match.matchId();
    background("reaction", () -> {
      session.sendReaction(matchId, kind, value);
      return null;
    });
  }

  private void showReaction(Payloads.ReactionEvent reaction) {
    reactionIcon = reaction.kind() == Payloads.ReactionKind.EMOJI
        ? emojiArt(reaction.value())
        : null;
    reactionSticker = reaction.kind() == Payloads.ReactionKind.STICKER
        ? stickerFor(reaction.value())
        : null;
    reactionPlaybackKey = new Object();
    reactionLeft = REACTION_SECONDS;
    if (reactionLabel != null) {
      boolean pictureSaysItAll = reactionIcon != null;
      reactionLabel.setText(reaction.fromUsername() + ": "
          + (pictureSaysItAll ? "" : reaction.value()));
    }
  }

  private static Sticker stickerFor(String label) {
    for (Sticker sticker : STICKER_REACTIONS) {
      if (sticker.label().equals(label)) {
        return sticker;
      }
    }
    return null;
  }

  private void choose(String type) {
    chosen = type.equals(chosen) ? null : type;
    paintCards();
  }

  @Override
  protected boolean clearSelection() {
    if (chosen == null) {
      return false;
    }
    chosen = null;
    paintCards();
    return true;
  }

  @Override
  protected void refreshPicker() {
    paintCards();
    if (reactionLeft <= 0f && reactionLabel != null && reactionLabel.getText().length() > 0) {
      reactionLabel.setText("");
    }
  }

  private void paintCards() {
    Snapshot state = snapshot;
    for (int i = 0; i < cards.size(); i++) {
      String name = keys.get(i);
      SeedCard card = cards.get(i);
      int recharge = rechargeOf(state, name);
      boolean affordable = purse(state) >= costs.get(i);
      card.setStatus(statusFor(recharge, affordable, drawable.get(i)));
      card.setSelected(name.equals(chosen));
      card.setTint(recharge > 0 || !affordable ? UNAVAILABLE : READY);
    }
  }

  private int purse(Snapshot state) {
    if (state == null) {
      return 0;
    }
    return role == MatchRole.ZOMBIES ? state.zombieSun() : state.plantSun();
  }

  private int rechargeOf(Snapshot state, String name) {
    if (state == null) {
      return 0;
    }
    Map<String, Integer> table =
        role == MatchRole.ZOMBIES ? state.zombieRecharge() : state.plantRecharge();
    Integer left = table == null ? null : table.get(name);
    return left == null ? 0 : left;
  }

  private static String statusFor(int rechargeTicks, boolean affordable, boolean drawable) {
    if (rechargeTicks > 0) {
      return String.format("recharging %.1fs",
          rechargeTicks / (double) IZombieEngine.TICKS_PER_SECOND);
    }
    if (!affordable) {
      return "need sun";
    }
    return drawable ? "ready" : "ready (no art)";
  }

  @Override
  protected void tickEngine() {}

  @Override
  protected boolean canPause() {
    return false;
  }

  @Override
  protected String leaveLabel() {
    return "Leave";
  }

  @Override
  protected String leaveButtonLabel() {
    return "Back to the lobby";
  }

  @Override
  protected void leave() {
    game.switchScreen(new MultiplayerScreen(game));
  }

  /** Only the zombie side places into a lane; the plant side plants into single tiles. */
  @Override
  protected boolean highlightsWholeRow() {
    return role == MatchRole.ZOMBIES;
  }

  @Override
  protected String onCellClicked(int row, int column) {
    if (chosen == null) {
      return "pick something from the row above first";
    }
    IZombieAction action = role == MatchRole.ZOMBIES
        ? IZombieAction.placeZombie(chosen, row, column)
        : IZombieAction.placePlant(chosen, row, column);
    String matchId = match.matchId();
    // The socket call blocks, so it cannot happen on the render thread; the server's verdict is
    background("action", () -> session.sendAction(matchId, action));
    return null;
  }

  private void background(String name, ProtocolCall call) {
    Thread worker = new Thread(() -> {
      String message;
      try {
        message = call.run();
      } catch (IOException e) {
        message = "error: the server did not answer (" + e.getMessage() + ")";
      }
      String toast = message;
      if (toast != null) {
        Gdx.app.postRunnable(() -> toast(toast));
      }
    }, "izombie-" + name);
    worker.setDaemon(true);
    worker.start();
  }

  private interface ProtocolCall {
    /** @return a message to show the player, or null to say nothing */
    String run() throws IOException;
  }

  @Override
  protected Snapshot currentSnapshot() {
    return snapshot;
  }

  @Override
  protected void drawWorld(float delta) {
    super.drawWorld(delta);
    Batch batch = context().getBatch();
    if (reactionLeft > 0f) {
      reactionLeft -= delta;
      // Corner of the board, out of the lanes that matter, as the doc suggests.
      if (reactionSticker != null) {
        art.drawSticker(batch, reactionPlaybackKey, reactionSticker.kind(),
            reactionSticker.rig(), reactionSticker.clip(),
            COLUMNS - 1, ROWS - 1, STICKER_ROW_FILL);
      } else if (reactionIcon != null) {
        art.drawProp(batch, reactionIcon, COLUMNS - 1, ROWS - 1, REACTION_ICON_FILL);
      }
    }
  }

  @Override
  protected boolean engineFinished() {
    return outcome != null;
  }

  @Override
  protected boolean engineWon() {
    Payloads.MatchEnded ended = outcome;
    String me = session.getUsername();
    return ended != null && ended.winner() != null && ended.winner().equalsIgnoreCase(me);
  }

  @Override
  protected String outcomeWon() {
    Payloads.MatchEnded ended = outcome;
    return "You win - " + (ended == null ? "the match is over" : ended.reason()) + ".";
  }

  @Override
  protected String outcomeLost() {
    Payloads.MatchEnded ended = outcome;
    if (ended == null || ended.winner() == null) {
      return "The match ended without a winner.";
    }
    return ended.winner() + " wins - " + ended.reason() + ".";
  }

  @Override
  protected void recordOutcome(boolean won) {
    if (outcomeRecorded) {
      return;
    }
    outcomeRecorded = true;
    super.recordOutcome(won);
    Snapshot state = snapshot;
    int score = IZombieMatch.scoreFor(state, role);
    if (score <= 0 || !session.isAuthenticated()) {
      return;
    }
    background("score", () -> {
      Payloads.ScoreResponse response = session.submitScore(score);
      return response.improved()
          ? "New leaderboard record: " + response.bestScore()
          : "Scored " + score + "; your record is still " + response.bestScore();
    });
  }

  @Override
  public void hide() {
    session.removeEventListener(serverEvents);
    super.hide();
  }

  @Override
  public void dispose() {
    session.removeEventListener(serverEvents);
    hudArt.dispose();
    super.dispose();
  }
}
