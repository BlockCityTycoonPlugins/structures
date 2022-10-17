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
import me.darkmun.blockcitytycoonstructures.CustomConfig;
import me.darkmun.blockcitytycoonstructures.listeners.JoinListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public final class BlockCityTycoonStructures extends JavaPlugin {

    private CustomConfig playerUpgradesConfig;
    private CustomConfig chunkDataConfig;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        playerUpgradesConfig = new CustomConfig();
        chunkDataConfig = new CustomConfig();

        playerUpgradesConfig.setup("playersUpgrades");
        playerUpgradesConfig.getConfig().options().copyDefaults(true);
        playerUpgradesConfig.saveConfig();

        chunkDataConfig.setup("chunkData");
        chunkDataConfig.getConfig().options().copyDefaults(true);
        chunkDataConfig.saveConfig();



        getCommand("chunkchange").setExecutor(new ChangeStructureCommand(this));
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.MAP_CHUNK) {

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
                    throw new RuntimeException(e);
                }
            }

            public void fillConfigWithChunkData(CustomConfig config, WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ) {
                Set<String> businessesChunks = getConfig().getKeys(false);
                Set<String> businessChunkUpgrades;
                Set<String> businessChunkUpgradeChunks;
                byte[] data;
                int bitMask;
                boolean groundUp;
                List<NbtBase<?>> tileEntities;

                for (String businessChunk : businessesChunks) {
                    businessChunkUpgrades = getConfig().getConfigurationSection(businessChunk).getKeys(false);
                    for (String businessChunkUpgrade : businessChunkUpgrades) {
                        businessChunkUpgradeChunks = getConfig().getConfigurationSection(businessChunk + "." + businessChunkUpgrade).getKeys(false);
                        for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                            int businessUpgradeChunkX = getConfig().getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                            int businessUpgradeChunkZ = getConfig().getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                            if ((businessUpgradeChunkX == ChunkX) && (businessUpgradeChunkZ == ChunkZ)) {
                                data = packet.getData();
                                bitMask = packet.getBitmask();
                                groundUp = packet.getGroundUpContinuous();
                                tileEntities = packet.getTileEntities();
                                String hey = null;
                                if (!tileEntities.isEmpty()) {
                                    NbtTextSerializer serializer = new NbtTextSerializer();
                                    int i = 0;
                                    for (NbtBase<?> tileEntity : tileEntities) {
                                        hey = serializer.serialize(tileEntity);
                                        config.getConfig().set(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".tile-entities-" + i, hey);
                                        i++;
                                    }
                                }
                                config.getConfig().set(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".data", data);
                                config.getConfig().set(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".bit-mask", bitMask);
                                config.getConfig().set(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".ground-up-continuous", groundUp);

                                config.saveConfig();
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
                    businessValue = playerUpgradesConfig.getConfig().getString(plUUID + "." + business);
                    if (!businessValue.startsWith("0") && chunkDataConfig.getConfig().contains(business + "." + businessValue)) {
                        businessChunkUpgradeChunks = chunkDataConfig.getConfig().getConfigurationSection(business + "." + businessValue).getKeys(false);
                        for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                            pasteChunkX = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                            pasteChunkZ = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                            if ((ChunkX == pasteChunkX) && (ChunkZ == pasteChunkZ)) {
                                data = (byte []) chunkDataConfig.getConfig().get(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".data");
                                bitMask = chunkDataConfig.getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".bit-mask");
                                groundUp = chunkDataConfig.getConfig().getBoolean(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".ground-up-continuous");
                                NbtTextSerializer serializer = new NbtTextSerializer();

                                int i = 0;
                                while(chunkDataConfig.getConfig().contains(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".tile-entities-" + i)) {

                                    String str = chunkDataConfig.getConfig().getString(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".tile-entities-" + i);
                                    if (str != null && !str.equals("") && !str.equals(" ")) {
                                        tileEntities.add(serializer.deserialize(str));
                                    }
                                    i++;
                                }


                                packet.setData(data);
                                packet.setBitmask(bitMask);
                                packet.setGroundUpContinuous(groundUp);
                                packet.setTileEntities(tileEntities);
                            }
                        }

                    }
                }
            }
        });

        getLogger().log(Level.INFO, "Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Plugin disabled.");
    }

    public CustomConfig getPlayerUpgradesConfig() { return playerUpgradesConfig; }
    public CustomConfig getChunkDataConfig() { return chunkDataConfig; }
}
