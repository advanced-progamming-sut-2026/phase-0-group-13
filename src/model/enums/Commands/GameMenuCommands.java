package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GameMenuCommands implements Command {
    EnterChapter("menu\\s+enter\\s+chapter\\s+-c\\s+(?<chapterName>.+)"),
    GreenHouse("menu\\s+greenhouse"),
    TravelLog("menu\\s+travel-log"),
    LeaderBoard("menu\\s+leaderboard"),
    CoinWallet("menu\\s+coin-wallet"),
    GemWallet("menu\\s+gem-wallet"),
    CheatAdd("menu\\s+cheat\\s+add\\s+(?<count>\\S+)\\s+(?<currency>coin|diamond)");

    private final String pattern;

    GameMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
