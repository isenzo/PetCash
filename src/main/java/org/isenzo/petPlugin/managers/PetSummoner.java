package org.isenzo.petPlugin.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.models.Pet;

import java.util.*;

public class PetSummoner {

    private PetManager petManager;
    private final Map<UUID, List<Pet>> activePets = new HashMap<>();

    public PetSummoner() { }

    public void setPetManager(PetManager petManager) {
        this.petManager = petManager;
    }

    public void summonPet(Player player, Pet pet) {
        List<Pet> pets = activePets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

        if (pets.contains(pet)) {
            player.sendMessage(ChatColor.YELLOW + "Your pet " + pet.getName() + " is already summoned!");
            return;
        }

        if (pets.size() >= 5) {
            player.sendMessage(ChatColor.RED + "Masz już maksymalną liczbę przywołanych zwierzaków!");
            return;
        }

        // 🔥 Ustawiamy właściciela peta
        pet.setOwner(player);
        Bukkit.getLogger().info("[DEBUG] summonPet() - Owner set to " + player.getName());

        Location spawnLocation = player.getLocation().add(0, 1.5, 0);
        pet.spawn(spawnLocation);
        pets.add(pet);

        pet.setActive(true);
        petManager.updatePetInDatabase(pet);

        Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
            if (pet.getEntity() != null) {
                pet.getEntity().teleport(player.getLocation().add(0, 1.5, 0));
            }
        }, 1L);

        startPetMovement(pet, player);
    }


    public void despawnPet(Player player, Pet pet) {
        List<Pet> pets = activePets.get(player.getUniqueId());

        if (pets == null) return;

        if (pets.contains(pet)) {
            pets.remove(pet);
            pet.despawn();

            pet.setActive(false);
            petManager.updatePetInDatabase(pet);

            if (pets.isEmpty()) {
                activePets.remove(player.getUniqueId());
            }
        }
    }

    public void startPetMovement(Pet pet, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !pet.isActive() || pet.getEntity() == null) {
                    cancel();
                    return;
                }

                pet.moveToPlayerSmoothly();
            }
        }.runTaskTimer(PetMiningPlugin.getInstance(), 0L, 1L);
    }

    public void setPetName(Pet pet) {
        String formattedName = ChatColor.GOLD + pet.getName() + ChatColor.YELLOW + " [LvL " + pet.getLevel() + "]";
        pet.getEntity().setCustomName(formattedName);
        pet.getEntity().setCustomNameVisible(true);
    }
}
