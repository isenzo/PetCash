package org.isenzo.petPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
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
    private Inventory menu; // Przechowujemy GUI, aby aktualizować zamiast zamykać

    public PetGUI() {
        this.petManager = PetMiningPlugin.getInstance().getPetManager();
    }

    public void openPetMenu(Player player) {
        menu = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Twoje Pety 🐾");
        updateInventory(player); // 🔥 Przekazujemy gracza do funkcji aktualizującej
        player.openInventory(menu);
    }

    public void updateInventory(Player player) {
        if (menu == null) {
            return;
        }

        menu.clear(); // Usuwamy obecne elementy

        List<Pet> pets = petManager.getPlayerPets(player); // 🔥 Pobieramy pety dynamicznie dla danego gracza

        int activeSlot = 3;
        int inactiveSlot = 20;

        for (Pet pet : pets) {
            boolean isPetActive = petManager.isPetActiveInDatabase(pet.getId());
            ItemStack petItem = createPetItem(pet, isPetActive);

            if (isPetActive) {
                menu.setItem(activeSlot++, petItem);
            } else {
                menu.setItem(inactiveSlot++, petItem);
            }
        }
    }

    private ItemStack createPetItem(Pet pet, boolean isActive) {
        ItemStack item = SkullUtil.getCustomHead(BASE64);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(formatName(pet));
        meta.setLore(createLore(pet, isActive));

        if (isActive) {
            meta.addEnchant(Enchantment.BLAST_PROTECTION, 1, true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String formatName(Pet pet) {
        return "§6" + pet.getName() + "§8[§2LvL §a" + pet.getLevel() + "§8]";
    }

    private List<String> createLore(Pet pet, boolean isActive) {
        return Arrays.asList(
                "§7Typ: " + ChatColor.GREEN + pet.getType(),
                "§c⚔ §7Siła: " + ChatColor.AQUA + pet.getPower(),
                "§7ID: " + ChatColor.YELLOW + pet.getId(),
                "",
                isActive ? "§a▶ Kliknij, aby zdezaktywować" : "§c▶ Kliknij, aby aktywować"
        );
    }
}
