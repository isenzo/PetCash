package org.isenzo.petPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.managers.PetManager;
import org.isenzo.petPlugin.models.Pet;
import org.isenzo.petPlugin.utils.SkullUtil;

import java.util.Arrays;
import java.util.List;

public class PetGUI {
    private static final String BASE64 = "d42e9dbd6eb544f6bb59cb2569e9113d750931e3ad01fd586eebd0f071e492d0";

    private final PetManager petManager;

    public PetGUI() {
        this.petManager = PetMiningPlugin.getInstance().getPetManager();
    }

    public void openPetMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Your Pets 🐾");
        List<Pet> pets = petManager.getPlayerPets(player);

        int slot = 10;
        for (Pet pet : pets) {
            if (slot >= 17) break;
            menu.setItem(slot++, createPetItem(pet));
        }

        player.openInventory(menu);
    }

    private ItemStack createPetItem(Pet pet) {
        ItemStack item = SkullUtil.getCustomHead(BASE64);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(formatName(pet));
        meta.setLore(createLore(pet));
        item.setItemMeta(meta);

        return item;
    }

    private String formatName(Pet pet) {
        return ChatColor.GOLD + pet.getName() + ChatColor.GRAY + "[LvL " + ChatColor.GREEN +  pet.getLevel() + ChatColor.GRAY +  "]";
    }

    private List<String> createLore(Pet pet) {
        return Arrays.asList(
                ChatColor.GRAY + "Type: " + ChatColor.GREEN + pet.getType(),
                ChatColor.GRAY + "XP: " + ChatColor.AQUA + pet.getExperience(),
                ChatColor.GRAY + "ID: " + ChatColor.YELLOW + pet.getId(),
                "",
                pet.isActive() ? ChatColor.RED + "▶ Click to despawn!" : ChatColor.GREEN + "▶ Click to summon!"
        );
    }
}