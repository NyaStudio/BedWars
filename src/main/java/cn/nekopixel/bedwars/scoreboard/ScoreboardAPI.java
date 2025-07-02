package cn.nekopixel.bedwars.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardAPI {
    private static final String OBJECTIVE_NAME = "bedwars";
    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, String> lines = new HashMap<>();
    private final Map<Integer, Team> teams = new HashMap<>();
    
    public ScoreboardAPI(Player player) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        player.setScoreboard(scoreboard);
    }
    
    public void setTitle(String title) {
        objective.setDisplayName(title);
    }
    
    public void setLine(int line, String text) {
        if (line < 0 || line > 15) return;
        
        if (lines.containsKey(line)) {
            removeLine(line);
        }
        
        if (text == null || text.isEmpty()) {
            text = getEmptyLine(line);
        }
        
        if (text.length() > 40) {
            text = text.substring(0, 40);
        }
        
        Team team = scoreboard.registerNewTeam("line" + line);
        String entry = getEntry(line);
        team.addEntry(entry);
        
        if (text.length() <= 16) {
            team.setPrefix(text);
        } else if (text.length() <= 32) {
            String prefix = text.substring(0, 16);
            String suffix = text.substring(16);
            String lastColors = ChatColor.getLastColors(prefix);
            team.setPrefix(prefix);
            team.setSuffix(lastColors + suffix);
        } else {
            String prefix = text.substring(0, 16);
            String suffix = text.substring(16, Math.min(text.length(), 32));
            String lastColors = ChatColor.getLastColors(prefix);
            team.setPrefix(prefix);
            team.setSuffix(lastColors + suffix);
        }
        
        objective.getScore(entry).setScore(15 - line);
        
        lines.put(line, text);
        teams.put(line, team);
    }
    
    public void removeLine(int line) {
        if (!lines.containsKey(line)) return;
        
        String entry = getEntry(line);
        scoreboard.resetScores(entry);
        
        Team team = teams.remove(line);
        if (team != null) {
            team.unregister();
        }
        
        lines.remove(line);
    }
    
    public void clearLines() {
        for (int i = 0; i < 16; i++) {
            removeLine(i);
        }
    }
    
    public void delete() {
        clearLines();
        objective.unregister();
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    
    private String getEntry(int line) {
        return ChatColor.values()[line].toString() + ChatColor.RESET;
    }
    
    private String getEmptyLine(int line) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= line; i++) {
            sb.append(ChatColor.RESET);
        }
        return sb.toString();
    }
}