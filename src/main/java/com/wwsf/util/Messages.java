package com.wwsf.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.wwsf.WWSFPlugin;

public final class Messages {

    private Messages() {
    }

    public static String get(String path, String def) {
        FileConfiguration cfg = WWSFPlugin.getInstance().getConfig();
        return color(cfg.getString(path, def));
    }

    public static String get(String path, String def, String... replacements) {
        String msg = get(path, def);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public static void send(CommandSender sender, String path, String def) {
        sender.sendMessage(get(path, def));
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
