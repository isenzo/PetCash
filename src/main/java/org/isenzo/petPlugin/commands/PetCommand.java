package org.isenzo.petPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.isenzo.petPlugin.PetMiningPlugin;
import org.isenzo.petPlugin.gui.PetGUI;
import org.isenzo.petPlugin.gui.PetShopGUI;
import org.isenzo.petPlugin.managers.PetManager;
import org.isenzo.petPlugin.managers.PetSummoner;
import org.isenzo.petPlugin.models.Pet;

import java.util.List;
import java.util.Optional;

public class PetCommand implements CommandExecutor {

    private final PetManager petManager;
    private final PetSummoner petSummoner;
    private final PetGUI petGUI;

    public PetCommand() {
        this.petManager = PetMiningPlugin.getInstance().getPetManager();
        this.petSummoner = PetMiningPlugin.getInstance().getPetSummoner();
        this.petGUI = new PetGUI();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            petGUI.openPetMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "summon":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pet summon <petId>");
                    return true;
                }
                summonPet(player, args[1]);
                break;

            case "despawn":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pet despawn <petId>");
                    return true;
                }
                despawnPet(player, args[1]);
                break;

            case "shop":
                new PetShopGUI(player).open();
                break;

            case "list":
                listPets(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown command. Use /pet for the main menu.");
        }

        return true;
    }

    private void summonPet(Player player, String petId) {
        Optional<Pet> petOpt = petManager.getPlayerPets(player)
                .stream()
                .filter(p -> p.getId().equals(petId))
                .findFirst();

        if (!petOpt.isPresent()) {
            player.sendMessage(ChatColor.RED + "Error: You don't own a pet with this ID.");
            return;
        }

        Pet pet = petOpt.get();
        if (pet.isActive()) {
            player.sendMessage(ChatColor.RED + "This pet is already summoned!");
            return;
        }

        petManager.spawnPet(player, petId);
        petSummoner.summonPet(player, pet);
        player.sendMessage(ChatColor.GREEN + "You have summoned " + pet.getName() + "!");
        petSummoner.setPetName(pet);
    }

    private void despawnPet(Player player, String petId) {
        Optional<Pet> petOpt = petManager.getPlayerPets(player)
                .stream()
                .filter(p -> p.getId().equals(petId))
                .findFirst();

        if (!petOpt.isPresent()) {
            player.sendMessage(ChatColor.RED + "Error: You don't own a pet with this ID.");
            return;
        }

        Pet pet = petOpt.get();
        if (!pet.isActive()) {
            player.sendMessage(ChatColor.RED + "This pet is not currently summoned!");
            return;
        }

        petManager.despawnPet(player, petId);
        petSummoner.despawnPet(player, pet);
        player.sendMessage(ChatColor.YELLOW + "You have despawned " + pet.getName() + ".");
    }


    private void listPets(Player player) {
        List<Pet> pets = petManager.getPlayerPets(player);
        if (pets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You don't own any pets!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Your Pets:");
        for (Pet pet : pets) {
            player.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + pet.getName()
                    + ChatColor.GRAY + " (Lv. " + pet.getLevel() + ")"
                    + (pet.isActive() ? ChatColor.YELLOW + " [ACTIVE]" : ""));
        }
    }
}
