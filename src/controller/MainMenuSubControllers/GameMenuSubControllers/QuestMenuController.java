package controller.MainMenuSubControllers.GameMenuSubControllers;

import controller.BaseController;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.List;
import java.util.regex.Matcher;
import model.Result;
import model.account.Progress;
import model.account.User;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.QuestMenuCommands;
import model.enums.Menu;
import model.game.quest.Quest;

public class QuestMenuController implements BaseController {

    private static final List<String> AVAILABLE_MINI_GAMES = List.of("zombotany", "vasebreaker");

    @Override
    public void initController() {
        ensureQuestsInitialized();
    }

    @Override
    public void handleinput(String command) {
        ensureQuestsInitialized();
        Matcher matcher;

        if (QuestMenuCommands.ShowQuests.getMatcher(command) != null) {
            handleShowQuests(null);
        } else if ((matcher = QuestMenuCommands.TravelLogPage.getMatcher(command)) != null) {
            handleShowQuests(matcher.group("pageName").trim());
        } else if ((matcher = QuestMenuCommands.ClaimQuest.getMatcher(command)) != null) {
            handleClaimQuest(matcher.group("title").trim());
        } else if (QuestMenuCommands.ShowMiniGames.getMatcher(command) != null) {
            handleShowMiniGames();
        } else if ((matcher = QuestMenuCommands.PlayMiniGame.getMatcher(command)) != null) {
            handlePlayMiniGame(matcher.group("name").trim());
        } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
            System.out.println("Travel Log");
        } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
            exit();
        } else {
            System.out.println("invalid input");
        }
    }

    private User requireUser() {
        User user = UserManager.getInstance().getCurrentUser();
        if (user == null) {
            System.out.println("error: no user logged in");
        }
        return user;
    }

    private void ensureQuestsInitialized() {
        User user = UserManager.getInstance().getCurrentUser();
        if (user == null || !user.getQuests().isEmpty()) {
            return;
        }

        List<Quest> templates =
                GameDataManager.questRepository != null ? GameDataManager.questRepository.getAll() : null;
        if (templates == null) {
            return;
        }

        for (Quest template : templates) {
            user.getQuests().add(new Quest(template));
        }
    }

    private void handleShowQuests(String pageName) {
        User user = requireUser();
        if (user == null) return;

        List<Quest> quests = user.getQuests();
        if (quests.isEmpty()) {
            System.out.println("No quests are available yet.");
            return;
        }

        boolean filtering = pageName != null && !pageName.isBlank() && !pageName.equalsIgnoreCase("all");

        System.out.println(filtering ? "--- Travel Log: " + pageName + " ---" : "--- Travel Log ---");
        boolean printedAny = false;
        for (Quest quest : quests) {
            if (filtering && (quest.getCategory() == null ||
                    !quest.getCategory().toLowerCase().contains(pageName.toLowerCase()))) {
                continue;
            }
            printedAny = true;
            printQuest(quest);
        }

        if (!printedAny) {
            System.out.println("No quests found for \"" + pageName + "\".");
        }
    }

    private void printQuest(Quest quest) {
        String status;
        if (quest.isCompleted() && quest.isRewardClaimed()) {
            status = "claimed";
        } else if (quest.isCompleted()) {
            status = "completed - reward unclaimed";
        } else {
            status = "in progress (" + quest.getProgressOfQuest() + ")";
        }

        System.out.println(
                "  "
                        + quest.getTitle()
                        + " ["
                        + (quest.getCategory() != null ? quest.getCategory() : "general")
                        + "] - "
                        + quest.getCondition()
                        + " - "
                        + status);
    }

    private void handleClaimQuest(String title) {
        User user = requireUser();
        if (user == null) return;

        Quest quest = findQuest(user, title);
        if (quest == null) {
            System.out.println("error: unknown quest: " + title);
            return;
        }
        if (!quest.isCompleted()) {
            System.out.println("error: that quest isn't completed yet");
            return;
        }
        if (quest.isRewardClaimed()) {
            System.out.println("error: you already claimed that reward");
            return;
        }

        quest.claimReward(user);
        System.out.println("Reward claimed for \"" + quest.getTitle() + "\"!");
        saveState();
    }

    private Quest findQuest(User user, String title) {
        for (Quest quest : user.getQuests()) {
            if (quest.getTitle() != null && quest.getTitle().equalsIgnoreCase(title)) {
                return quest;
            }
        }
        return null;
    }

    private void handleShowMiniGames() {
        User user = requireUser();
        if (user == null) return;

        Progress progress = user.getProgress();
        if (!progress.isMiniGamesUnlocked()) {
            System.out.println("Mini-Games mode is still locked. Keep clearing stages to unlock it!");
            return;
        }

        System.out.println("--- Mini-Games ---");
        for (String name : AVAILABLE_MINI_GAMES) {
            boolean unlocked = progress.isMiniGameUnlocked(name);
            System.out.println("  " + name + " - " + (unlocked ? "unlocked" : "locked"));
        }
    }

    private void handlePlayMiniGame(String name) {
        User user = requireUser();
        if (user == null) return;

        Progress progress = user.getProgress();
        String key = name.toLowerCase().trim();

        if (!AVAILABLE_MINI_GAMES.contains(key)) {
            System.out.println("error: unknown mini-game: " + name);
            return;
        }
        if (!progress.isMiniGameUnlocked(key)) {
            Result unlockResult = progress.unlockMiniGame(key);
            if (!unlockResult.success()) {
                System.out.println(unlockResult.message());
                return;
            }
            saveState();
        }

        // Handing off to the gameplay engine, which owns the actual mini-game modes.
        System.out.println("Launching mini-game: " + key + "...");
    }

    private void saveState() {
        try {
            UserManager.getInstance().updateCurrentUserGameState();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void exit() {
        System.out.println("Changed to Game Menu.");
        App.setCurrentMenu(Menu.GameMenu);
    }
}