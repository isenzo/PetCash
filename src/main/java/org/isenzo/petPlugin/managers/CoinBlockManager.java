package org.isenzo.petPlugin.managers;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.isenzo.petPlugin.models.CoinBlock;
import org.isenzo.petPlugin.utils.CoinItemUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoinBlockManager {
    private final Map<Location, CoinBlock> coinBlocks = new ConcurrentHashMap<>();

    public boolean isCoinBlock(Location location) {
        return coinBlocks.containsKey(location);
    }

    public void handleBlockDamage(Block block, Player player) {
        CoinBlock coinBlock = coinBlocks.get(block.getLocation());
        if (coinBlock == null) return;

        // Sprawdzamy, czy gracz trzyma specjalny miecz
        if (!CoinItemUtil.isCoinSword(player.getInventory().getItemInMainHand())) {
            player.sendMessage("§cPotrzebujesz specjalnego miecza, aby niszczyć ten coinblock!");
            return;
        }

        // Odejmuje np. 10 HP przy każdym \"uderzeniu\"
        coinBlock.damage(0.5, player);
    }

    public void addCoinBlock(Location location, double maxHealth) {
        coinBlocks.put(location, new CoinBlock(location, maxHealth));
    }

    public void removeCoinBlock(Location location) {
        coinBlocks.remove(location);
    }
}
