package com.bnjrKemal;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DECommand implements CommandExecutor {

    private final Main main;
    private String prefix;
    private String reload;
    private String noPermission;

    public DECommand(Main instance) {
        main = instance;
        prefix = main.getConfig().getString("prefix");
        noPermission = main.getConfig().getString("no-permission").replace("{prefix}", prefix);
        reload = main.getConfig().getString("reload").replace("{prefix}", prefix);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            if (!sender.isOp()) {
                sender.sendMessage(noPermission);
                return false;
            }
        }

        main.reloading();
        sender.sendMessage(reload);
        return true;
    }
}
