package network.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;

/** Requests block until the reply with the same id arrives. Anything without an id is an event. */
public final class NetworkClient implements Closeable {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 7070;
  private static final long DEFAULT_TIMEOUT_MS = 5000;

  private final Map<String, CompletableFuture<NetworkMessage>> pending = new ConcurrentHashMap<>();
  private final List<Consumer<NetworkMessage>> listeners = new CopyOnWriteArrayList<>();

  private Socket socket;
  private BufferedReader in;
  private BufferedWriter out;
  private Thread reader;

  public void connect(String host, int port) throws IOException {
    socket = new Socket(host, port);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    out = new BufferedWriter(
        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    reader = new Thread(this::readLoop, "network-client-reader");
    reader.setDaemon(true);
    reader.start();
  }

  public boolean isConnected() {
    return socket != null && socket.isConnected() && !socket.isClosed();
  }

  public void onEvent(Consumer<NetworkMessage> listener) {
    listeners.add(listener);
  }

  public NetworkMessage request(MessageType type, Object payload) throws IOException {
    return request(type, payload, DEFAULT_TIMEOUT_MS);
  }

  public NetworkMessage request(MessageType type, Object payload, long timeoutMs)
      throws IOException {
    NetworkMessage message = NetworkMessage.request(type, payload);
    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    pending.put(message.id(), future);
    send(message);
    try {
      return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      pending.remove(message.id());
      throw new IOException("no reply to " + type + " within " + timeoutMs + "ms");
    } catch (Exception e) {
      pending.remove(message.id());
      throw new IOException("request failed: " + e.getMessage(), e);
    }
  }

  public synchronized void send(NetworkMessage message) throws IOException {
    if (!isConnected()) {
      throw new IOException("not connected");
    }
    out.write(message.encode());
    out.write('\n');
    out.flush();
  }

  private void readLoop() {
    try {
      String line;
      while ((line = in.readLine()) != null) {
        dispatch(NetworkMessage.decode(line));
      }
    } catch (IOException | RuntimeException e) {
      // connection dropped; the finally below wakes up anyone still waiting
    } finally {
      pending.values().forEach(future -> future.completeExceptionally(new IOException("disconnected")));
      pending.clear();
    }
  }

  private void dispatch(NetworkMessage message) {
    CompletableFuture<NetworkMessage> future =
        message.id() == null ? null : pending.remove(message.id());
    if (future != null) {
      future.complete(message);
      return;
    }
    listeners.forEach(listener -> listener.accept(message));
  }

  @Override
  public void close() {
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException ignored) {
      // already closed
    }
  }
}
