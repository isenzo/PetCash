package org.isenzo.petPlugin.models;

import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.managers.PetManager;
import org.isenzo.petPlugin.utils.SkullUtil;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class Pet {
    private String id;
    private String name;
    private String type;
    private int level;
    private double experience;
    private Player owner;
    private UUID armorStandId;
    private ArmorStand entity;
    private boolean active;
    private int positionIndex;
    private int power;

    private CoinBlock targetBlock;

    private static final String BASE64 = "d42e9dbd6eb544f6bb59cb2569e9113d750931e3ad01fd586eebd0f071e492d0";

    private transient PetManager petManager;

    public Pet(String id, String name, String type, Player owner, int positionIndex, PetManager petManager) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.level = 1;
        this.experience = 0;
        this.active = false;
        this.positionIndex = positionIndex;
        this.petManager = petManager;
        this.power = determinePower(type);
    }

    private int determinePower(String type) {
        switch (type.toLowerCase()) {
            case "silver": return 20;
            case "radiation": return 50;
            default: return 10;
        }
    }

    public void spawn(Location location) {
        if (Objects.nonNull(entity) && !entity.isDead()) {
            entity.remove();
        }

        entity = (ArmorStand) owner.getWorld().spawnEntity(owner.getLocation().add(0, 0.5, 0), EntityType.ARMOR_STAND);
        entity.setVisible(false);
        entity.setSmall(true);
        entity.setCustomName(ChatColor.GOLD + name + " §8[§2Level §a" + level + "§8]");
        entity.setCustomNameVisible(true);
        entity.setGravity(false);
        entity.getEquipment().setHelmet(SkullUtil.getCustomHead(BASE64));

        active = true;
        armorStandId = entity.getUniqueId();

        if(Objects.nonNull(petManager)) {
            petManager.updatePetInDatabase(this);
        }
    }

    public void moveToPlayerSmoothly() {
        if (entity == null || !active || owner == null) {
            return;
        }

        double angle = (positionIndex * (2.3 * Math.PI)) / 5;
        double offsetX = Math.cos(angle) * 1.2;
        double offsetZ = Math.sin(angle) * 1.2;

        Location targetLocation = owner.getLocation().add(offsetX, 1.5, offsetZ);

        entity.teleport(entity.getLocation().add(
                (targetLocation.getX() - entity.getLocation().getX()) * 0.3,
                (targetLocation.getY() - entity.getLocation().getY()) * 0.3,
                (targetLocation.getZ() - entity.getLocation().getZ()) * 0.3
        ));
    }

    public void despawn() {
        if (Objects.nonNull(entity)) {
            if(!entity.isDead()) {
                Bukkit.getLogger().info("[DEBUG] <Pet.java> Usuwam encję peta: " + name);
                entity.remove();
            }else {
                Bukkit.getLogger().info("[DEBUG] <Pet.java> Encja peta już była martwa: " + name);
            }
            entity = null;
        }
    }

    public void killArmorStand() {
        if (armorStandId == null) {
            Bukkit.getLogger().warning("[ERROR] <Pet.java> Próba zabicia peta, ale armorStandId jest NULL!");
            return;
        }

        String armorStandUUID = armorStandId.toString();

        Bukkit.getScheduler().runTask(PetMiningPlugin.getInstance(), () -> {
            boolean found = false;
            for (ArmorStand armorStand : owner.getWorld().getEntitiesByClass(ArmorStand.class)) {
                if (armorStand.getUniqueId().toString().equals(armorStandUUID)) {
                    armorStand.remove();
                    found = true;
                    break;
                }
            }

            if (!found) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill " + armorStandUUID);
            }

            Bukkit.getLogger().info("[DEBUG] <Pet.java> Usunięto ArmorStand: " + armorStandUUID);
        });

        armorStandId = null; // Resetujemy ID w obiekcie
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
                .append("positionIndex", positionIndex)
                .append("armorStandId", armorStandId != null ? armorStandId.toString() : null);
    }

    public void updateEntityFromUUID() {
        if (armorStandId == null) {
            Bukkit.getLogger().warning("[DEBUG] <Pet.java> Pet " + name + " nie ma przypisanego ArmorStandId!");
            return;
        }

        for (Entity entity : owner.getWorld().getEntities()) {
            if (entity instanceof ArmorStand && entity.getUniqueId().equals(armorStandId)) {
                this.entity = (ArmorStand) entity;
                Bukkit.getLogger().info("[DEBUG] <Pet.java> Zaktualizowano encję peta " + name + " z bazowego ArmorStandId!");
                return;
            }
        }

        Bukkit.getLogger().warning("[DEBUG] <Pet.java> Nie znaleziono istniejącego ArmorStand dla peta " + name + "!");
    }

    public boolean isAttacking() {
        return targetBlock != null;
    }
}
