package data.persistence;

import data.repository.SaveGameRepository;

public class SaveManager {
    private final SaveGameRepository saveRepository;

    public SaveManager(SaveGameRepository saveRepository) {
        this.saveRepository = saveRepository;
    }

    public void saveCurrentGame(int saveId) {
    }


    public void loadGame(int saveId) {
    }
}
