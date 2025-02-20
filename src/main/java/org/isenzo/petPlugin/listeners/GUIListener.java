package org.isenzo.petPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.gui.PetMenuGUI;
import org.isenzo.petPlugin.gui.PetShopGUI;
import org.isenzo.petPlugin.managers.PetManager;

import java.util.List;

public class GUIListener implements Listener {
    private final PetManager petManager;

    public GUIListener() {
        this.petManager = PetMiningPlugin.getInstance().getPetManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;

        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (!title.equals("§6Pet Management") && !title.equals("§6Pet Shop")) {
            return;
        }

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (title.equals("§6Pet Management")) {
            handlePetMenuClick(player, clickedItem);
        } else if (title.equals("§6Pet Shop")) {
            handlePetShopClick(player, clickedItem);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (title.equals("§6Pet Management") || title.equals("§6Pet Shop")) {
            player.sendMessage(ChatColor.GRAY + "Menu zamknięte. Możesz używać ekwipunku.");
        }
    }

    private void handlePetMenuClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || meta.getDisplayName() == null || meta.getLore() == null) return;

        String itemName = ChatColor.stripColor(meta.getDisplayName());

        if (itemName.equalsIgnoreCase("Get New Pet")) {
            new PetShopGUI(player).open();
            return;
        }

        String petId = getPetIdFromItem(clickedItem);
        if (petId != null) {
            if (isPetActive(meta.getLore())) {
                petManager.despawnPet(player, petId);
            } else {
                petManager.spawnPet(player, petId);
            }
            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "Error: Could not find pet ID!");
        }
    }

    private void handlePetShopClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        String itemName = ChatColor.stripColor(meta.getDisplayName());

        if (itemName.equalsIgnoreCase("Back to Pet Menu")) {
            new PetMenuGUI(player).open();
            return;
        }

        if (itemName.contains("Pet")) {
            String type = itemName.replace(" Pet", "").trim();
            if (petManager.canPurchasePet(player, type)) {
                petManager.purchasePet(player, type);
                player.closeInventory();
            } else {
                player.sendMessage(ChatColor.RED + "You cannot purchase this pet right now!");
            }
        }
    }

    private String getPetIdFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return null;

        for (String line : meta.getLore()) {
            if (line.contains("ID:")) {
                return ChatColor.stripColor(line).replace("ID: ", "").trim();
            }
        }
        return null;
    }

    private boolean isPetActive(List<String> lore) {
        for (String line : lore) {
            if (ChatColor.stripColor(line).contains("Currently Active")) {
                return true;
            }
        }
        return false;
    }
}
