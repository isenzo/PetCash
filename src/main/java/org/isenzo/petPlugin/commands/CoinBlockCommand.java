package org.isenzo.petPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.isenzo.petPlugin.PetMiningPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Komenda do stawiania \"CoinBlock\" (bariera z HP).
 * /coinblock <hp>
 */
public class CoinBlockCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracz może użyć tej komendy.");
            return true;
        }
        Player player = (Player) sender;

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

        // Szukamy wolnego miejsca (np. max 5 bloków do przodu)
        Block targetBlock = null;
        BlockIterator iterator = new BlockIterator(player, 5);
        while (iterator.hasNext()) {
            Block b = iterator.next();
            if (b.getType() == Material.AIR) {
                // Znaleziono pierwsze puste pole
                targetBlock = b;
                break;
            }
        }
        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "Brak wolnego miejsca w zasięgu!");
            return true;
        }

        // Zamieniamy wolny blok na barrier (nasz coin block)
        targetBlock.setType(Material.BARRIER);
        // Dodajemy do coinBlockManager
        PetMiningPlugin.getInstance().getCoinBlockManager()
                .addCoinBlock(targetBlock.getLocation(), hp);

        player.sendMessage(ChatColor.GREEN + "Postawiono CoinBlock z HP = " + hp);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("10");
                    suggestions.add("50");
                            suggestions.add("100");
        }
        return suggestions;
    }
}
