package data.persistence;

import data.repository.SaveGameRepository;

public class SaveManager {
    private final SaveGameRepository saveRepository;
    private final JsonSerializer jsonSerializer;

    public SaveManager(SaveGameRepository saveRepository, JsonSerializer jsonSerializer) {
        this.saveRepository = saveRepository;
        this.jsonSerializer = jsonSerializer;
    }

    public void saveCurrentGame(int saveId) {

    }


    public void loadGame(int saveId) {

    }
}
