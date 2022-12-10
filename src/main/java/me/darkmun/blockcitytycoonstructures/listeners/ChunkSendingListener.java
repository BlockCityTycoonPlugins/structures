package me.darkmun.blockcitytycoonstructures.listeners;

import com.comphenix.packetwrapper.WrapperPlayServerMapChunk;
import com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityPainting;
import com.comphenix.packetwrapper.WrapperPlayServerUnloadChunk;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import io.netty.buffer.Unpooled;
import me.darkmun.blockcitytycoonstructures.BlockCityTycoonStructures;
import me.darkmun.blockcitytycoonstructures.ChunkDataSerializer;
import me.darkmun.blockcitytycoonstructures.CustomConfig;
import me.darkmun.blockcitytycoonstructures.NbtTagCompoundSerializer;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ChunkSendingListener extends PacketAdapter {

    public static final int CHUNK_WIDTH = 16;
    private static int entityCount = 0;
    private final FileConfiguration BCTSconfig = BlockCityTycoonStructures.getPlugin().getConfig();

    public ChunkSendingListener(Plugin plugin, PacketType... types) {
        super(plugin, types);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        WrapperPlayServerMapChunk wrapper = new WrapperPlayServerMapChunk(packet);

        Player pl = event.getPlayer();
        int ChunkX = packet.getIntegers().read(0);
        int ChunkZ = packet.getIntegers().read(1);
                    /*for (Entity painting : Bukkit.getWorld("world").getEntities()) {
                        NbtFactory.fromNMSCompound().
                        NBTTagCompound asd = new NBTTagCompound();
                        asd.
                        ((Painting)painting).
                        Bukkit.getLogger().info("entity: " + painting.getType());
                    }*/
        fillConfigWithChunkData(wrapper, ChunkX, ChunkZ, pl.getWorld());

        try {
            changeChunkToPlayer(BlockCityTycoonStructures.getPlayerUpgradesConfig(), wrapper, ChunkX, ChunkZ, pl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fillConfigWithChunkData(WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ, World world) {
        Set<String> businessesChunks = BCTSconfig.getKeys(false);
        businessesChunks.remove("enable");
        Set<String> businessChunkUpgrades;
        Set<String> businessChunkUpgradeChunks;
        byte[] data;
        int bitMask;
        boolean groundUp;
        List<NbtBase<?>> tileEntities;
        List<String> configTileEntities = new ArrayList<>();

        for (String businessChunk : businessesChunks) {
            businessChunkUpgrades = BCTSconfig.getConfigurationSection(businessChunk).getKeys(false);
            for (String businessChunkUpgrade : businessChunkUpgrades) {
                businessChunkUpgradeChunks = BCTSconfig.getConfigurationSection(businessChunk + "." + businessChunkUpgrade).getKeys(false);
                for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                    int businessUpgradeChunkX = BCTSconfig.getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                    int businessUpgradeChunkZ = BCTSconfig.getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                    if ((businessUpgradeChunkX == ChunkX) && (businessUpgradeChunkZ == ChunkZ) /*&& !chunkDataConfig.getConfig().contains(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk)*/) {
                        data = packet.getData();
                        bitMask = packet.getBitmask();
                        groundUp = packet.getGroundUpContinuous();
                        tileEntities = packet.getTileEntities();

                        try {
                            PreparedStatement statement = BlockCityTycoonStructures.getDatabase().getConnection()
                                    .prepareStatement("INSERT INTO chunk_data (structure,upgrade,chunk,data,bit_mask,ground_up_continuous,tile_entities,paintings,item_frames) " +
                                            "SELECT * FROM (SELECT ? AS structure, ? AS upgrade, ? AS chunk, ? AS data, ? AS bit_mask, ? AS ground_up_continuous, ? AS tile_entities, ? AS paintings, ? AS item_frames) AS temp " +
                                            "WHERE NOT EXISTS ( " +
                                            "    SELECT structure, upgrade, chunk FROM chunk_data WHERE structure = ? AND upgrade = ? AND chunk = ? " +
                                            ") LIMIT 1");
                            statement.setString(1, businessChunk);
                            statement.setString(2, businessChunkUpgrade);
                            statement.setString(3, businessChunkUpgradeChunk);
                            statement.setBytes(4, data);
                            statement.setInt(5, bitMask);
                            statement.setBoolean(6, groundUp);

                            NbtBinarySerializer nbtSerializer = new NbtBinarySerializer();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            DataOutputStream out = new DataOutputStream(baos);
                            for (NbtBase<?> tileEntity : tileEntities) {
                                nbtSerializer.serialize(tileEntity, out);
                            }
                            byte[] bytes = baos.toByteArray();
                            statement.setBytes(7, bytes);

                            ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                            DataOutputStream out1 = new DataOutputStream(baos1);
                            serializePaintingsFromChunk(world, businessUpgradeChunkX, businessUpgradeChunkZ, out1);
                            if (baos1.toByteArray().length == 0) {
                                Bukkit.getLogger().info("Empty paintings");
                            } else {
                                Bukkit.getLogger().info("Not empty paintings");
                            }
                            statement.setBytes(8, baos1.toByteArray());

                            statement.setBytes(9, null);
                            statement.setString(10, businessChunk);
                            statement.setString(11, businessChunkUpgrade);
                            statement.setString(12, businessChunkUpgradeChunk);

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

    public void changeChunkToPlayer(CustomConfig playerUpgradesConfig, WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ, Player player) throws IOException {
        Set<String> businesses = BCTSconfig.getKeys(false);
        Set<String> businessChunkUpgradeChunks;
        String businessValue;
        String plUUID = player.getUniqueId().toString();

        byte[] data;
        int bitMask;
        boolean groundUp;

        int pasteChunkX;
        int pasteChunkZ;

        for (String business : businesses) {
            if (playerUpgradesConfig.getConfig().contains(plUUID + "." + business)) {
                businessValue = playerUpgradesConfig.getConfig().getString(plUUID + "." + business);
                //if (chunkDataConfig.getConfig().contains(business + "." + businessValue)) {
                businessChunkUpgradeChunks = BCTSconfig.getConfigurationSection(business + "." + businessValue).getKeys(false);
                for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                    pasteChunkX = BCTSconfig.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                    pasteChunkZ = BCTSconfig.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                    if ((ChunkX == pasteChunkX) && (ChunkZ == pasteChunkZ)) {
                        try {
                            PreparedStatement statement = BlockCityTycoonStructures.getDatabase().getConnection().prepareStatement("SELECT * FROM chunk_data WHERE structure = ? AND upgrade = ? AND chunk = ?");
                            statement.setString(1, business);
                            statement.setString(2, businessValue);
                            statement.setString(3, businessChunkUpgradeChunk);

                            ResultSet set = statement.executeQuery();
                            if (set.next()) {
                                data = set.getBytes("data");
                                bitMask = set.getInt("bit_mask");
                                groundUp = set.getBoolean("ground_up_continuous");
                                byte[] tileEntitiesBytes = set.getBytes("tile_entities");
                                byte[] paintingBytes = set.getBytes("paintings");


                                List<NbtBase<?>> tileEntities = new ArrayList<>();
                                NbtBinarySerializer nbtSerializer = new NbtBinarySerializer();
                                ByteArrayInputStream bais = new ByteArrayInputStream(tileEntitiesBytes);
                                DataInputStream in = new DataInputStream(bais);
                                while (in.available() > 0) {
                                    tileEntities.add(nbtSerializer.deserialize(in));
                                }

                                int copyChunkX = BCTSconfig.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                                int copyChunkZ = BCTSconfig.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                                int chunkXDelta = pasteChunkX - copyChunkX;
                                int chunkZDelta = pasteChunkZ - copyChunkZ;

                                for (NbtBase<?> tileEntity : tileEntities) {
                                    Map<String, NbtBase<?>> map = (Map<String, NbtBase<?>>) tileEntity.getValue();
                                    int x = (int) map.get("x").getValue();
                                    int z = (int) map.get("z").getValue();
                                    map.put("x", NbtFactory.of("x", x + CHUNK_WIDTH * chunkXDelta));
                                    map.put("z", NbtFactory.of("z", z + CHUNK_WIDTH * chunkZDelta));

                                }

                                                /*for (Painting painting : player.getWorld().getEntitiesByClass(Painting.class)) {
                                                    int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                                                    int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                                                    if (paintingChunkX == copyChunkX && paintingChunkZ == copyChunkZ) {
                                                        double paintingX = painting.getLocation().getX();
                                                        double paintingY = painting.getLocation().getY();
                                                        double paintingZ = painting.getLocation().getZ();
                                                        Painting newPainting = clonePaintingEntityToAnotherLocation(painting, new Location(painting.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta));
                                                        for (Player pl : newPainting.getWorld().getPlayers()) {
                                                            if (pl != player) {
                                                                entityHider.hideEntity(pl, newPainting);
                                                            }
                                                        }
                                                    }
                                                }*/
                                ByteArrayInputStream bais1 = new ByteArrayInputStream(paintingBytes);
                                List<NBTTagCompound> paintings = deserializePaintings(player.getWorld(), bais1);
                                for (NBTTagCompound painting : paintings) {
                                    double paintingX = painting.getInt("TileX");
                                    double paintingY = painting.getInt("TileY");
                                    double paintingZ = painting.getInt("TileZ");
                                    Bukkit.getLogger().info("Painting X: " + paintingX + " Painting Y: " + paintingY + " Painting Z: " + paintingZ);
                                    Location newLocation = new Location(player.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta);
                                    Bukkit.getLogger().info("Painting X (new): " + (paintingX + CHUNK_WIDTH * chunkXDelta) + " Painting Y (new): " + paintingY + " Painting Z (new): " + (paintingZ + CHUNK_WIDTH * chunkZDelta));
                                    sendPaintingFromNBT(painting, newLocation, player);
                                }

                                Chunk chunk;
                                if (groundUp) {
                                    chunk = new Chunk(((CraftPlayer)player).getHandle().getWorld(), ChunkX, ChunkZ);
                                } else {
                                    chunk = ((CraftPlayer)player).getHandle().getWorld().getChunkAt(ChunkX, ChunkZ);
                                }
                                ChunkDataSerializer chunkDataSerializer = new ChunkDataSerializer();
                                chunkDataSerializer.ReadChunkColumn(chunk, bitMask, Unpooled.wrappedBuffer(data));
                                PacketPlayOutMapChunk packetMapChunk = new PacketPlayOutMapChunk(chunk, 65535);

                                byte[] bytes;
                                Field dataField;
                                try {
                                    dataField = packetMapChunk.getClass().getDeclaredField("d");
                                    dataField.setAccessible(true);
                                    bytes = (byte[])dataField.get(packetMapChunk);
                                    dataField.setAccessible(false);

                                    boolean[] bools = new boolean[data.length];
                                    for (int i = 0; i < data.length; i++) {
                                        bools[i] = data[i] == bytes[i];
                                    }
                                    //Bukkit.getLogger().info(Arrays.toString(bools));

                                    WrapperPlayServerUnloadChunk wrapperUnloadChunk = new WrapperPlayServerUnloadChunk();
                                    wrapperUnloadChunk.setChunkX(ChunkX);
                                    wrapperUnloadChunk.setChunkZ(ChunkZ);
                                    wrapperUnloadChunk.sendPacket(player);

                                    packet.setData(bytes);
                                    //packet.setData(data);
                                    packet.setBitmask(bitMask);
                                    packet.setGroundUpContinuous(groundUp);
                                    packet.setTileEntities(tileEntities);
                                } catch (NoSuchFieldException | SecurityException | IllegalAccessException e){
                                    e.printStackTrace();
                                }
                            }

                        } catch (SQLException | NoSuchFieldException | InvocationTargetException |
                                 IllegalAccessException | NoSuchMethodException | MojangsonParseException ex) {
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

    private void sendPaintingFromNBT(NBTTagCompound compound, Location location, Player pl) {
        WrapperPlayServerSpawnEntityPainting wrapper = new WrapperPlayServerSpawnEntityPainting();
        if (entityCount == Integer.MIN_VALUE) {
            entityCount = 0;
        }
        Bukkit.getLogger().info("old count: " + entityCount);
        wrapper.setEntityID(--entityCount);
        Bukkit.getLogger().info("new count: " + entityCount);
        wrapper.getHandle().getStrings().write(0, compound.getString("Motive"));
        wrapper.setDirection(EnumWrappers.Direction.valueOf(EnumDirection.fromType2(compound.getByte("Facing")).toString().toUpperCase()));
        wrapper.setLocation(new BlockPosition(location.toVector()));
        wrapper.sendPacket(pl);
    }

    private void serializePaintingsFromChunk(World world, int chunkX, int chunkZ, DataOutput destination) throws IOException {
        for (Painting painting : world.getEntitiesByClass(Painting.class)) {
                int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                if (paintingChunkX == chunkX && paintingChunkZ == chunkZ) {
                    NbtTagCompoundSerializer.serialize(painting, destination);
                }
        }
    }

    private List<NBTTagCompound> deserializePaintings(World world, InputStream source) throws IOException, MojangsonParseException {
        List<NBTTagCompound> paintings = new ArrayList<>();
        DataInputStream in = new DataInputStream(source);
        while (source.available() > 0) {
            paintings.add(NbtTagCompoundSerializer.deserialize(in, world));
        }
        return paintings;
    }

    public static int getEntityCount() {
        return entityCount;
    }
}
