package cn.nekopixel.bedwars.game;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum GameStatus {
    WAITING,
    INGAME,
    ENDING,
    RESETTING;

    public static List<String> getNames() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public static String getNamesAsString() {
        return String.join(", ", getNames());
    }
}
