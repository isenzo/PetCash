package org.isenzo.petPlugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class CoinItemUtil {

    private static final String SPECIAL_SWORD_NAME = ChatColor.GREEN + "Magiczna Flet";

    public static ItemStack createCoinSword() {
        ItemStack sword = new ItemStack(Material.STICK);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SPECIAL_SWORD_NAME);
            meta.setLore(Collections.singletonList(ChatColor.YELLOW + "Rozkurwiator bloków"));
                    sword.setItemMeta(meta);
        }
        return sword;
    }

    public static boolean isCoinSword(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        return meta.getDisplayName().equals(SPECIAL_SWORD_NAME);
    }
}
