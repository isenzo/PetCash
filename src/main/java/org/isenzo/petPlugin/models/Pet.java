package org.isenzo.petPlugin.models;

import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.utils.SkullUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Pet {
    private String id;
    private String name;
    private String type;
    private int level;
    private double experience;
    private Player owner;
    private ArmorStand entity;
    private boolean active;
    private int positionIndex;

    private static final String BASE64 = "d42e9dbd6eb544f6bb59cb2569e9113d750931e3ad01fd586eebd0f071e492d0";

    public Pet(String id, String name, String type, Player owner, int positionIndex) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.level = 1;
        this.experience = 0;
        this.active = false;
        this.positionIndex = positionIndex;
    }

    public void spawn(Location location) {
        if (entity != null && !entity.isDead()) return;

        entity = (ArmorStand) owner.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        entity.setVisible(false);
        entity.setSmall(true);
        entity.setCustomName(ChatColor.GOLD + name);
        entity.setCustomNameVisible(true);
        entity.setGravity(false);

        entity.getEquipment().setHelmet(SkullUtil.getCustomHead(BASE64));
        active = true;
    }

    public void moveToPlayerSmoothly() {
        if (entity == null || !active || owner == null) return;

        double angle = (positionIndex * (2 * Math.PI)) / 5;
        double offsetX = Math.cos(angle) * 1.5;
        double offsetZ = Math.sin(angle) * 1.5;

        Location targetLocation = owner.getLocation().add(offsetX, 1.5, offsetZ);

        entity.teleport(entity.getLocation().add(
                (targetLocation.getX() - entity.getLocation().getX()) * 0.3,
                (targetLocation.getY() - entity.getLocation().getY()) * 0.3,
                (targetLocation.getZ() - entity.getLocation().getZ()) * 0.3
        ));
    }

    public void updatePetInfo() {
        Document doc = PetMiningPlugin.getInstance().getDatabase().getCollection("pets")
                .find(Filters.eq("_id", this.id))
                .first();

        if (doc != null) {
            int newLevel = doc.containsKey("level") ? doc.getInteger("level") : this.level;
            double newExp = doc.containsKey("experience") ? doc.getDouble("experience") : this.experience;

            if (newLevel != this.level || newExp != this.experience) {
                this.level = newLevel;
                this.experience = newExp;

                Bukkit.getScheduler().runTask(PetMiningPlugin.getInstance(), this::updateNameAndLore);
            }
        }
    }

    private void updateNameAndLore() {
        if (entity == null || entity.isDead()) return;

        entity.setCustomName(ChatColor.GOLD + name + ChatColor.GRAY + " [LvL " + ChatColor.GREEN + level + ChatColor.GRAY + "]");

        ItemStack skull = SkullUtil.getCustomHead(BASE64);
        ItemMeta meta = skull.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Poziom: " + ChatColor.YELLOW + level);
            lore.add(ChatColor.GRAY + "Exp: " + ChatColor.GREEN + experience);
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }

        entity.getEquipment().setHelmet(skull);
    }

    public void despawn() {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        active = false;
    }

    public Document toDocument() {
        return new Document()
                .append("_id", id)
                .append("name", name)
                .append("type", type)
                .append("level", level)
                .append("experience", experience)
                .append("owner", owner.getUniqueId().toString())
                .append("active", active)
                .append("positionIndex", positionIndex);
    }
}
