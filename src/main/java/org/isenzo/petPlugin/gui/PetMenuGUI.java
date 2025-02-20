package org.isenzo.petPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.managers.PetManager;
import org.isenzo.petPlugin.models.Pet;

import java.util.ArrayList;
import java.util.List;

public class PetMenuGUI {
    private Player player;
    private Inventory inventory;
    private PetManager petManager;

    public PetMenuGUI(Player player) {
        this.player = player;
        this.petManager = PetMiningPlugin.getInstance().getPetManager();
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(null, 54, "§6Pet Management");
        updateInventory();
    }

    private void updateInventory() {
        inventory.clear();

        List<Pet> pets = petManager.getPlayerPets(player);
        int slot = 0;

        for (Pet pet : pets) {
            ItemStack petItem = createPetItem(pet);
            inventory.setItem(slot++, petItem);
        }

        if (pets.size() < petManager.getMaxOwnedPets()) {
            ItemStack newPetItem = new ItemStack(Material.EMERALD);
            ItemMeta meta = newPetItem.getItemMeta();
            meta.setDisplayName("§aGet New Pet");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to view available pets!");
            meta.setLore(lore);
            newPetItem.setItemMeta(meta);
            inventory.setItem(53, newPetItem);
        }
    }

    private ItemStack createPetItem(Pet pet) {
        ItemStack item = new ItemStack(getPetMaterial(pet.getType()));
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§b" + pet.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§7Type: " + pet.getType());
        lore.add("§7Level: " + pet.getLevel());
        lore.add("§7XP: " + String.format("%.2f", pet.getExperience()));
        lore.add("");

        if (pet.isActive()) {
            lore.add("§aCurrently Active");
            lore.add("§eClick to despawn");
        } else {
            lore.add("§cCurrently Inactive");
            lore.add("§eClick to summon");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private Material getPetMaterial(String type) {
        switch (type.toLowerCase()) {
            case "silver":
                return Material.IRON_INGOT;
            case "radiation":
                return Material.EMERALD;
            default:
                return Material.BONE;
        }
    }

    public void open() {
        player.openInventory(inventory);
    }
}