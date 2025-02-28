package org.isenzo.petPlugin.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.models.CoinBlock;
import org.isenzo.petPlugin.models.Pet;
import org.isenzo.petPlugin.utils.CoinItemUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CoinBlockManager {

    private final Map<Location, CoinBlock> coinBlocks = new ConcurrentHashMap<>();
    private final MongoCollection<Document> coinBlockCollection;
    private final Set<CoinBlock> blocksToRespawnMap = ConcurrentHashMap.newKeySet();
    private final Map<Pet, CoinBlock> actualPetMap = new ConcurrentHashMap<>();

    public CoinBlockManager() {
        this.coinBlockCollection = PetMiningPlugin.getInstance().getDatabase().getCollection("coinblocks");
        loadCoinBlocksFromDatabase();
    }

    public boolean isCoinBlock(Location location) {
        return coinBlocks.containsKey(location);
    }

    public void handleBlockDamage(Block block, Player player) {
        CoinBlock coinBlock = coinBlocks.get(block.getLocation());
        if (coinBlock == null) {
            return;
        }

        if (!CoinItemUtil.isCoinSword(player.getInventory().getItemInMainHand())) {
            return;
        }

        double damage = 12.5;
        coinBlock.damage(damage, player);
        Bukkit.getLogger().info("[DEBUG] <CoinBlockManager.java> handleBlockDamage: CoinBlock otrzymał " + damage + " obrażeń!");

        if (coinBlock.getHealthBar() != null) {
            Bukkit.getLogger().info("[DEBUG] <CoinBlockManager.java> Aktualizowanie paska HP CoinBlocka!");
            coinBlock.getHealthBar().updateHP(coinBlock.getCurrentHealth(), coinBlock.getMaxHealth());
            player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1f);
        } else {
            Bukkit.getLogger().warning("[DEBUG] <CoinBlockManager.java> Brak paska HP dla CoinBlocka!");
        }
    }

    public void addCoinBlock(Location location, double maxHealth, Player owner) {
        // ✅ Jeśli już istnieje, nie dodawaj ponownie
        if (coinBlocks.containsKey(location)) {
            Bukkit.getLogger().warning("[DEBUG] <CoinBlockManager.java> CoinBlock na " + location + " już istnieje!");
            return;
        }

        // ✅ Ustawiamy blok na GOLD_BLOCK
        Block block = location.getBlock();
        block.setType(Material.GOLD_BLOCK);

        // ✅ Tworzymy nowy CoinBlock i zapisujemy w bazie
        CoinBlock coinBlock = new CoinBlock(location, maxHealth, owner);
        coinBlocks.put(location, coinBlock);
        saveCoinBlockToDatabase(coinBlock);

        Bukkit.getLogger().info("[DEBUG] <CoinBlockManager.java> Dodano CoinBlock na " + location + " z HP = " + maxHealth);
    }


    public void saveCoinBlockToDatabase(CoinBlock block) {
        Document doc = new Document()
                .append("_id", UUID.randomUUID().toString())
                .append("x", block.getLocation().getBlockX())
                .append("y", block.getLocation().getBlockY())
                .append("z", block.getLocation().getBlockZ())
                .append("world", Objects.requireNonNull(block.getLocation().getWorld()).getName())
                .append("maxHealth", block.getMaxHealth())
                .append("currentHealth", block.getCurrentHealth())
                .append("ownerUUID", block.getOwner().getUniqueId().toString())
                .append("ownerName", block.getOwner().getName());

        coinBlockCollection.insertOne(doc);
        Bukkit.getLogger().info("[DEBUG] <CoinBlockManager.java> Zapisano CoinBlock do MongoDB: " + block.getLocation());
    }

    public void deleteCoinBlockFromDatabase(Location location) {
        coinBlockCollection.deleteOne(Filters.and(
                Filters.eq("x", location.getBlockX()),
                Filters.eq("y", location.getBlockY()),
                Filters.eq("z", location.getBlockZ()),
                Filters.eq("world", Objects.requireNonNull(location.getWorld()).getName())
        ));
    }

    public void loadCoinBlocksFromDatabase() {
        for (Document doc : coinBlockCollection.find()) {
            Location location = new Location(
                    Bukkit.getWorld(doc.getString("world")),
                    doc.getInteger("x"),
                    doc.getInteger("y"),
                    doc.getInteger("z")
            );

            double maxHealth = doc.getDouble("maxHealth");
            double currentHealth = doc.getDouble("currentHealth");
            UUID ownerUUID = UUID.fromString(doc.getString("ownerUUID"));
            Player owner = Bukkit.getPlayer(ownerUUID);

            World world = location.getWorld();
            if (world == null) {
                Bukkit.getLogger().warning("[ERROR] <CoinBlockManager.java> Świat " + doc.getString("world") + " nie został znaleziony!");
                continue;
            }

            // ✅ USUWAMY STARE ARMORSTANDY Z BAZY
            location.getWorld().getNearbyEntities(location, 1, 2, 1).forEach(entity -> {
                if (entity instanceof ArmorStand) {
                    entity.remove();
                }
            });

            CoinBlock coinBlock = new CoinBlock(location, maxHealth, owner);
            coinBlock.setCurrentHealth(currentHealth);
            location.getBlock().setType(Material.GOLD_BLOCK);
            coinBlock.spawnHealthBar();
            coinBlocks.put(location, coinBlock);
        }
    }

    public void handlePetAttack(Player player, Block block, boolean groupAttack) {
        CoinBlock coinBlock = coinBlocks.get(block.getLocation());
        long currentAttackers = getAttackingPetsCount(coinBlock);
        long attackingBlocks = actualPetMap.values().stream().distinct().count();

        if (Objects.isNull(coinBlock)) {
            return;
        }

        List<Pet> activePets = PetMiningPlugin.getInstance().getPetManager().getActivePetsFromDatabase(player);

        if (activePets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Nie masz aktywnych petów!");
            return;
        }

        // 🔹 FILTRUJEMY PETY, KTÓRE JUŻ NIE ATAKUJĄ INNEGO BLOKU
        List<Pet> availablePets = activePets.stream()
                .filter(pet -> !actualPetMap.containsKey(pet)) // 🔥 Tylko pety, które nic nie niszczą
                .sorted(Comparator.comparingInt(Pet::getPower).reversed()) // 🔥 Sortujemy według siły (mocniejsi pierwsi)
                .toList();

        if (availablePets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Twoje pety są już zajęte atakowaniem innych bloków!");
            return;
        }

        if (attackingBlocks >= 5) {
            player.sendMessage(ChatColor.RED + "Twoje pety już atakują 5 bloków!");
            return;
        }

        if (currentAttackers >= 4) {
            player.sendMessage(ChatColor.RED + "Ten blok już atakuje 4 petów!");
            return;
        }

        startPetAttack(availablePets.get(0), coinBlock);


        if (groupAttack) {
            // 🔹 JEŚLI ATAK GRUPOWY, WYBIERAMY MAX 5 PETÓW I PRZYDZIELAMY IM 5 BLOKÓW
            List<Block> blocksToAttack = findNearbyCoinBlocks(block, 5); // Znajdujemy 5 najbliższych bloków

            for (int i = 0; i < Math.min(availablePets.size(), blocksToAttack.size()); i++) {
                startPetAttack(availablePets.get(i), coinBlocks.get(blocksToAttack.get(i).getLocation()));
            }
        } else {
            // 🔹 JEŚLI POJEDYNCZY ATAK, WYBIERAMY NAJSILNIEJSZEGO PETA DO BLOKU
            startPetAttack(availablePets.get(0), coinBlock);
        }
    }

    private void startPetAttack(Pet pet, CoinBlock coinBlock) {
        Bukkit.getLogger().info("[DEBUG] 🛠 Sprawdzanie statusu peta w bazie...");
        if (!PetMiningPlugin.getInstance().getPetManager().isPetActiveInDatabase(pet.getId())) {
            Bukkit.getLogger().warning("[DEBUG] ❌ Pet " + pet.getName() + " NIE JEST aktywny w bazie! Przerywam atak.");
            return;
        }

        pet.updateEntityFromUUID();
        if (pet.getEntity() == null) {
            Bukkit.getLogger().severe("[ERROR] ❌ Nie udało się pobrać istniejącego ArmorStand dla peta: " + pet.getName());
            return;
        }

        if (actualPetMap.containsKey(pet)) {
            Bukkit.getLogger().info("[DEBUG] ❌ Pet " + pet.getName() + " już atakuje blok! Ignoruję nowe polecenie.");
            return;
        }

        // WYŁĄCZ RUCH ZA GRACZEM
        PetMiningPlugin.getInstance().getPetManager().getPetSummoner().stopPetMovement(pet);

        // USTAWIAMY PETA NA ŚRODKU BLOKU
        Location coinBlockLoc = coinBlock.getLocation();
        Location petPosition = coinBlockLoc.clone().add(0.5, 0.5, 0.5);
        pet.getEntity().teleport(petPosition);
        Bukkit.getLogger().info("[DEBUG] 🐾 Pet " + pet.getName() + " został ustawiony na środku bloku!");

        // DODAJEMY PETA DO LISTY ATAKUJĄCYCH
        actualPetMap.put(pet, coinBlock);

        // Dodajemy animację ataku peta
        petAttackCoinBlockAnimation(coinBlock, pet);
    }

    public CoinBlock getCoinBlock(Location location) {
        return coinBlocks.get(location);
    }

    public void removeCoinBlock(Location location) {
        CoinBlock coinBlock = coinBlocks.remove(location);

        if (Objects.nonNull(coinBlock)) {
            location.getBlock().setType(Material.AIR);

            if (coinBlock.getHealthBar() != null) {
                coinBlock.getHealthBar().remove();
            }

            deleteCoinBlockFromDatabase(location);

            Objects.requireNonNull(location.getWorld()).spawnParticle(Particle.LARGE_SMOKE, location.add(0.5, 1, 0.5), 20, 0.3, 0.3, 0.3, 0.05);
            location.getWorld().spawnParticle(Particle.FLAME, location.add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.02);
            location.getWorld().playSound(location, Sound.ENTITY_ALLAY_ITEM_TAKEN, 1f, 1f);

            Bukkit.getLogger().info("[DEBUG] Usunięto CoinBlock z " + location);
        }
    }

    public void removeAllCoinBlocksFromWorldOnServerDisable() {
        for (CoinBlock coinBlock : coinBlocks.values()) {
            coinBlock.getLocation().getBlock().setType(Material.AIR);
            coinBlock.removeHealthBar();
        }
        coinBlocks.clear();
        Bukkit.getLogger().info("[DEBUG] <CoinBlockManager.java> Wszystkie CoinBlocki zostały usunięte z gry przed zamknięciem serwera.");
    }

    public void scheduleRespawn(CoinBlock block) {
        blocksToRespawnMap.add(block);

        Bukkit.getScheduler().runTaskLater(PetMiningPlugin.getInstance(), () -> {
            if (blocksToRespawnMap.contains(block)) {
                block.respawn();
                blocksToRespawnMap.remove(block);
            }
        }, 100L); // 📌 5 sekund (100 ticków)
    }

    private List<Block> findNearbyCoinBlocks(Block startBlock, int maxBlocks) {
        return coinBlocks.keySet().stream()
                .filter(loc -> !isBlockUnderAttack(loc)) // 🔥 Ignorujemy bloki, które już są atakowane
                .sorted(Comparator.comparingDouble(loc -> loc.distance(startBlock.getLocation()))) // 🔥 Sortujemy po odległości
                .limit(maxBlocks) // 🔥 Bierzemy tylko `maxBlocks` najbliższych bloków
                .map(loc -> loc.getWorld().getBlockAt(loc))
                .toList();
    }

    private boolean isBlockUnderAttack(Location loc) {
        return actualPetMap.containsValue(coinBlocks.get(loc));
    }

    public long getAttackingPetsCount(CoinBlock block) {
        return actualPetMap.values().stream()
                .filter(b -> b.equals(block)).count();
    }

    private void petAttackCoinBlockAnimation(CoinBlock coinBlock, Pet pet) {
        new BukkitRunnable() {
            int attackCounter = 0;
            boolean forward = true;

            @Override
            public void run() {
                if (coinBlock.getCurrentHealth() <= 0) {
                    Bukkit.getLogger().info("[DEBUG] 🏆 Blok został zniszczony! Pet wraca do właściciela.");
                    pet.setTargetBlock(null);
                    actualPetMap.remove(pet); // ✅ Usuwamy z mapy

                    // ✅ PRZYTELEPORTUJ PETA Z POWROTEM DO GRACZA
                    pet.getEntity().teleport(pet.getOwner().getLocation().add(0, 1, 0));

                    // ✅ WŁĄCZ ZNOWU RUCH ZA GRACZEM
                    PetMiningPlugin.getInstance().getPetManager().getPetSummoner().startPetMovement(pet, pet.getOwner());

                    cancel();
                    return;
                }

                if (pet.getEntity() == null || pet.getEntity().isDead()) {
                    Bukkit.getLogger().warning("[DEBUG] ❌ Pet " + pet.getName() + " stracił encję! Przerywam atak.");
                    pet.setTargetBlock(null);
                    actualPetMap.remove(pet);
                    cancel();
                    return;
                }

                // 📌 ANIMACJA ATAKU
                Location currentLoc = pet.getEntity().getLocation();
                double movementOffset = forward ? -0.15 : 0.15;
                pet.getEntity().teleport(currentLoc.add(0, 0, movementOffset));
                forward = !forward;

                attackCounter++;
                if (attackCounter % 2 == 0) { // 📌 Atak raz na sekundę
                    coinBlock.damage(pet.getPower(), pet.getOwner());
                    coinBlock.playDamageEffect();

                    if (coinBlock.getHealthBar() != null) {
                        coinBlock.getHealthBar().updateHP(coinBlock.getCurrentHealth(), coinBlock.getMaxHealth());
                    } else {
                        Bukkit.getLogger().warning("[DEBUG] ⚠️ HealthBar został usunięty, nie można go zaktualizować!");
                    }

                    Bukkit.getLogger().info("[DEBUG] ⚔️ Pet " + pet.getName() + " uderza blok za " + pet.getPower() + " obrażeń!");
                }
            }
        }.runTaskTimer(PetMiningPlugin.getInstance(), 0L, 20L);
    }
}
