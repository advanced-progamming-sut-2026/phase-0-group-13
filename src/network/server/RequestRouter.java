package network.server;

import model.enums.MatchRole;
import model.game.minigame.arcade.IZombieAction;
import model.game.minigame.arcade.IZombieMatch;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.Payloads;

/**
 * Turns one client message into one server action.
 *
 * <p>It is also {@link MatchService.Listener}: the match clock calls back here after every tick
 * with the board it just advanced, and once more when a match is over. That is the only place
 * MATCH_STATE_UPDATE and MATCH_ENDED come from, so a client is never told the game moved by
 * anything other than the server that moved it.
 */
public final class RequestRouter implements MatchService.Listener {

  private final SessionManager sessions;
  private final AuthenticationService authentication;
  private final MatchmakingService matchmaking;
  private final MatchService matches;
  private final LeaderboardService leaderboard;

  public RequestRouter(
      SessionManager sessions,
      AuthenticationService authentication,
      MatchmakingService matchmaking,
      MatchService matches,
      LeaderboardService leaderboard) {
    this.sessions = sessions;
    this.authentication = authentication;
    this.matchmaking = matchmaking;
    this.matches = matches;
    this.leaderboard = leaderboard;
  }

  public void handle(ClientConnection connection, NetworkMessage message) {
    if (message.type() == MessageType.PING) {
      connection.send(message.reply(MessageType.PONG, new Payloads.Ack(true, "pong")));
      return;
    }
    if (message.type() == MessageType.REGISTER_REQUEST) {
      register(connection, message);
      return;
    }
    if (message.type() == MessageType.LOGIN_REQUEST) {
      login(connection, message);
      return;
    }
    if (!connection.isAuthenticated()) {
      connection.send(message.reply(MessageType.ERROR, new Payloads.Ack(false, "error: log in first")));
      return;
    }
    handleAuthenticated(connection, message);
  }

