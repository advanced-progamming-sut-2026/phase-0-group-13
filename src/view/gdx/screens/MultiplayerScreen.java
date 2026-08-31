package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import java.io.IOException;
import java.util.function.Consumer;
import network.client.ClientSession;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.Payloads;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;

/**
 * The lobby: where a networked I, Zombie match is arranged.
 *
 * <p>Two ways in, both of them the server's. Naming an opponent sends MATCH_INVITE and the named
 * player gets a popup they can accept or refuse; asking for anybody sends MATCHMAKING_REQUEST and
 * waits in the queue until someone else does the same. Either way the match itself arrives as a
 * MATCH_FOUND event carrying the role, and this screen does nothing but hand that straight to
 * {@link NetworkIZombieScreen}.
 *
 * <p>Every protocol call blocks on the socket, so all of them run on a worker thread and come back
 * through {@code Gdx.app.postRunnable}; the screen keeps repainting while it waits instead of
 * freezing on a server that is thinking about it.
 *
 * <p>Because the match lives on the server, walking out of one is not the same as ending it. If
 * there is still a match in progress when this screen opens, it offers the way back into it.
 */
public final class MultiplayerScreen extends MenuScreen {

  private static final String GAME = "i-zombie";

  private final ClientSession session = ClientSession.getInstance();
  private final Consumer<NetworkMessage> serverEvents = this::onServerEvent;

  private TextField opponentField;
  private Label state;
  private TextButton queueButton;
  private boolean queued;

  public MultiplayerScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Multiplayer";
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/darkagesseason.atlas";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  public void show() {
    super.show();
    session.onEvent(serverEvents);
    // A match found while this screen was being built would otherwise sit there unnoticed.
    Payloads.MatchFound existing = session.getMatch();
    if (existing != null) {
      say("You are already in a match against " + existing.opponent() + ".");
    }
  }

  @Override
  protected void buildContent(Table content) {
    if (!session.isAuthenticated()) {
      content.add(new Label("Log in to play against another player.", skin,
          UiSkinProvider.LABEL_MEDIUM)).pad(20f).row();
      content.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
          .width(200f).height(64f);
      return;
    }

    Table panel = panel();
    panel.add(new Label("I, Zombie  -  two players", skin, UiSkinProvider.LABEL_BIG))
        .colspan(2).padBottom(16f).row();

    opponentField = field(panel, "Opponent", false);
    Table invite = new Table();
    invite.defaults().pad(6f).width(200f).height(64f);
    invite.add(button("Invite", UiSkinProvider.BUTTON_GREEN, this::invite));
    panel.add(invite).colspan(2).padBottom(10f).row();

    Table random = new Table();
    random.defaults().pad(6f).width(240f).height(64f);
    queueButton = button("Random opponent", UiSkinProvider.BUTTON_PURPLE, this::toggleQueue);
    random.add(queueButton);
    Payloads.MatchFound current = session.getMatch();
    if (current != null) {
      random.add(button("Return to match", UiSkinProvider.BUTTON_GREEN,
          () -> enterMatch(current)));
    }
    panel.add(random).colspan(2).row();

    state = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    state.setWrap(true);
    panel.add(state).colspan(2).width(560f).padTop(14f).row();

    content.add(panel).row();
    content.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .width(200f).height(64f).padTop(14f);

    say(session.isConnected()
        ? "Connected as " + session.getUsername() + "."
        : session.getLastError());
  }

  private void invite() {
    String target = opponentField.getText().trim();
    if (target.isEmpty()) {
      say("Type the username you want to play against.");
      return;
    }
    say("Inviting " + target + "...");
    // The server owns "does that account exist" and "is it online"; its answer is what is shown.
    ask(() -> session.invite(target));
  }

  private void toggleQueue() {
    if (queued) {
      say("Leaving the queue...");
      ask(() -> {
        Payloads.Ack ack = session.cancelMatchmaking();
        setQueued(false);
        return ack;
      });
      return;
    }
    say("Looking for an opponent...");
    ask(() -> {
      Payloads.Ack ack = session.requestMatchmaking(GAME);
      setQueued(ack.success());
      return ack;
    });
  }

  private void setQueued(boolean waiting) {
    queued = waiting;
    if (queueButton != null) {
      queueButton.setText(waiting ? "Cancel search" : "Random opponent");
    }
  }

  // MATCH_INVITE_EVENT and MATCH_FOUND belong to InviteWatcher now, so an invite reaches the
  // player wherever they are and only one popup is ever raised for it.
  private void onServerEvent(NetworkMessage message) {
    if (message.type() == MessageType.ACK) {
      // The only unsolicited ACK the server pushes is the host being told an invite was refused.
      Payloads.Ack ack = message.payloadAs(Payloads.Ack.class);
      if (ack != null) {
        Gdx.app.postRunnable(() -> ifStillCurrent(() -> say(ack.message())));
      }
    }
  }

  /**
   * The listener is removed in hide()/dispose(), but an event dispatched right before that can
   * still have queued a postRunnable that only runs on a later frame - by which point the player
   * may have navigated somewhere else entirely. Dropping it here instead of acting on it keeps a
   * stale event from touching a stage that is already gone.
   */
  private void ifStillCurrent(Runnable action) {
    if (game.getScreen() == this) {
      action.run();
    }
  }

  private void enterMatch(Payloads.MatchFound found) {
    setQueued(false);
    go(new NetworkIZombieScreen(game, found));
  }

  /** Runs a blocking protocol call off the render thread and shows whatever it answers. */
  private void ask(ProtocolCall call) {
    Thread worker = new Thread(() -> {
      String message;
      try {
        Payloads.Ack ack = call.run();
        message = ack.message();
      } catch (IOException e) {
        message = "error: the server did not answer (" + e.getMessage() + ")";
      }
      String answer = message;
      Gdx.app.postRunnable(() -> say(answer));
    }, "multiplayer-request");
    worker.setDaemon(true);
    worker.start();
  }

  private interface ProtocolCall {
    Payloads.Ack run() throws IOException;
  }

  private void say(String message) {
    if (state != null && message != null) {
      state.setText(message);
    }
  }

  @Override
  public void hide() {
    session.removeEventListener(serverEvents);
    super.hide();
  }

  @Override
  public void dispose() {
    session.removeEventListener(serverEvents);
    state = null;
    super.dispose();
  }
}
