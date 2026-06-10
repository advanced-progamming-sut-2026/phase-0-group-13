package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PlantSelectionMenuCommands {
    ShowAllPlants("show\\s+all\\s+plants"),
    ShowAvailablePlants("show\\s+available\\s+plants"),
    AddPlant("add\\s+plant\\s+-t\\s+(?<type>.+)"),
    RemovePlant("remove\\s+plant\\s+-t\\s+(?<type>.+)"),
    BoostPlant("boost\\s+plant\\s+-t\\s+(?<type>.+)"),
    StartGame("start\\s+game");

    private final String pattern;

    PlantSelectionMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
