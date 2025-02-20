package org.isenzo.petPlugin.models;

import lombok.Data;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.isenzo.petPlugin.PetMiningPlugin;

@Data
public class CoinBlock {
    private Location location;
    private double maxHealth;
    private double currentHealth;
    private boolean breaking;
    private long lastDamageTime;

    private HealthBarArmorStand healthBar;

    public CoinBlock(Location location, double maxHealth) {
        this.location = location;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.breaking = false;
        this.lastDamageTime = 0;

        Location standLoc = location.clone().add(0, -0.4, 0);
        this.healthBar = new HealthBarArmorStand();
        this.healthBar.spawn(standLoc, currentHealth, maxHealth);
    }

    public void damage(double amount, Player player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDamageTime < 50) {
            return;
        }
        currentHealth -= amount;
        lastDamageTime = currentTime;

        this.healthBar.updateHP(currentHealth, maxHealth);

        if (currentHealth <= 0) {
            breakBlock(player);
        }
    }


    private void breakBlock(Player player) {
        Block block = location.getBlock();
        block.setType(Material.AIR);

        if (healthBar != null) {
            healthBar.remove();
            healthBar = null;
        }

        player.sendMessage("Zebrałeś monety!");
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 3));
        player.playEffect(player.getLocation(), Effect.PHANTOM_BITE, null);

        PetMiningPlugin.getInstance().getServer().getScheduler().runTaskLater(
                PetMiningPlugin.getInstance(),
                this::respawn,
                100L
        );
    }

    private void respawn() {
        Block block = location.getBlock();
        block.setType(Material.BARRIER);
        currentHealth = maxHealth;

        this.healthBar = new HealthBarArmorStand();
        this.healthBar.spawn(location.clone().add(0, 0, 0), currentHealth, maxHealth);
    }
}
