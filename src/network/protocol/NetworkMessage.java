package network.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.UUID;

/** One line of JSON on the socket. The id is what pairs a reply with its request. */
public final class NetworkMessage {

  private static final Gson GSON = new Gson();

  private final MessageType type;
  private final String id;
  private final JsonElement payload;

  private NetworkMessage(MessageType type, String id, JsonElement payload) {
    this.type = type;
    this.id = id;
    this.payload = payload;
  }

  public static NetworkMessage request(MessageType type, Object payload) {
    return new NetworkMessage(type, UUID.randomUUID().toString(), GSON.toJsonTree(payload));
  }

  public static NetworkMessage event(MessageType type, Object payload) {
    return new NetworkMessage(type, null, GSON.toJsonTree(payload));
  }

  public NetworkMessage reply(MessageType replyType, Object replyPayload) {
    return new NetworkMessage(replyType, id, GSON.toJsonTree(replyPayload));
  }

  public MessageType type() {
    return type;
  }

  public String id() {
    return id;
  }

  public <T> T payloadAs(Class<T> payloadType) {
    return payload == null ? null : GSON.fromJson(payload, payloadType);
  }

  public String encode() {
    return GSON.toJson(this);
  }

  public static NetworkMessage decode(String line) {
    NetworkMessage message = GSON.fromJson(line, NetworkMessage.class);
    if (message == null || message.type == null) {
      throw new IllegalArgumentException("malformed message: " + line);
    }
    return message;
  }
}
