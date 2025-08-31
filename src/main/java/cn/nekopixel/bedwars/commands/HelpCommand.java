package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.language.LanguageManager;
import org.bukkit.command.CommandSender;

public class HelpCommand {
    public static void sendMainHelp(CommandSender sender) {
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.title"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.version"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.switch"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.reload"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.save"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.setmode"));
        sender.sendMessage("");
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.map_title"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.setjoin"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.setrespawning"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.setbed"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.removebed"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.listbeds"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.setspawn"));
        sender.sendMessage("");
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.npc_title"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.addnpc"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.listnpcs"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.removenpc"));
        sender.sendMessage("");
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.spawner_title"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.setspawner"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.listspawners"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.removespawner"));
        sender.sendMessage("");
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.protect_title"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.pos1"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.pos2"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.addprotect"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.removeprotect"));
        sender.sendMessage(LanguageManager.getInstance().getMessage("help.listprotect"));
    }
}