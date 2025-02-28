package org.isenzo.petPlugin.models;

import lombok.Data;
import org.bukkit.Bukkit;
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
    private Player owner;

    private HealthBarArmorStand healthBar;

    public CoinBlock(Location location, double maxHealth, Player owner) {
        this.location = location;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.breaking = false;
        this.lastDamageTime = 0;
        this.owner = owner;

        Location standLoc = location.clone().add(0, -0.4, 0);
        this.healthBar = new HealthBarArmorStand();
        this.healthBar.spawn(standLoc, currentHealth, maxHealth);
    }

    public void damage(double amount, Player player) {
        currentHealth -= amount;
        if (currentHealth <= 0) {
            breakBlock(player);
        }
    }

    private void breakBlock(Player player) {
        Block block = location.getBlock();
        block.setType(Material.AIR);

        player.sendMessage("Zebrałeś monety!");
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 3));
        player.playEffect(player.getLocation(), Effect.WITHER_BREAK_BLOCK, null);
        removeHealthBar();

        Bukkit.getLogger().info("[DEBUG] <CoinBlock.java> CoinBlock zniszczony, regeneracja za 5 sekund...");

        // 📌 Przekazujemy obsługę respawnu do CoinBlockManager!
        PetMiningPlugin.getInstance().getCoinBlockManager().scheduleRespawn(this);
    }


    public void respawn() {
        Block block = location.getBlock();
        block.setType(Material.GOLD_BLOCK);
        currentHealth = maxHealth;

        this.healthBar = new HealthBarArmorStand();
        this.healthBar.spawn(location.clone().add(0.5, 1.3, 0.5), currentHealth, maxHealth);

        Bukkit.getLogger().info("[DEBUG] <CoinBlock.java> CoinBlock odrodzony z HP = " + maxHealth);
    }

    public void removeHealthBar() {
        if (healthBar != null) {
            healthBar.remove();
            healthBar = null;
            Bukkit.getLogger().info("[DEBUG] <CoinBlock.java> Usunięto pasek HP CoinBlocka!");
        }
    }

    public void saveToDatabase() {
        Bukkit.getLogger().info("[DEBUG] <CoinBlock.java> Zapisano CoinBlock do MongoDB: " + location);
    }

    public void playDamageEffect() {
        location.getWorld().playSound(location, "entity.player.attack.strong", 1f, 1f);
    }

    public void spawnHealthBar() {
        if (healthBar == null) {
            this.healthBar = new HealthBarArmorStand();
            this.healthBar.spawn(location.clone().add(0, 1, 0), currentHealth, maxHealth);
        }
    }

}
