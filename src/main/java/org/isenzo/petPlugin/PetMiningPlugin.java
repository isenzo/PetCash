package org.isenzo.petPlugin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.isenzo.petPlugin.commands.CoinBlockCommand;
import org.isenzo.petPlugin.commands.CoinSwordCommand;
import org.isenzo.petPlugin.commands.PetCommand;
import org.isenzo.petPlugin.listeners.*;
import org.isenzo.petPlugin.managers.CoinBlockManager;
import org.isenzo.petPlugin.managers.ConfigManager;
import org.isenzo.petPlugin.managers.PetManager;
import org.isenzo.petPlugin.managers.PetSummoner;

import java.util.Objects;

@Getter
public class PetMiningPlugin extends JavaPlugin {
    private static PetMiningPlugin instance;
    private MongoDatabase database;
    private MongoClient mongoClient;
    private FileConfiguration config;
    private ConfigManager configManager;
    private PetManager petManager;
    private CoinBlockManager coinBlockManager;
    private PetSummoner petSummoner;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.config = configManager.getConfig();

        if (this.config == null) {
            getLogger().severe("Błąd: Nie udało się wczytać config.yml! Wyłączanie pluginu.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Plik config.yml został poprawnie załadowany.");

        String mongoUri = configManager.getMongoUri();
        String dbName = configManager.getMongoDatabase();

        try {
            mongoClient = MongoClients.create(mongoUri);
            database = mongoClient.getDatabase(dbName);
            getLogger().info("Successfully connected to MongoDB!");
        } catch (Exception e) {
            getLogger().severe("Failed to connect to MongoDB: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (database == null) {
            getLogger().severe("MongoDB database is null. Plugin is shutting down.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.petSummoner = new PetSummoner();
        this.petManager = new PetManager(petSummoner);
        this.petSummoner.setPetManager(petManager);
        this.coinBlockManager = new CoinBlockManager();

        if (getCommand("pet") != null) {
            Objects.requireNonNull(getCommand("pet")).setExecutor(new PetCommand());
        } else {
            getLogger().severe("Command 'pet' is missing in plugin.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getCommand("coinblock") != null) {
            getCommand("coinblock").setExecutor(new CoinBlockCommand());
        }

        if (getCommand("coinsword") != null) {
            getCommand("coinsword").setExecutor(new CoinSwordCommand());
        }

        getServer().getPluginManager().registerEvents(new PetListener(), this);
        getServer().getPluginManager().registerEvents(new MiningListener(), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(petManager, petSummoner), this);
        getServer().getPluginManager().registerEvents(new PetGuiListener(petManager, petSummoner), this);

        getLogger().info("PetMining plugin has been enabled!");
    }


    @Override
    public void onDisable() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                getLogger().info("Closed MongoDB connection.");
            } catch (Exception e) {
                getLogger().warning("Error closing MongoDB connection: " + e.getMessage());
            }
        }
    }

    public static PetMiningPlugin getInstance() {
        return instance;
    }
}
