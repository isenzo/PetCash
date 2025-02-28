package org.isenzo.petPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class CoinRemoverCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracz może użyć tej komendy.");
            return true;
        }

        Player player = (Player) sender;

        Bukkit.getLogger().info("[DEBUG] Otrzymano komendę: " + command.getName() + " od gracza: " + player.getName());

        if (!player.hasPermission("coinblock.admin")) {
            player.sendMessage(ChatColor.RED + "Nie masz uprawnień do tej komendy!");
            return true;
        }

        ItemStack remover = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = remover.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "CoinBlock Remover");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Kliknij Shift + PPM, aby usunąć CoinBlock."));
            remover.setItemMeta(meta);
        }

        player.getInventory().addItem(remover);
        player.sendMessage(ChatColor.GREEN + "✔ Otrzymałeś CoinBlock Remover!");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);

        Bukkit.getLogger().info("[DEBUG] Gracz " + player.getName() + " otrzymał CoinBlock Remover.");
        return true;
    }
}
