package org.isenzo.petPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CoinBlockCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracz może użyć tej komendy.");
            return true;
        }
        Player player = (Player) sender;

        // ✅ Obsługa komendy "/coinblock"
        if (command.getName().equalsIgnoreCase("coinblock")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.YELLOW + "Użycie: /coinblock <hp>");
                return true;
            }

            double hp;
            try {
                hp = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Podaj liczbę jako HP!");
                return true;
            }

            // ✅ Tworzymy przedmiot CoinBlock do ręki
            ItemStack coinBlockItem = new ItemStack(Material.GOLD_BLOCK, 1);
            ItemMeta meta = coinBlockItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "CoinBlock " + ChatColor.GREEN + hp);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Blok do tworzenia coinblockow");
                lore.add(ChatColor.YELLOW + "HP: " + hp);
                meta.setLore(lore);
                coinBlockItem.setItemMeta(meta);
            }

            // ✅ Dajemy blok graczowi
            player.getInventory().addItem(coinBlockItem);
            player.sendMessage(ChatColor.GREEN + "✅ Otrzymałeś CoinBlock z HP = " + hp);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("50");
            suggestions.add("150");
            suggestions.add("500");
            suggestions.add("1500");
        }
        return suggestions;
    }
}
