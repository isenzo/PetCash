package org.isenzo.petPlugin.managers;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.logging.Level;

@Getter
public class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.saveResource("config.yml", false);
                plugin.getLogger().info("Plik config.yml został utworzony.");
            }
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("Plik config.yml został wczytany poprawnie.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas ładowania config.yml!", e);
        }
    }

    // 📌 DODAJ TĘ METODĘ, ABY ZWRÓCIĆ KONFIGURACJĘ!
    public FileConfiguration getConfig() {
        return this.config;
    }

    public String getMongoUri() {
        return config.getString("mongodb.uri", "mongodb://localhost:27017");
    }

    public String getMongoDatabase() {
        return config.getString("mongodb.database", "PetPlug");
    }
}
