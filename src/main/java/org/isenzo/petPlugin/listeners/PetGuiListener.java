package org.isenzo.petPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.gui.PetGUI;
import org.isenzo.petPlugin.managers.PetManager;
import org.isenzo.petPlugin.managers.PetSummoner;

import java.util.List;

public class PetGuiListener implements Listener {

    private final PetManager petManager;

    public PetGuiListener(PetManager petManager, PetSummoner petSummoner) {
        this.petManager = petManager;
    }

    @EventHandler
    public void onPetGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        // 📌 Sprawdzenie czy GUI to menu petów
        if (!event.getView().getTitle().equalsIgnoreCase(ChatColor.DARK_GREEN + "Twoje Pety 🐾")) {
            return;
        }

        event.setCancelled(true); // 🔥 BLOKUJEMY WYCIĄGANIE PRZEDMIOTÓW!

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || meta.getLore() == null) {
            return;
        }

        String petId = getPetIdFromLore(meta.getLore());
        if (petId == null) {
            player.sendMessage(ChatColor.RED + "❌ Błąd: Nie znaleziono ID peta!");
            return;
        }

        boolean isActive = petManager.isPetActiveInDatabase(petId);

        if (isActive) {
            petManager.despawnPet(player, petId);
            player.sendMessage(ChatColor.RED + "❌ Twój pet został odwołany!");
        } else {
            petManager.spawnPet(player, petId);
            player.sendMessage(ChatColor.GREEN + "✅ Twój pet został przywołany!");
        }

        // 🔥 Odświeżamy GUI po kliknięciu
        Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
            new PetGUI().openPetMenu(player);
        }, 1L);
    }

    private String getPetIdFromLore(List<String> lore) {
        for (String line : lore) {
            String cleanLine = ChatColor.stripColor(line).trim();
            if (cleanLine.startsWith("ID:")) {
                return cleanLine.replace("ID: ", "").trim();
            }
        }
        return null;
    }
}
