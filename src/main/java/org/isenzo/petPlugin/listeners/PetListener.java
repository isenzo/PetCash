package org.isenzo.petPlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.managers.PetManager;

public class PetListener implements Listener {
    private PetManager petManager;

    public PetListener() {
        this.petManager = PetMiningPlugin.getInstance().getPetManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        petManager.getPlayerPets(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        petManager.saveAndDespawnPets(event.getPlayer());
    }
}

