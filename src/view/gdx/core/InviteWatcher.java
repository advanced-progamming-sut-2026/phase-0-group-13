package view.gdx.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.io.IOException;
import model.core.GameSession;
import network.client.ClientSession;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.Payloads;
import view.gdx.screens.BaseScreen;
import view.gdx.screens.NetworkIZombieScreen;
import view.gdx.ui.Popup;
import view.gdx.ui.UiSkinProvider;

/**
 * Match invites and match starts, watched for the whole run of the client rather than by whichever
 * screen happens to be open.
 */
public final class InviteWatcher {

  private final PvzGdxGame game;
  private final ClientSession session = ClientSession.getInstance();

  private Payloads.MatchInviteEvent pending;
  private Stage shownOn;

  public InviteWatcher(PvzGdxGame game) {
    this.game = game;
    session.onEvent(this::onServerEvent);
  }

  private void onServerEvent(NetworkMessage message) {
    if (message.type() == MessageType.MATCH_INVITE_EVENT) {
      Payloads.MatchInviteEvent invite = message.payloadAs(Payloads.MatchInviteEvent.class);
      if (invite != null) {
        Gdx.app.postRunnable(() -> {
          pending = invite;
          shownOn = null;
          present();
        });
      }
    } else if (message.type() == MessageType.MATCH_FOUND) {
      Payloads.MatchFound found = message.payloadAs(Payloads.MatchFound.class);
      if (found != null) {
        Gdx.app.postRunnable(() -> enterMatch(found));
      }
    }
  }

  public void present() {
    if (pending == null) {
      return;
    }
    Stage stage = currentStage();
    Skin skin = game.getUiSkin().get();
    if (stage == null || skin == null || stage == shownOn) {
      return;
    }
    shownOn = stage;
    Payloads.MatchInviteEvent invite = pending;
    Table body = new Table();
    body.add(new Label(invite.fromUsername() + " wants to play I, Zombie.", skin,
        UiSkinProvider.LABEL_MEDIUM));
    Popup.show(stage, skin, "Match invite", body,
        "Accept", () -> answer(invite, true),
        "Reject", () -> answer(invite, false));
  }

  private void answer(Payloads.MatchInviteEvent invite, boolean accepted) {
    pending = null;
    shownOn = null;
    Thread worker = new Thread(() -> {
      try {
        session.answerInvite(invite.inviteId(), accepted);
      } catch (IOException e) {
        System.out.println("could not answer the invite: " + e.getMessage());
      }
    }, "invite-answer");
    worker.setDaemon(true);
    worker.start();
  }

  private void enterMatch(Payloads.MatchFound found) {
    pending = null;
    shownOn = null;
    GameSession.end();
    game.switchScreen(new NetworkIZombieScreen(game, found));
  }

  private Stage currentStage() {
    Screen screen = game.getScreen();
    return screen instanceof BaseScreen base ? base.uiStage() : null;
  }
}
