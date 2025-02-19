package org.isenzo.petPlugin.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class SkullUtil {

    public static ItemStack getCustomHead(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta == null) return skull;

        PlayerProfile profile = createProfile(base64);
        skullMeta.setOwnerProfile(profile);

        skull.setItemMeta(skullMeta);
        return skull;
    }

    private static PlayerProfile createProfile(String base64) {
        PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        try {
            URL url = new URL("http://textures.minecraft.net/texture/" + base64);
            textures.setSkin(url);
            profile.setTextures(textures);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return profile;
    }
}
