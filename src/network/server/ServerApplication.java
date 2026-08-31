package network.server;

import data.GameDataManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class ServerApplication {

  public static final int DEFAULT_PORT = 7070;

  private final int port;
  private final ServerAccountStore accounts = new ServerAccountStore();
  private final SessionManager sessions = new SessionManager();
  private final MatchmakingService matchmaking = new MatchmakingService();
  private final MatchService matches = new MatchService();
  private final RequestRouter router;
  private volatile boolean running;
  private ServerSocket serverSocket;

  public ServerApplication(int port) {
    this.port = port;
    this.router = new RequestRouter(
        sessions,
        new AuthenticationService(accounts),
        matchmaking,
        matches,
        new LeaderboardService(accounts));
    matches.setListener(router);
  }

  public void start() throws IOException {
    // The match engine prices plants out of plants.json, so the server needs the game data even
    // though it never opens a lawn of its own.
    new GameDataManager();
    serverSocket = new ServerSocket(port);
    running = true;
    matches.start();
    System.out.println("PvZ server listening on port " + port);
    System.out.println("accounts: " + accounts.size() + " (" + accounts.getFilePath() + ")");

    while (running) {
      try {
        Socket socket = serverSocket.accept();
        ClientConnection connection = new ClientConnection(socket, router);
        Thread thread = new Thread(connection, "client-" + connection.remoteAddress());
        thread.setDaemon(true);
        thread.start();
        System.out.println("client connected: " + connection.remoteAddress());
      } catch (IOException e) {
        if (running) {
          System.out.println("accept failed: " + e.getMessage());
        }
      }
    }
  }

  public void stop() {
    running = false;
    matches.shutdown();
    try {
      if (serverSocket != null) {
        serverSocket.close();
      }
    } catch (IOException ignored) {
    }
  }

  public static void main(String[] args) throws IOException {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
    ServerApplication server = new ServerApplication(port);
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "server-shutdown"));
    server.start();
  }
}
