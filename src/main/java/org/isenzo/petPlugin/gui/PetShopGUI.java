package org.isenzo.petPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.managers.PetManager;

import java.util.ArrayList;
import java.util.List;

public class PetShopGUI {
    private Player player;
    private Inventory inventory;
    private PetManager petManager;

    public PetShopGUI(Player player) {
        this.player = player;
        this.petManager = PetMiningPlugin.getInstance().getPetManager();
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(null, 27, "§6Pet Shop");
        updateInventory();
    }

    private void updateInventory() {
        inventory.clear();

        // Add available pet types
        addPetOption("Silver", Material.IRON_INGOT, 1000, 0);
        addPetOption("Radiation", Material.EMERALD, 2000, 2);

        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName("§cBack to Pet Menu");
        backButton.setItemMeta(meta);
        inventory.setItem(26, backButton);
    }

    private void addPetOption(String type, Material material, int cost, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§b" + type + " Pet");

        List<String> lore = new ArrayList<>();
        lore.add("§7Cost: §6" + cost + " coins");
        lore.add("");
        lore.add("§7Features:");

        switch (type.toLowerCase()) {
            case "silver":
                lore.add("§7• 50% bonus damage to iron blocks");
                lore.add("§7• Generates silver particles");
                break;
            case "radiation":
                lore.add("§7• Immune to radiation damage");
                lore.add("§7• Can mine in toxic zones");
                break;
        }

        lore.add("");
        lore.add("§eClick to purchase!");

        meta.setLore(lore);
        item.setItemMeta(meta);

        inventory.setItem(slot, item);
    }

    public void open() {
        player.openInventory(inventory);
    }
}