  private void handleAuthenticated(ClientConnection connection, NetworkMessage message) {
    switch (message.type()) {
      case LOGOUT_REQUEST -> logout(connection, message);
      case MATCHMAKING_REQUEST -> queueUp(connection, message);
      case MATCHMAKING_CANCEL -> {
        matchmaking.cancel(connection.getUsername());
        connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "left the queue")));
      }
      case MATCH_INVITE -> invite(connection, message);
      case MATCH_INVITE_DECISION -> inviteDecision(connection, message);
      case GAME_ACTION -> gameAction(connection, message);
      case REACTION -> reaction(connection, message);
      case LEADERBOARD_REQUEST -> {
        Payloads.LeaderboardRequest request = message.payloadAs(Payloads.LeaderboardRequest.class);
        int limit = request == null ? 10 : request.limit();
        connection.send(message.reply(MessageType.LEADERBOARD_RESPONSE, leaderboard.top(limit)));
      }
      case PROFILE_UPDATE -> {
        Payloads.Profile stored = authentication.update(
            connection.getUsername(), message.payloadAs(Payloads.ProfileUpdate.class));
        if (stored == null) {
          connection.send(message.reply(MessageType.ERROR,
              new Payloads.Ack(false, "error: could not store the profile")));
        } else {
          connection.send(message.reply(MessageType.PROFILE_RESPONSE, stored));
        }
      }
      case SCORE_SUBMISSION -> {
        Payloads.ScoreSubmission submission = message.payloadAs(Payloads.ScoreSubmission.class);
        connection.send(message.reply(MessageType.SCORE_RESPONSE,
            leaderboard.submit(connection.getUsername(), submission.score())));
      }
      default -> connection.send(message.reply(MessageType.ERROR,
          new Payloads.Ack(false, "error: unsupported request " + message.type())));
    }
  }

  private void register(ClientConnection connection, NetworkMessage message) {
    Payloads.AuthResponse response =
        authentication.register(message.payloadAs(Payloads.RegisterRequest.class));
    connection.send(message.reply(MessageType.REGISTER_RESPONSE, response));
  }

  private void login(ClientConnection connection, NetworkMessage message) {
    Payloads.AuthResponse response =
        authentication.login(message.payloadAs(Payloads.LoginRequest.class));
    if (response.success() && !sessions.bind(response.profile().username(), connection)) {
      connection.send(message.reply(MessageType.LOGIN_RESPONSE,
          new Payloads.AuthResponse(false, "error: already logged in elsewhere", null)));
      return;
    }
    if (response.success()) {
      connection.setUsername(response.profile().username());
    }
    connection.send(message.reply(MessageType.LOGIN_RESPONSE, response));
  }

  private void logout(ClientConnection connection, NetworkMessage message) {
    matchmaking.cancel(connection.getUsername());
    sessions.unbind(connection);
    connection.setUsername(null);
    connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "logged out")));
  }

  private void queueUp(ClientConnection connection, NetworkMessage message) {
    if (matches.matchOf(connection.getUsername()) != null) {
      connection.send(message.reply(MessageType.ERROR,
          new Payloads.Ack(false, "error: you are already in a match")));
      return;
    }
    String opponent = matchmaking.enqueue(connection.getUsername());
    ClientConnection opponentConnection =
        opponent == null ? null : sessions.connectionOf(opponent);
    if (opponentConnection == null) {
      if (opponent != null) {
        // the waiting player went offline, so take their spot instead of dropping out
        matchmaking.enqueue(connection.getUsername());
      }
      connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "waiting for an opponent")));
      return;
    }
    connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "matched")));
    startMatch(opponentConnection, opponent, connection, connection.getUsername());
  }

  private void invite(ClientConnection connection, NetworkMessage message) {
    Payloads.MatchInviteRequest request = message.payloadAs(Payloads.MatchInviteRequest.class);
    String target = request == null ? null : request.targetUsername();
    if (target == null || target.equalsIgnoreCase(connection.getUsername())) {
      connection.send(message.reply(MessageType.ERROR, new Payloads.Ack(false, "error: invalid target")));
      return;
    }
    if (!sessions.isOnline(target)) {
      connection.send(message.reply(MessageType.ERROR,
          new Payloads.Ack(false, "error: " + target + " is not online")));
      return;
    }
    MatchmakingService.Invite invite = matchmaking.invite(connection.getUsername(), target);
    sessions.connectionOf(target).send(NetworkMessage.event(MessageType.MATCH_INVITE_EVENT,
        new Payloads.MatchInviteEvent(invite.id(), connection.getUsername())));
    connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "invite sent")));
  }

  private void inviteDecision(ClientConnection connection, NetworkMessage message) {
    Payloads.MatchInviteDecision decision = message.payloadAs(Payloads.MatchInviteDecision.class);
    // Consumed against the authenticated caller, not against the id alone: an id is a guessable
    // token and accepting on somebody else's behalf would put a stranger in their match.
    MatchmakingService.Invite invite = decision == null
        ? null
        : matchmaking.consumeInvite(decision.inviteId(), connection.getUsername());
    if (invite == null) {
      connection.send(message.reply(MessageType.ERROR,
          new Payloads.Ack(false, "error: that invite is not yours to answer")));
      return;
    }
    ClientConnection host = sessions.connectionOf(invite.from());
    if (!decision.accepted() || host == null) {
      if (host != null) {
        host.send(NetworkMessage.event(MessageType.ACK,
            new Payloads.Ack(false, invite.to() + " declined")));
      }
      connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "invite declined")));
      return;
    }
    connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "invite accepted")));
    startMatch(host, invite.from(), connection, invite.to());
  }

  /** Whoever was waiting or sent the invite takes plants, the other takes zombies. */
  private void startMatch(
      ClientConnection plantsConnection,
      String plantsPlayer,
      ClientConnection zombiesConnection,
      String zombiesPlayer) {
    NetworkMatch match = matches.create(plantsPlayer, zombiesPlayer);
    plantsConnection.send(NetworkMessage.event(MessageType.MATCH_FOUND,
        new Payloads.MatchFound(match.getId(), zombiesPlayer, MatchRole.PLANTS, match.getLevel())));
    zombiesConnection.send(NetworkMessage.event(MessageType.MATCH_FOUND,
        new Payloads.MatchFound(match.getId(), plantsPlayer, MatchRole.ZOMBIES, match.getLevel())));
    System.out.println("match " + match.getId() + ": " + plantsPlayer + " (plants) vs "
        + zombiesPlayer + " (zombies)");
    // The first board, so both screens have something to draw before the clock's first tick.
    broadcastState(match);
  }

  private void gameAction(ClientConnection connection, NetworkMessage message) {
    Payloads.GameAction action = message.payloadAs(Payloads.GameAction.class);
    NetworkMatch match = action == null ? null : matches.get(action.matchId());
    if (match == null) {
      connection.send(message.reply(MessageType.ERROR, new Payloads.Ack(false, "error: no such match")));
      return;
    }
    MatchRole role = match.roleOf(connection.getUsername());
    if (role == null) {
      connection.send(message.reply(MessageType.ERROR, new Payloads.Ack(false, "error: not your match")));
      return;
    }
    if (match.isFinished()) {
      connection.send(message.reply(MessageType.ERROR,
          new Payloads.Ack(false, "error: the match is over")));
      return;
    }

    String rejection = match.getState().apply(role, toAction(action));
    if (rejection != null) {
      connection.send(message.reply(MessageType.ERROR, new Payloads.Ack(false, rejection)));
      return;
    }
    connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "applied")));
    broadcastState(match);
  }

  private static IZombieAction toAction(Payloads.GameAction action) {
    return "place-plant".equalsIgnoreCase(action.action())
        ? IZombieAction.placePlant(action.argument(), action.row(), action.col())
        : IZombieAction.placeZombie(action.argument(), action.row(), action.col());
  }

  @Override
  public void onTick(NetworkMatch match) {
    broadcastState(match);
  }

  @Override
  public void onFinished(NetworkMatch match) {
    MatchRole winningRole = match.getState().winner();
    String winner = winningRole == null ? null : match.playerOf(winningRole);
    String loser = winner == null ? null : match.opponentOf(winner);
    // The final board first, so the losing side sees what did it before the verdict lands.
    broadcastState(match);
    Payloads.MatchEnded ended = new Payloads.MatchEnded(match.getId(), winner, loser, winningRole,
        reasonFor(match, winningRole));
    sendToBoth(match, NetworkMessage.event(MessageType.MATCH_ENDED, ended));
    System.out.println("match " + match.getId() + " ended: " + ended.reason());
  }

  private static String reasonFor(NetworkMatch match, MatchRole winningRole) {
    if (winningRole == null) {
      return "the match was abandoned";
    }
    if (winningRole == MatchRole.ZOMBIES) {
      return "every brain was eaten";
    }
    return match.getState().ticksRemaining() == 0
        ? "the plants held out for " + IZombieMatch.SURVIVAL_SECONDS + " seconds"
        : "the zombies ran out of sun and had nothing left on the lawn";
  }

  private void broadcastState(NetworkMatch match) {
    Payloads.MatchStateUpdate update =
        new Payloads.MatchStateUpdate(match.getId(), match.getState().snapshot());
    sendToBoth(match, NetworkMessage.event(MessageType.MATCH_STATE_UPDATE, update));
  }

  private void sendToBoth(NetworkMatch match, NetworkMessage message) {
    for (MatchRole role : MatchRole.values()) {
      ClientConnection target = sessions.connectionOf(match.playerOf(role));
      if (target != null) {
        target.send(message);
      }
    }
  }

  private void reaction(ClientConnection connection, NetworkMessage message) {
    Payloads.Reaction reaction = message.payloadAs(Payloads.Reaction.class);
    NetworkMatch match = reaction == null ? null : matches.get(reaction.matchId());
    if (match == null || match.roleOf(connection.getUsername()) == null) {
      connection.send(message.reply(MessageType.ERROR, new Payloads.Ack(false, "error: not your match")));
      return;
    }
    ClientConnection opponent = sessions.connectionOf(match.opponentOf(connection.getUsername()));
    if (opponent != null) {
      opponent.send(NetworkMessage.event(MessageType.REACTION_EVENT,
          new Payloads.ReactionEvent(connection.getUsername(), reaction.kind(), reaction.value())));
    }
    connection.send(message.reply(MessageType.ACK, new Payloads.Ack(true, "sent")));
  }

  public void onDisconnect(ClientConnection connection) {
    String username = connection.getUsername();
    if (username == null) {
      return;
    }
    matchmaking.cancel(username);
    NetworkMatch match = matches.matchOf(username);
    if (match != null) {
      String opponentName = match.opponentOf(username);
      ClientConnection opponent = sessions.connectionOf(opponentName);
      if (match.claimEnded() && opponent != null) {
        opponent.send(NetworkMessage.event(MessageType.MATCH_ENDED,
            new Payloads.MatchEnded(match.getId(), opponentName, username,
                match.roleOf(opponentName), "your opponent left the match")));
      }
      matches.end(match.getId());
    }
    sessions.unbind(connection);
  }
}
