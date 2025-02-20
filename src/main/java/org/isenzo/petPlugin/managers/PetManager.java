package org.isenzo.petPlugin.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.models.Pet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PetManager {
    private PetSummoner petSummoner;

    private final MongoCollection<Document> petsCollection;
    private final Map<UUID, List<Pet>> playerPets;
    private final Map<UUID, Set<String>> activePets;
    private final Map<UUID, List<Pet>> petDataCache;

    private final int maxOwnedPets;
    private final int maxActivePets;

    public PetManager(PetSummoner petSummoner) {
        this.petSummoner = petSummoner;
        this.petsCollection = PetMiningPlugin.getInstance().getDatabase().getCollection("pets");
        this.playerPets = new ConcurrentHashMap<>();
        this.activePets = new ConcurrentHashMap<>();
        this.petDataCache = new ConcurrentHashMap<>();

        this.maxOwnedPets = PetMiningPlugin.getInstance().getConfig().getInt("pets.max_owned", 10);
        this.maxActivePets = PetMiningPlugin.getInstance().getConfig().getInt("pets.max_active", 5);

        startAutoRefresh();
    }

    private void startAutoRefresh() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(PetMiningPlugin.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID playerUUID = player.getUniqueId();
                List<Pet> updatedPets = loadPetsFromDatabase(player);

                if (!updatedPets.equals(petDataCache.get(playerUUID))) {
                    petDataCache.put(playerUUID, updatedPets);
                    playerPets.put(playerUUID, updatedPets);
                }
            }
        }, 0L, 100L);
    }

    public List<Pet> getPlayerPets(Player player) {
        UUID uuid = player.getUniqueId();

        List<Pet> cachedPets = playerPets.get(uuid);
        if (cachedPets != null) {
            return cachedPets;
        }

        List<Pet> pets = loadPetsFromDatabase(player);
        playerPets.put(uuid, pets);

        return pets;
    }

    private List<Pet> loadPetsFromDatabase(Player player) {
        List<Pet> pets = new ArrayList<>();
        for (Document doc : petsCollection.find(Filters.eq("owner", player.getUniqueId().toString()))) {
            pets.add(createPetFromDocument(doc, player));
        }
        return pets;
    }

    public List<Pet> getActivePetsFromDatabase(Player player) {
        List<Pet> activePets = new ArrayList<>();

        for (Document document : petsCollection.find(Filters.and(
                Filters.eq("owner", player.getUniqueId().toString()),
                Filters.eq("active", true)
        ))) {
            activePets.add(createPetFromDocument(document, player));
        }

        return activePets;
    }

    public boolean isPetActiveInDatabase(String petId) {
        Document document = petsCollection.find(Filters.eq("_id", petId)).first();
        return Objects.nonNull(document) && document.getBoolean("active", false);
    }

    private Pet createPetFromDocument(Document doc, Player player) {
        String petId = doc.getString("_id");
        String petName = doc.getString("name");
        String petType = doc.getString("type");
        int level = doc.containsKey("level") ? ((Number) doc.get("level")).intValue() : 1;
        double experience = doc.containsKey("experience") ? ((Number) doc.get("experience")).doubleValue() : 0.0;
        boolean active = doc.getBoolean("active", false);

        UUID armorStandId = null;
        if (doc.containsKey("armorStandId") && doc.getString("armorStandId") != null && !doc.getString("armorStandId").isEmpty()) {
            try {
                armorStandId = UUID.fromString(doc.getString("armorStandId"));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[PetPlugin] ⚠ Błędny UUID ArmorStand w bazie dla peta: " + petName);
            }
        }

        Pet pet = new Pet(petId, petName, petType, player, doc.getInteger("positionIndex", 0), this);
        pet.setLevel(level);
        pet.setExperience(experience);
        pet.setActive(active);
        pet.setArmorStandId(armorStandId);

        return pet;
    }

    public boolean canPurchasePet(Player player, String type) {
        List<Pet> pets = getPlayerPets(player);

        if (pets == null) {
            player.sendMessage("§cError: Could not load your pets.");
            return false;
        }

        if (pets.size() >= maxOwnedPets) {
            player.sendMessage("§cYou have reached the maximum number of pets!");
            return false;
        }

        int cost = getPetCost(type);
        if (!hasEnoughMoney(player, cost)) {
            player.sendMessage("§cYou don't have enough money! Need: " + cost + " coins");
            return false;
        }

        return true;
    }

    public void purchasePet(Player player, String type) {
        if (!canPurchasePet(player, type)) {
            return;
        }

        int cost = getPetCost(type);
        withdrawMoney(player, cost);

        String petId = UUID.randomUUID().toString();
        String petName = type + " Pet #" + (getPlayerPets(player).size() + 1);

        int positionIndex = getPlayerPets(player).size();

        Pet pet = new Pet(petId, petName, type, player, positionIndex, this);

        try {
            petsCollection.insertOne(pet.toDocument());

            playerPets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(pet);

            player.sendMessage("§aSuccessfully purchased " + petName + "!");
        } catch (Exception e) {
            PetMiningPlugin.getInstance().getLogger().severe("Error saving pet to MongoDB: " + e.getMessage());
        }
    }

    public void spawnPet(Player player, String petId) {
        Optional<Pet> petOpt = findPetById(player, petId);
        if (!petOpt.isPresent()) return;

        Pet pet = petOpt.get();
        if (pet.isActive()) {
            player.sendMessage(ChatColor.RED + "Ten zwierzak jest już przywołany!");
            return;
        }

        if (pet.getEntity() != null) {
            pet.getEntity().remove();
            pet.setEntity(null);
        }

        pet.setOwner(player);
        pet.spawn(player.getLocation().add(0, 1.5, 0));

        activePets.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(petId);

        pet.setActive(true);
        updatePetInDatabase(pet);

        petSummoner.startPetMovement(pet, player);
    }

    public void updatePetInDatabase(Pet pet) {
        Bukkit.getScheduler().runTaskAsynchronously(PetMiningPlugin.getInstance(), () -> {
            try {
                petsCollection.updateOne(
                        Filters.eq("_id", pet.getId()),
                        Updates.combine(
                                Updates.set("active", pet.isActive()),
                                Updates.set("level", pet.getLevel()),
                                Updates.set("experience", pet.getExperience()),
                                Updates.set("armorStandId", pet.getArmorStandId() != null ? pet.getArmorStandId().toString() : null)
                        )
                );
                Bukkit.getLogger().info("[DEBUG] Zaktualizowano peta w bazie: " + pet.getName() + " (active=" + pet.isActive() + ", armorStandId=" + pet.getArmorStandId() + ")");
            } catch (Exception e) {
                Bukkit.getLogger().severe("Błąd zapisu peta w bazie: " + e.getMessage());
            }
        });
    }

    public void despawnPet(Player player, String petId) {
        Optional<Pet> petOpt = findPetById(player, petId);
        if (petOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Error: Pet not found!");
            return;
        }

        Pet pet = petOpt.get();

        if (!pet.isActive()) {
            player.sendMessage(ChatColor.RED + "This pet is not currently summoned!");
            return;
        }

        pet.killArmorStand();
        pet.setArmorStandId(null);
        pet.setActive(false);
        updatePetInDatabase(pet);

        playerPets.computeIfPresent(player.getUniqueId(), (uuid, pets) -> {
            pets.remove(pet);
            return pets.isEmpty() ? null : pets;
        });

        player.sendMessage(ChatColor.YELLOW + "You have despawned " + pet.getName() + ".");
    }

    public void saveAndDespawnPets(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<Pet> activePets = getActivePetsFromDatabase(player);

        for (Pet pet : activePets) {
            if (Objects.nonNull(pet.getEntity())) {
                pet.getEntity().remove();
                pet.setEntity(null);
            }
            pet.despawn();
        }

        playerPets.remove(playerUUID);
        activePets.remove(playerUUID);
    }

    private Optional<Pet> findPetById(Player player, String petId) {
        List<Pet> pets = playerPets.get(player.getUniqueId());

        if (pets != null) {
            return pets.stream()
                    .filter(p -> p.getId().equals(petId))
                    .findFirst();
        }

        return loadPetsFromDatabase(player).stream()
                .filter(p -> p.getId().equals(petId))
                .findFirst();
    }

    private int getPetCost(String type) {
        switch (type.toLowerCase()) {
            case "silver":
                return 1000;
            case "radiation":
                return 2000;
            default:
                return 5000;
        }
    }

    private boolean hasEnoughMoney(Player player, int amount) {
        return true;
    }

    private void withdrawMoney(Player player, int amount) {
    }

    public void addActivePet(Player player, String petId) {
        activePets.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(petId);
    }

    public void clearPlayerCache(Player player) {
        UUID playerUUID = player.getUniqueId();

        playerPets.remove(playerUUID);
        activePets.remove(playerUUID);
        petDataCache.remove(playerUUID);
    }
}
