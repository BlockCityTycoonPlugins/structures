package me.darkmun.blockcitytycoonstructures;

import com.comphenix.packetwrapper.WrapperPlayServerMapChunk;
import com.comphenix.packetwrapper.WrapperPlayServerUnloadChunk;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import com.comphenix.protocol.wrappers.nbt.io.NbtTextSerializer;
import example.EntityHider;
import io.netty.buffer.Unpooled;
import me.darkmun.blockcitytycoonstructures.commands.ChangeStructureCommand;
import me.darkmun.blockcitytycoonstructures.commands.Test2Command;
import me.darkmun.blockcitytycoonstructures.listeners.ChunkSendingListener;
import me.darkmun.blockcitytycoonstructures.listeners.JoinListener;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public final class BlockCityTycoonStructures extends JavaPlugin {

    private static CustomConfig playerUpgradesConfig;
    private static CustomConfig chunkDataConfig;
    private static Database database;
    private static BlockCityTycoonStructures plugin;
    private static EntityHider entityHider;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        plugin = this;

        if (getConfig().getBoolean("enable")) {
            playerUpgradesConfig = new CustomConfig();
            chunkDataConfig = new CustomConfig();

            playerUpgradesConfig.setup("playersUpgrades");
            playerUpgradesConfig.getConfig().options().copyDefaults(true);

            try {
                database = new Database();
                database.initializeDatabase();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            entityHider = new EntityHider(this, EntityHider.Policy.BLACKLIST);
            ProtocolManager manager = ProtocolLibrary.getProtocolManager();

            getCommand("testspawn").setExecutor(new Test2Command());
            getCommand("chunkchange").setExecutor(new ChangeStructureCommand(this));
            getServer().getPluginManager().registerEvents(new JoinListener(), this);
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
        //removeAllPaintings(Bukkit.getWorld("world"));
        getLogger().log(Level.INFO, "Plugin disabled.");
    }

    /*public void spawnAllPaintings(World world) {
        Set<String> businessesChunks = getConfig().getKeys(false);
        businessesChunks.remove("enable");
        Set<String> businessChunkUpgrades;
        String lastUpgrade;
        Set<String> businessChunkUpgradeChunks;
        for (String businessChunk : businessesChunks) {
            businessChunkUpgrades = getConfig().getConfigurationSection(businessChunk).getKeys(false);
            lastUpgrade = (String) businessChunkUpgrades.toArray()[businessChunkUpgrades.size() - 1];
            businessChunkUpgradeChunks = getConfig().getConfigurationSection(businessChunk + "." + lastUpgrade).getKeys(false);
            for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                int copyChunkX = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                int copyChunkZ = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                int pasteChunkX = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                int pasteChunkZ = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                int chunkXDelta = pasteChunkX - copyChunkX;
                int chunkZDelta = pasteChunkZ - copyChunkZ;

                for (Entity painting : world.getEntities()) {
                    Bukkit.getLogger().info("entity: " + painting.getType());
                    if (painting instanceof Painting) {
                        Bukkit.getLogger().info("painting");
                        int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                        int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                        Bukkit.getLogger().info("paintingChunkX: " + paintingChunkX + " paintingChunkZ" + paintingChunkZ);
                        Bukkit.getLogger().info("painting name: " + painting.getName());
                        if (paintingChunkX == copyChunkX && paintingChunkZ == copyChunkZ) {
                            double paintingX = painting.getLocation().getX();
                            double paintingY = painting.getLocation().getY();
                            double paintingZ = painting.getLocation().getZ();
                            Painting newPainting = clonePaintingEntityToAnotherLocation((Painting) painting, new Location(painting.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta));
                        }
                    }
                }
            }
        }
    }

    public void removeAllPaintings(World world) {
        Set<String> businessesChunks = getConfig().getKeys(false);
        businessesChunks.remove("enable");
        Set<String> businessChunkUpgrades;
        String lastUpgrade;
        Set<String> businessChunkUpgradeChunks;
        for (String businessChunk : businessesChunks) {
            businessChunkUpgrades = getConfig().getConfigurationSection(businessChunk).getKeys(false);
            lastUpgrade = (String) businessChunkUpgrades.toArray()[businessChunkUpgrades.size() - 1];
            businessChunkUpgradeChunks = getConfig().getConfigurationSection(businessChunk + "." + lastUpgrade).getKeys(false);
            for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                //int copyChunkX = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                //int copyChunkZ = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                int pasteChunkX = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                int pasteChunkZ = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                //int chunkXDelta = pasteChunkX - copyChunkX;
                //int chunkZDelta = pasteChunkZ - copyChunkZ;

                for (Painting painting : world.getEntitiesByClass(Painting.class)) {
                    int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                    int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                    Bukkit.getLogger().info("paintingChunkX: " + paintingChunkX + " paintingChunkZ" + paintingChunkZ);
                    Bukkit.getLogger().info("painting name: " + painting.getName());
                    if (paintingChunkX == pasteChunkX && paintingChunkZ == pasteChunkZ) {
                        painting.remove();
                    }
                }
            }
        }
    }

    public Painting clonePaintingEntityToAnotherLocation(Painting painting, Location location) {
        Painting paintingClone = location.getWorld().spawn(location, Painting.class);
        paintingClone.setArt(painting.getArt());
        paintingClone.setFallDistance(painting.getFallDistance());
        paintingClone.setPortalCooldown(painting.getPortalCooldown());
        paintingClone.setFacingDirection(painting.getFacing());
        paintingClone.setVelocity(painting.getVelocity());
        Bukkit.getLogger().info("Painting clone name: " + paintingClone.getName());
        return paintingClone;
    }*/

    public static CustomConfig getPlayerUpgradesConfig() {
        return playerUpgradesConfig;
    }

    public static CustomConfig getChunkDataConfig() {
        return chunkDataConfig;
    }

    public static Database getDatabase() {
        return database;
    }

    public static EntityHider getEntityHider() {
        return entityHider;
    }

    public static BlockCityTycoonStructures getPlugin() {
        return plugin;
    }
}
