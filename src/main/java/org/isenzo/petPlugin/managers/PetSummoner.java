package org.isenzo.petPlugin.managers;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.gui.PetGUI;
import org.isenzo.petPlugin.models.Pet;

import java.util.*;

@Setter
public class PetSummoner {

    private PetManager petManager;
    private final Map<UUID, List<Pet>> activePets = new HashMap<>();

    public PetSummoner() {
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

    public void despawnPetOnCommand(Player player, Pet pet) {
        List<Pet> pets = activePets.get(player.getUniqueId());

        if (Objects.isNull(pets)) return;

        if (pets.contains(pet)) {
            pets.remove(pet);

            if (Objects.nonNull(pet.getEntity())) {
                pet.getEntity().remove();
                pet.setEntity(null);
            }

            pet.setActive(false);
            petManager.updatePetInDatabase(pet);

            if (pets.isEmpty()) {
                activePets.remove(player.getUniqueId());
            }

            Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
                new PetGUI().openPetMenu(player);
            }, 2L);

            player.sendMessage(ChatColor.YELLOW + "You have despawned " + pet.getName() + ".");
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
