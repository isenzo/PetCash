package org.isenzo.petPlugin.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.managers.CoinBlockManager;
import org.isenzo.petPlugin.utils.CoinItemUtil;

import java.util.List;
import java.util.Objects;

import static org.isenzo.petPlugin.utils.CoinItemUtil.isCoinSword;

public class MiningListener implements Listener {

    private final CoinBlockManager coinBlockManager;

    public MiningListener() {
        this.coinBlockManager = PetMiningPlugin.getInstance().getCoinBlockManager();
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();


        if (event.getBlock().getType() != Material.GOLD_BLOCK) {
            return;
        }

        if (coinBlockManager.isCoinBlock(block.getLocation())) {

            if(!isCoinSword(player.getInventory().getItemInMainHand())) {
                player.sendMessage(ChatColor.RED + "Potrzebujesz specjalnego miecza do niszczenia CoinBlocków!");
                return;
            }

            event.setCancelled(true);
            Bukkit.getLogger().info("[DEBUG] <MiningListener.java> Gracz " + player.getName() + " uderza CoinBlock i zadaje obrażenia!");
            coinBlockManager.handleBlockDamage(block, player);
        }
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        Location blockLocation = block.getLocation();
        Player player = event.getPlayer();

        if (!coinBlockManager.isCoinBlock(blockLocation)) {
            return;
        }

        if (Objects.nonNull(coinBlockManager.getCoinBlock(blockLocation))) {
            Bukkit.getLogger().info("[DEBUG] <MiningListener.java> Blok w lokalizacji {" + blockLocation + " } to CoinBlock");
            handleDestroyingActionOnCoinBlockDependsWhatButtonIsClicked(event, block, player);
        }
    }

    private void handleDestroyingActionOnCoinBlockDependsWhatButtonIsClicked(PlayerInteractEvent event, Block block, Player player) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        ItemStack itemInSecondHand = player.getInventory().getItemInMainHand();

        Bukkit.getLogger().info("[DEBUG] <MiningListener.java> Gracz " + player.getName() + " używa przedmiotu: "
                + (itemInMainHand.hasItemMeta() ? itemInMainHand.getItemMeta().getDisplayName() : itemInMainHand.getType()));

        if ((isCoinBlockRemover(itemInMainHand) || isCoinBlockRemover(itemInSecondHand)) && player.isSneaking() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            coinBlockManager.removeCoinBlock(block.getLocation());
            player.sendMessage(ChatColor.GREEN + "§4◈ §2Usunięto CoinBlock!");
            return;
        }

        if (!CoinItemUtil.isCoinSword(itemInMainHand) && !CoinItemUtil.isCoinSword(itemInSecondHand)) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Bukkit.getLogger().info("[DEBUG] <MiningListener.java> Gracz uderza CoinBlock LPM.");
            coinBlockManager.handleBlockDamage(block, player);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            boolean isPlayerSneaking = player.isSneaking();
            Bukkit.getLogger().info("[DEBUG] <MiningListener.java> Gracz używa PPM, pet atakuje blok.");
            coinBlockManager.handlePetAttack(player, block, isPlayerSneaking);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Block block = event.getBlock();
        Location location = block.getLocation();

        if (item.getType() != Material.GOLD_BLOCK) {
            return;
        }

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        if (!item.getItemMeta().getDisplayName().startsWith(ChatColor.GOLD + "CoinBlock")) {
            return;
        }

        double hp = 100;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore != null && lore.size() > 1) {
            String hpLine = ChatColor.stripColor(lore.get(1)).replace("HP: ", "").trim();
            try {
                hp = Double.parseDouble(hpLine);
            } catch (NumberFormatException ignored) {}
        }

        block.setType(Material.GOLD_BLOCK);
        coinBlockManager.addCoinBlock(location, hp, player);

        player.sendMessage(ChatColor.GREEN + "✔ Postawiłeś CoinBlock z HP = " + hp + "!");
        Bukkit.getLogger().info("[DEBUG] <MiningListener.java> Dodano CoinBlock: " + location);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (coinBlockManager.isCoinBlock(block.getLocation())) {
            if (player.getGameMode() == GameMode.CREATIVE || player.breakBlock(block)) {
                event.setCancelled(true);
                return;
            }

            if (isCoinBlockRemover(player.getInventory().getItemInMainHand()) && player.isSneaking()) {
                coinBlockManager.removeCoinBlock(block.getLocation());
                player.sendMessage(ChatColor.GREEN + "✅ Usunięto CoinBlock!");
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "§7▶ §eCoin Bloki §cmożna niszczyć tylko przy pomocy §dMagiczna flet");
            }
        }
    }

    private boolean isCoinBlockRemover(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        return item.getItemMeta().getDisplayName().equals(ChatColor.RED + "CoinBlock Remover");
    }
}