package org.isenzo.petPlugin.models;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter
public class HealthBarArmorStand {
    private ArmorStand stand;

    private static final int TOTAL_SEGMENTS = 30;

    public void spawn(Location location, double currentHp, double maxHp) {
        Location spawnLoc = location.clone().add(0.5, 1.3, 0.5);

        stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setBasePlate(false);
        stand.setMarker(true);
        stand.setCustomNameVisible(true);
        stand.setSmall(true);

        stand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.ADDING_OR_CHANGING);

        ItemStack nugget = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = nugget.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(2);
            nugget.setItemMeta(meta);
        }

        if (stand != null && !stand.isDead()) {
            stand.getEquipment().setHelmet(nugget);
        }

        updateHP(currentHp, maxHp);
    }

    public void updateHP(double currentHp, double maxHp) {
        if (stand == null || stand.isDead()) {
            Bukkit.getLogger().warning("[DEBUG] <HealthBarArmorStand.java> Próba aktualizacji paska HP, ale ArmorStand nie istnieje!");
            return;
        }

        String bar = getProgressBar(currentHp, maxHp, TOTAL_SEGMENTS);
        stand.setCustomName(bar);
        Bukkit.getLogger().info("[DEBUG] <HealthBarArmorStand.java> Pasek HP zaktualizowany: " + currentHp);
    }

    private String getProgressBar(double current, double max, int totalSegments) {
        double ratio = Math.max(0, Math.min(1, current / max));
        int filled = (int) Math.round(ratio * totalSegments);

        ChatColor color;
        if (ratio > 0.66) {
            color = ChatColor.GREEN;
        } else if (ratio > 0.33) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalSegments; i++) {
            if (i < filled) {
                sb.append(color).append("|");
            } else {
                sb.append(ChatColor.GRAY).append("|");
            }
        }
        return sb.toString();
    }

    public void remove() {
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }
}
