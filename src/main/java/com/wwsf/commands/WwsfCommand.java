package com.wwsf.commands;

import javax.annotation.Nonnull;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wwsf.WWSFPlugin;
import com.wwsf.gas.GasImmunityCommand;

/**
 * {@code /wwsf gas immunity}.
 *
 * <p>The Gun Forge and Ammo Press recipe editors were removed on 2026-07-31, so
 * only the gas-immunity registry remains.</p>
 */
public class WwsfCommand implements CommandExecutor {

    private final GasImmunityCommand gasImmunityCommand;

    public WwsfCommand(@Nonnull WWSFPlugin plugin) {
        this.gasImmunityCommand = new GasImmunityCommand(plugin);
    }

    @Nonnull
    public GasImmunityCommand getGasImmunityCommand() {
        return gasImmunityCommand;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command,
                            @Nonnull String label, @Nonnull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§cYou must be opped to use this command.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("gas") && args[1].equalsIgnoreCase("immunity")) {
            gasImmunityCommand.open(player);
            return true;
        }

        player.sendMessage("§eWWSF Commands:");
        player.sendMessage("§7  /wwsf gas immunity  §8— Register items that grant gas immunity");
        return true;
    }
}
