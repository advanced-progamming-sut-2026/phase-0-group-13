package network.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.Payloads;

public final class ClientConnection implements Runnable {

  private final Socket socket;
  private final RequestRouter router;
  private final BufferedReader in;
  private final BufferedWriter out;
  private volatile String username;

  public ClientConnection(Socket socket, RequestRouter router) throws IOException {
    this.socket = socket;
    this.router = router;
    this.in = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    this.out = new BufferedWriter(
        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public boolean isAuthenticated() {
    return username != null;
  }

  public synchronized void send(NetworkMessage message) {
    try {
      out.write(message.encode());
      out.write('\n');
      out.flush();
    } catch (IOException e) {
      close();
    }
  }

  @Override
  public void run() {
    try {
      String line;
      while ((line = in.readLine()) != null) {
        handle(line);
      }
    } catch (IOException e) {
      // client went away; fall through to cleanup
    } finally {
      router.onDisconnect(this);
      close();
    }
  }

  private void handle(String line) {
    NetworkMessage message;
    try {
      message = NetworkMessage.decode(line);
    } catch (RuntimeException e) {
      send(NetworkMessage.event(MessageType.ERROR, new Payloads.Ack(false, "malformed message")));
      return;
    }
    try {
      router.handle(this, message);
    } catch (RuntimeException e) {
      send(message.reply(MessageType.ERROR,
          new Payloads.Ack(false, "server error: " + e.getMessage())));
    }
  }

  public void close() {
    try {
      socket.close();
    } catch (IOException ignored) {
      // already closed
    }
  }

  public String remoteAddress() {
    return socket.getRemoteSocketAddress().toString();
  }
}
