package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GreenHouseMenuCommands implements Command {
    ShowGreenHouse("show\\s+greenhouse"),
    PlantPot("plant\\s+pot\\s+at\\s+\\((?<x>\\S+),\\s*(?<y>\\S+)\\)"),
    Collect("collect\\s+\\((?<x>\\S+),\\s*(?<y>\\S+)\\)"),
    Grow("grow\\s+\\((?<x>\\S+),\\s*(?<y>\\S+)\\)"),
    EnterShop("enter\\s+shop");

    private final String pattern;

    GreenHouseMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
