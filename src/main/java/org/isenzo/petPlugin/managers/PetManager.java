package org.isenzo.petPlugin.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.models.Pet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
                    Bukkit.getLogger().info("[DEBUG] Zaktualizowano dane petów dla: " + player.getName());
                }
            }
        }, 0L, 100L); // 🔥 100 ticków = 5 sekund
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

    private Pet createPetFromDocument(Document doc, Player player) {
        String petId = doc.getString("_id");
        String petName = doc.getString("name");
        String petType = doc.getString("type");
        int level = doc.containsKey("level") ? ((Number) doc.get("level")).intValue() : 1;
        double experience = doc.containsKey("experience") ? ((Number) doc.get("experience")).doubleValue() : 0.0;
        boolean active = doc.getBoolean("active", false);

        Pet pet = new Pet(petId, petName, petType, player, doc.getInteger("positionIndex", 0));
        pet.setLevel(level);
        pet.setExperience(experience);
        pet.setActive(active);

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

        Pet pet = new Pet(petId, petName, type, player, positionIndex);

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

        if (activePets.getOrDefault(player.getUniqueId(), new HashSet<>()).size() >= maxActivePets) {
            player.sendMessage(ChatColor.RED + "Masz już maksymalną liczbę przywołanych zwierzaków!");
            return;
        }

        pet.setOwner(player);

        Bukkit.getLogger().info("[DEBUG] Pet " + pet.getName() + " owner set to: " + pet.getOwner().getName());

        petSummoner.summonPet(player, pet);

        activePets.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(petId);

        pet.setActive(true);
        updatePetInDatabase(pet);

        Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
            if (pet.getEntity() != null) {
                pet.getEntity().teleport(player.getLocation().add(0, 1.5, 0));
            }
        }, 1L);

        petSummoner.startPetMovement(pet, player);
    }

    public void updatePetInDatabase(Pet pet) {
        Bukkit.getScheduler().runTaskAsynchronously(PetMiningPlugin.getInstance(), () -> {
            Document currentDoc = petsCollection.find(Filters.eq("_id", pet.getId())).first();

            if (currentDoc != null) {
                int currentLevel = ((Number) currentDoc.get("level")).intValue();
                double currentExp = ((Number) currentDoc.get("experience")).doubleValue();

                // 🔥 Jeśli poziom lub exp się zmienił w bazie, nie nadpisujemy!
                if (currentLevel != pet.getLevel() || currentExp != pet.getExperience()) {
                    Bukkit.getLogger().info("[DEBUG] Pet " + pet.getName() + " ma różne dane w bazie. Pobieram nowe...");
                    loadPetsFromDatabase(pet.getOwner()); // 🔥 Pobieramy najnowsze dane
                    return;
                }
            }

            try {
                petsCollection.updateOne(
                        Filters.eq("_id", pet.getId()),
                        Updates.combine(
                                Updates.set("active", pet.isActive()),
                                Updates.set("level", pet.getLevel()),
                                Updates.set("experience", pet.getExperience())
                        )
                );
                Bukkit.getLogger().info("[DEBUG] Zapisano zmiany w bazie dla " + pet.getName());
            } catch (Exception e) {
                PetMiningPlugin.getInstance().getLogger().severe("Error updating pet in MongoDB: " + e.getMessage());
            }
        });
    }

    public void despawnPet(Player player, String petId) {
        Optional<Pet> petOpt = findPetById(player, petId);
        if (!petOpt.isPresent()) {
            player.sendMessage(ChatColor.RED + "Error: Pet not found!");
            return;
        }

        Pet pet = petOpt.get();
        pet.setActive(false);
        pet.despawn();

        petSummoner.despawnPet(player, pet);

        activePets.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).remove(petId);

        updatePetInDatabase(pet);

        player.sendMessage(ChatColor.YELLOW + "✔ You have despawned " + pet.getName() + ".");
    }

    public void saveAndDespawnPets(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<Pet> pets = getPlayerPets(player);

        for (Pet pet : pets) {
            if (pet.isActive()) {
                if (pet.getEntity() != null && !pet.getEntity().isDead()) {
                    pet.getEntity().remove();
                }
                pet.setActive(false);
                pet.despawn();
                updatePetInDatabase(pet);
            }
        }

        playerPets.remove(playerUUID);
        activePets.remove(playerUUID);

        playerPets.remove(playerUUID);
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

    public int getMaxOwnedPets() {
        return maxOwnedPets;
    }

    public int getMaxActivePets() {
        return maxActivePets;
    }

    public void addActivePet(Player player, String petId) {
        activePets.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(petId);
    }

    public void removeActivePet(Player player, String petId) {
        Set<String> set = activePets.get(player.getUniqueId());
        if (set != null) {
            set.remove(petId);
        }
    }
}
