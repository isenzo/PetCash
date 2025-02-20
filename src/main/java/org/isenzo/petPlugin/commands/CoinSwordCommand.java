package org.isenzo.petPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.isenzo.petPlugin.utils.CoinItemUtil;

public class CoinSwordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracz może wykonać tę komendę!");
            return true;
        }
        Player player = (Player) sender;

        player.getInventory().addItem(CoinItemUtil.createCoinSword());
        player.sendMessage(ChatColor.GREEN + "Otrzymałeś specjalny miecz do niszczenia coinblocków!");
        return true;
    }
}
