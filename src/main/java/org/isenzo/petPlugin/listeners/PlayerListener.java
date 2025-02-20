package org.isenzo.petPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.managers.PetManager;
import org.isenzo.petPlugin.managers.PetSummoner;
import org.isenzo.petPlugin.models.Pet;

import java.util.List;

public class PlayerListener implements Listener {

    private final PetManager petManager;
    private final PetSummoner petSummoner;

    public PlayerListener(PetManager petManager, PetSummoner petSummoner) {
        this.petManager = petManager;
        this.petSummoner = petSummoner;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
            List<Pet> activePets = petManager.getActivePetsFromDatabase(player);

            if (activePets.isEmpty()) return;

            for (Pet pet : activePets) {
                pet.setOwner(player);
                pet.spawn(player.getLocation().add(0, 1.5, 0));

                Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
                    if (pet.getEntity() != null) {
                        pet.getEntity().teleport(player.getLocation().add(0, 1.5, 0));
                    }
                }, 5L);

                petSummoner.startPetMovement(pet, player);
                petManager.addActivePet(player, pet.getId());

                player.sendMessage(ChatColor.GREEN + "🐾 Twój pet " + pet.getName() + " wrócił!");
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        List<Pet> activePets = petManager.getActivePetsFromDatabase(player);

        if (activePets.isEmpty()) return;

        for (Pet pet : activePets) {
            if (pet.getArmorStandId() != null) {
                pet.killArmorStand();

                Bukkit.getLogger().info("[DEBUG] Usunięto ArmorStand dla: " + pet.getName());
            } else {
                Bukkit.getLogger().info("[DEBUG] Brak ArmorStandID dla: " + pet.getName());
            }

            pet.setArmorStandId(null);
            petManager.updatePetInDatabase(pet);
        }
        petManager.clearPlayerCache(player);
    }

}
