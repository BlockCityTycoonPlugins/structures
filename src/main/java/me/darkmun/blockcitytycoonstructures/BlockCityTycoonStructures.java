package me.darkmun.blockcitytycoonstructures;

import com.comphenix.packetwrapper.WrapperPlayServerMapChunk;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.io.NbtTextSerializer;
import me.darkmun.blockcitytycoonstructures.commands.ChangeStructureCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public final class BlockCityTycoonStructures extends JavaPlugin {

    private CustomConfig playerUpgradesConfig;
    private CustomConfig chunkDataConfig;
    private Database database;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();

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

            //chunkDataConfig.setup("chunkData");
            //chunkDataConfig.getConfig().options().copyDefaults(true);




            getCommand("chunkchange").setExecutor(new ChangeStructureCommand(this));

            ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.MAP_CHUNK) {
                final NbtTextSerializer serializer = new NbtTextSerializer();

                @Override
                public void onPacketSending(PacketEvent event) {
                    PacketContainer packet = event.getPacket();
                    WrapperPlayServerMapChunk wrapper = new WrapperPlayServerMapChunk(packet);

                    Player pl = event.getPlayer();
                    int ChunkX = packet.getIntegers().read(0);
                    int ChunkZ = packet.getIntegers().read(1);

                    //fillConfigWithChunkData(chunkDataConfig, wrapper, ChunkX, ChunkZ);

                    try {
                        changeChunkToPlayer(playerUpgradesConfig, chunkDataConfig, wrapper, ChunkX, ChunkZ, pl);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                public void fillConfigWithChunkData(CustomConfig chunkDataConfig, WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ) {
                    Set<String> businessesChunks = getConfig().getKeys(false);
                    businessesChunks.remove("enable");
                    Set<String> businessChunkUpgrades;
                    Set<String> businessChunkUpgradeChunks;
                    byte[] data;
                    int bitMask;
                    boolean groundUp;
                    List<NbtBase<?>> tileEntities;
                    List<String> configTileEntities = new ArrayList<>();

                    for (String businessChunk : businessesChunks) {
                        businessChunkUpgrades = getConfig().getConfigurationSection(businessChunk).getKeys(false);
                        for (String businessChunkUpgrade : businessChunkUpgrades) {
                            businessChunkUpgradeChunks = getConfig().getConfigurationSection(businessChunk + "." + businessChunkUpgrade).getKeys(false);
                            for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                                int businessUpgradeChunkX = getConfig().getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                                int businessUpgradeChunkZ = getConfig().getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                                if ((businessUpgradeChunkX == ChunkX) && (businessUpgradeChunkZ == ChunkZ) && !chunkDataConfig.getConfig().contains(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk)) {
                                    data = packet.getData();
                                    bitMask = packet.getBitmask();
                                    groundUp = packet.getGroundUpContinuous();
                                    tileEntities = packet.getTileEntities();
                                    if (!tileEntities.isEmpty()) {
                                        for (NbtBase<?> tileEntity : tileEntities) {
                                            configTileEntities.add(serializer.serialize(tileEntity));
                                        }
                                    }
                                    //chunkDataConfig.getConfig().set(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".data", data);
                                    //chunkDataConfig.getConfig().set(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".bit-mask", bitMask);
                                    //chunkDataConfig.getConfig().set(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".ground-up-continuous", groundUp);
                                    //chunkDataConfig.getConfig().set(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".tile-entities", configTileEntities);

                                    //chunkDataConfig.saveConfig();
                                    return;
                                }
                            }
                        }
                    }
                }

                public void changeChunkToPlayer(CustomConfig playerUpgradesConfig, CustomConfig chunkDataConfig, WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ, Player player) throws IOException {
                    Set<String> businesses = getConfig().getKeys(false);
                    Set<String> businessChunkUpgradeChunks;
                    String businessValue;
                    String plUUID = player.getUniqueId().toString();

                    byte[] data;
                    int bitMask;
                    boolean groundUp;
                    List<NbtBase<?>> tileEntities = new ArrayList<>();

                    int pasteChunkX;
                    int pasteChunkZ;

                    for (String business : businesses) {
                        if (playerUpgradesConfig.getConfig().contains(plUUID + "." + business)) {
                            businessValue = playerUpgradesConfig.getConfig().getString(plUUID + "." + business);
                            if (chunkDataConfig.getConfig().contains(business + "." + businessValue)) {
                                businessChunkUpgradeChunks = chunkDataConfig.getConfig().getConfigurationSection(business + "." + businessValue).getKeys(false);
                                for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                                    pasteChunkX = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                                    pasteChunkZ = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                                    if ((ChunkX == pasteChunkX) && (ChunkZ == pasteChunkZ)) {
                                        data = (byte []) chunkDataConfig.getConfig().get(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".data");
                                        bitMask = chunkDataConfig.getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".bit-mask");
                                        groundUp = chunkDataConfig.getConfig().getBoolean(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".ground-up-continuous");

                                        List<String> configTileEntities = chunkDataConfig.getConfig().getStringList(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".tile-entities");
                                        for (String tileEntity : configTileEntities) {
                                            tileEntities.add(serializer.deserialize(tileEntity));
                                        }
                                        /*int i = 0;
                                        while(chunkDataConfig.getConfig().contains(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".tile-entities-" + i)) {

                                            String str = chunkDataConfig.getConfig().getString(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".tile-entities-" + i);
                                            if (str != null && !str.equals("") && !str.equals(" ")) {
                                                tileEntities.add(serializer.deserialize(str));
                                            }
                                            i++;
                                        }*/


                                        packet.setData(data);
                                        packet.setBitmask(bitMask);
                                        packet.setGroundUpContinuous(groundUp);
                                        packet.setTileEntities(tileEntities);
                                        return;
                                    }
                                }

                            }
                        }

                    }
                }
            });

            getLogger().log(Level.INFO, "Plugin enabled.");
        }
        else {
            getLogger().log(Level.INFO, "Plugin not enabled.");
        }

    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Plugin disabled.");
    }

    public CustomConfig getPlayerUpgradesConfig() { return playerUpgradesConfig; }
    public CustomConfig getChunkDataConfig() { return chunkDataConfig; }
}
