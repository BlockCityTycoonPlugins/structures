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
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

                    fillConfigWithChunkData(chunkDataConfig, wrapper, ChunkX, ChunkZ);

                    try {
                        changeChunkToPlayer(playerUpgradesConfig, chunkDataConfig, wrapper, ChunkX, ChunkZ, pl);
                    } catch (IOException e) {
                        e.printStackTrace();
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
                                if ((businessUpgradeChunkX == ChunkX) && (businessUpgradeChunkZ == ChunkZ) /*&& !chunkDataConfig.getConfig().contains(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk)*/) {
                                    data = packet.getData();
                                    bitMask = packet.getBitmask();
                                    groundUp = packet.getGroundUpContinuous();
                                    tileEntities = packet.getTileEntities();

                                    if (!tileEntities.isEmpty()) {
                                        for (NbtBase<?> tileEntity : tileEntities) {
                                            configTileEntities.add(serializer.serialize(tileEntity));
                                        }
                                    }

                                    try {
                                        PreparedStatement statement = database.getConnection()
                                                .prepareStatement("INSERT INTO chunk_data (structure,upgrade,chunk,data,bit_mask,ground_up_continuous,tile_entities) " +
                                                        "SELECT * FROM (SELECT ? AS structure, ? AS upgrade, ? AS chunk, ? AS data, ? AS bit_mask, ? AS ground_up_continuous, ? AS tile_entities) AS temp " +
                                                        "WHERE NOT EXISTS ( " +
                                                        "    SELECT structure, upgrade, chunk FROM chunk_data WHERE structure = ? AND upgrade = ? AND chunk = ? " +
                                                        ") LIMIT 1");
                                        statement.setString(1, businessChunk);
                                        statement.setString(2, businessChunkUpgrade);
                                        statement.setString(3, businessChunkUpgradeChunk);
                                        statement.setBytes(4, data);
                                        statement.setInt(5, bitMask);
                                        statement.setBoolean(6, groundUp);

                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        DataOutputStream out = new DataOutputStream(baos);
                                        for (String element : configTileEntities) {
                                            out.writeUTF(element);
                                        }
                                        byte[] bytes = baos.toByteArray();

                                        statement.setBytes(7, bytes);
                                        statement.setString(8, businessChunk);
                                        statement.setString(9, businessChunkUpgrade);
                                        statement.setString(10, businessChunkUpgradeChunk);

                                        statement.executeUpdate();

                                        statement.close();
                                    } catch (SQLException | IOException ex) {
                                        ex.printStackTrace();
                                    }
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
                            //if (chunkDataConfig.getConfig().contains(business + "." + businessValue)) {
                                businessChunkUpgradeChunks = getConfig().getConfigurationSection(business + "." + businessValue).getKeys(false);
                                for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                                    pasteChunkX = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                                    pasteChunkZ = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                                    if ((ChunkX == pasteChunkX) && (ChunkZ == pasteChunkZ)) {
                                        try {
                                            PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM chunk_data WHERE structure = ? AND upgrade = ? AND chunk = ?");
                                            statement.setString(1, business);
                                            statement.setString(2, businessValue);
                                            statement.setString(3, businessChunkUpgradeChunk);

                                            ResultSet set = statement.executeQuery();
                                            if (set.next()) {
                                                data = set.getBytes("data");
                                                bitMask = set.getInt("bit_mask");
                                                groundUp = set.getBoolean("ground_up_continuous");
                                                byte[] tileEntitiesBytes = set.getBytes("tile_entities");

                                                List<String> configTileEntities = new ArrayList<>();
                                                ByteArrayInputStream bais = new ByteArrayInputStream(tileEntitiesBytes);
                                                DataInputStream in = new DataInputStream(bais);
                                                while (in.available() > 0) {
                                                    String element = in.readUTF();
                                                    configTileEntities.add(element);
                                                }

                                                for (String tileEntity : configTileEntities) {
                                                    tileEntities.add(serializer.deserialize(tileEntity));
                                                }

                                                packet.setData(data);
                                                packet.setBitmask(bitMask);
                                                packet.setGroundUpContinuous(groundUp);
                                                packet.setTileEntities(tileEntities);
                                            }

                                        } catch (SQLException ex) {
                                            ex.printStackTrace();
                                        }

                                        /*data = (byte []) chunkDataConfig.getConfig().get(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".data");
                                        bitMask = chunkDataConfig.getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".bit-mask");
                                        groundUp = chunkDataConfig.getConfig().getBoolean(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".ground-up-continuous");

                                        List<String> configTileEntities = chunkDataConfig.getConfig().getStringList(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".tile-entities");
                                        for (String tileEntity : configTileEntities) {
                                            tileEntities.add(serializer.deserialize(tileEntity));
                                        }*/


                                        return;
                                    }
                                }

                            //}
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
