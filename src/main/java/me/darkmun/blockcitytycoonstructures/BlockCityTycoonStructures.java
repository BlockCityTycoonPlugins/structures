package me.darkmun.blockcitytycoonstructures;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.darkmun.blockcitytycoonstructures.commands.ChangeStructureCommand;
import me.darkmun.blockcitytycoonstructures.commands.ReloadCommand;
import me.darkmun.blockcitytycoonstructures.listeners.ChunkSendingListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public final class BlockCityTycoonStructures extends JavaPlugin {

    private static final CustomConfig playerUpgradesConfig = new CustomConfig();
    private static final CustomConfig BCTFPlayersFurnacesDataConfig = new CustomConfig(); //это из плагина с плавильней
    private static final CustomConfig BCTEPlayersEventsDataConfig = new CustomConfig();
    private static Database database;
    private static BlockCityTycoonStructures plugin;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        plugin = this;

        if (getConfig().getBoolean("enable")) {
            playerUpgradesConfig.setup(getDataFolder(), "playersUpgrades");
            playerUpgradesConfig.getConfig().options().copyDefaults(true);

            BCTFPlayersFurnacesDataConfig.setup(Bukkit.getPluginManager().getPlugin("BlockCityTycoonFoundry").getDataFolder(), "players-furnaces-data");

            BCTEPlayersEventsDataConfig.setup(Bukkit.getPluginManager().getPlugin("BlockCityTycoonEvents").getDataFolder(), "playerEventsData");

            try {
                database = new Database();
                database.initializeDatabase();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            ProtocolManager manager = ProtocolLibrary.getProtocolManager();

            //getCommand("testspawn").setExecutor(new Test2Command());
            getCommand("chunkchange").setExecutor(new ChangeStructureCommand(this));
            getCommand("bctstructures").setExecutor(new ReloadCommand());
            //getServer().getPluginManager().registerEvents(new JoinListener(), this);
            manager.addPacketListener(new ChunkSendingListener(this, PacketType.Play.Server.MAP_CHUNK));

            getLogger().log(Level.INFO, "Plugin enabled.");
        }
        else {
            getLogger().log(Level.INFO, "Plugin not enabled.");
        }

    }

    @Override
    public void onDisable() {
        database.closeConnection();
        getLogger().log(Level.INFO, "Plugin disabled.");
    }

    public static CustomConfig getPlayerUpgradesConfig() {
        return playerUpgradesConfig;
    }

    public static CustomConfig getBCTFPlayersFurnacesDataConfig() {
        return BCTFPlayersFurnacesDataConfig;
    }

    public static CustomConfig getBCTEPlayersEventsDataConfig() {
        return BCTEPlayersEventsDataConfig;
    }

    public static Database getDatabase() {
        return database;
    }

    public static BlockCityTycoonStructures getPlugin() {
        return plugin;
    }
}
