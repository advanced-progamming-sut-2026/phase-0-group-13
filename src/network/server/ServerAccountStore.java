package network.server;

import data.persistence.DataPath;
import data.persistence.JsonSerializer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ServerAccountStore {

  private static final String PATH_KEY = "server-accounts";
  private static final String FILE_NAME = "server-accounts.json";

  private final JsonSerializer serializer = new JsonSerializer();
  private final String filePath;
  private final List<ServerAccount> accounts;

  public ServerAccountStore() {
    this.filePath = resolvePath().toString();
    ServerAccount[] loaded = serializer.readFromFile(filePath, ServerAccount[].class);
    this.accounts = loaded == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(loaded));
  }

  private static Path resolvePath() {
    Path users = DataPath.getInstance().getPath("users");
    Path file = users != null
        ? users.resolveSibling(FILE_NAME)
        : Paths.get("data", "database", FILE_NAME);
    DataPath.getInstance().registerPath(PATH_KEY, file);
    return file;
  }

  public synchronized ServerAccount find(String username) {
    if (username == null) {
      return null;
    }
    for (ServerAccount account : accounts) {
      if (account.getUsername().equalsIgnoreCase(username)) {
        return account;
      }
    }
    return null;
  }

  public synchronized boolean add(ServerAccount account) {
    if (find(account.getUsername()) != null) {
      return false;
    }
    accounts.add(account);
    save();
    return true;
  }

  public synchronized List<ServerAccount> all() {
    return new ArrayList<>(accounts);
  }

  public synchronized void save() {
    serializer.writeToFile(filePath, accounts);
  }

  public int size() {
    return accounts.size();
  }

  public String getFilePath() {
    return filePath;
  }
}
