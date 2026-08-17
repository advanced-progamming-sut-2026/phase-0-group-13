package network.server;

import model.enums.MatchRole;
import model.game.minigame.arcade.IZombieAction;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.Payloads;

public final class RequestRouter {

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
    MatchmakingService.Invite invite =
        decision == null ? null : matchmaking.consumeInvite(decision.inviteId());
    if (invite == null) {
      connection.send(message.reply(MessageType.ERROR,
          new Payloads.Ack(false, "error: unknown invite")));
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
        new Payloads.MatchFound(match.getId(), zombiesPlayer, MatchRole.PLANTS)));
    zombiesConnection.send(NetworkMessage.event(MessageType.MATCH_FOUND,
        new Payloads.MatchFound(match.getId(), plantsPlayer, MatchRole.ZOMBIES)));
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

  private void broadcastState(NetworkMatch match) {
    Payloads.MatchStateUpdate update = new Payloads.MatchStateUpdate(
        match.getId(), "brains=" + match.getState().snapshot().brainsRemaining());
    for (MatchRole role : MatchRole.values()) {
      ClientConnection target = sessions.connectionOf(match.playerOf(role));
      if (target != null) {
        target.send(NetworkMessage.event(MessageType.MATCH_STATE_UPDATE, update));
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
      ClientConnection opponent = sessions.connectionOf(match.opponentOf(username));
      if (opponent != null) {
        opponent.send(NetworkMessage.event(MessageType.MATCH_ENDED,
            new Payloads.MatchEnded(match.getId(), match.opponentOf(username), "opponent left")));
      }
      matches.end(match.getId());
    }
    sessions.unbind(connection);
  }
}
