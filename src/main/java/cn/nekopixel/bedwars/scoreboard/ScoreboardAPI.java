package cn.nekopixel.bedwars.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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
        } else {
            int splitIndex = 16;
            
            while (splitIndex > 0 && splitIndex < text.length()) {
                if (text.charAt(splitIndex - 1) == '&' || text.charAt(splitIndex - 1) == '§') {
                    splitIndex--;
                } else if (splitIndex >= 2 && 
                          (text.charAt(splitIndex - 2) == '&' || text.charAt(splitIndex - 2) == '§')) {
                    break;
                } else {
                    break;
                }
            }
            
            String prefix = text.substring(0, splitIndex);
            String suffix = text.substring(splitIndex);
            
            String lastColors = ChatColor.getLastColors(prefix);
            team.setPrefix(prefix);
            
            if (suffix.length() > 16) {
                suffix = suffix.substring(0, 16);
            }
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