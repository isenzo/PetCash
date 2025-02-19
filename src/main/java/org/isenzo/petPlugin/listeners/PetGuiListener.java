package org.isenzo.petPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.isenzo.petPlugin.managers.PetManager;
import org.isenzo.petPlugin.managers.PetSummoner;
import org.isenzo.petPlugin.models.Pet;

import java.util.List;
import java.util.Optional;

public class PetGuiListener implements Listener {

    private final PetManager petManager;
    private final PetSummoner petSummoner;

    public PetGuiListener(PetManager petManager, PetSummoner petSummoner) {
        this.petManager = petManager;
        this.petSummoner = petSummoner;
    }

    @EventHandler
    public void onPetGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equalsIgnoreCase(ChatColor.DARK_GREEN + "Your Pets 🐾")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta()) return;
            ItemMeta meta = clickedItem.getItemMeta();

            if (meta == null || meta.getLore() == null) return;

            String petId = getPetIdFromLore(meta.getLore());
            if (petId == null) {
                player.sendMessage(ChatColor.RED + "Error: Pet ID not found!");
                return;
            }

            Optional<Pet> petOpt = petManager.getPlayerPets(player).stream()
                    .filter(pet -> pet.getId().equals(petId))
                    .findFirst();

            if (!petOpt.isPresent()) {
                player.sendMessage(ChatColor.RED + "Error: Pet not found!");
                return;
            }

            Pet pet = petOpt.get();

            if (pet.isActive()) {
                petManager.despawnPet(player, pet.getId());
                petSummoner.despawnPet(player, pet);
                player.sendMessage(ChatColor.RED + "Your pet has been despawned!");
            } else {
                petManager.spawnPet(player, pet.getId());
                petSummoner.summonPet(player, pet);
                player.sendMessage(ChatColor.GREEN + "Your pet has been summoned!");
            }

            player.closeInventory();
        }
    }

    private String getPetIdFromLore(List<String> lore) {
        for (String line : lore) {
            if (line.contains("ID:")) {
                return ChatColor.stripColor(line).replace("ID: ", "").trim();
            }
        }
        return null;
    }
}
