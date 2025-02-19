package org.isenzo.petPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
            List<Pet> pets = petManager.getPlayerPets(player);
            if (pets == null || pets.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Nie znaleziono żadnych zwierzaków.");
                return;
            }

            for (Pet pet : pets) {
                if (pet.isActive()) {
                    Bukkit.getLogger().info("Przywracam peta: " + pet.getName());

                    // 🔥 Ustawiamy właściciela na nowo
                    pet.setOwner(player);

                    Location spawnLocation = player.getLocation().add(0, 1.5, 0);
                    pet.spawn(spawnLocation);

                    Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
                        if (pet.getEntity() != null) {
                            pet.getEntity().teleport(player.getLocation().add(0, 1.5, 0));
                        }
                    }, 1L);

                    petSummoner.summonPet(player, pet);

                    // 🔥 Uruchamiamy ruch peta
                    petSummoner.startPetMovement(pet, player);

                    petManager.addActivePet(player, pet.getId());

                    player.sendMessage(ChatColor.GREEN + "Your pet " + pet.getName() + " has returned!");
                }
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
            List<Pet> pets = petManager.getPlayerPets(player);

            if (pets == null || pets.isEmpty()) return;

            for (Pet pet : pets) {
                if (pet.isActive()) {
                    petSummoner.despawnPet(player, pet);
                    petManager.removeActivePet(player, pet.getId());

                    if (pet.getEntity() != null) {
                        pet.getEntity().remove();
                    }
                }
            }
        }, 10L);
    }
}
