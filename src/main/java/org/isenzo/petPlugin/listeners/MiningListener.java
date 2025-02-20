package org.isenzo.petPlugin.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.managers.CoinBlockManager;

public class MiningListener implements Listener {

    private final CoinBlockManager coinBlockManager;

    public MiningListener() {
        this.coinBlockManager = PetMiningPlugin.getInstance().getCoinBlockManager();
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getBlock().getType() != Material.BARRIER) {
            return;
        }

        if (coinBlockManager.isCoinBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            coinBlockManager.handleBlockDamage(event.getBlock(), event.getPlayer());
        }
    }
}