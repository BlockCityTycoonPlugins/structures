package me.darkmun.blockcitytycoonstructures.listeners;

import com.comphenix.packetwrapper.*;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import io.netty.buffer.Unpooled;
import me.darkmun.blockcitytycoonstructures.BlockCityTycoonStructures;
import me.darkmun.blockcitytycoonstructures.serializers.ChunkDataSerializer;
import me.darkmun.blockcitytycoonstructures.CustomConfig;
import me.darkmun.blockcitytycoonstructures.serializers.NbtTagCompoundSerializer;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.xml.crypto.Data;
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

                            ByteArrayOutputStream baosPaintings = new ByteArrayOutputStream();
                            DataOutputStream outPainting = new DataOutputStream(baosPaintings);
                            serializePaintingsFromChunk(world, businessUpgradeChunkX, businessUpgradeChunkZ, outPainting);
                            /*if (baosPaintings.toByteArray().length == 0) {
                                Bukkit.getLogger().info("Empty paintings");
                            } else {
                                Bukkit.getLogger().info("Not empty paintings");
                            }*/
                            statement.setBytes(8, baosPaintings.toByteArray());

                            ByteArrayOutputStream baosItemFrames = new ByteArrayOutputStream();
                            DataOutputStream outItemFrames = new DataOutputStream(baosItemFrames);
                            serializeItemFramesFromChunk(world, businessUpgradeChunkX, businessUpgradeChunkZ, outItemFrames);
                            /*if (baosItemFrames.toByteArray().length == 0) {
                                Bukkit.getLogger().info("Empty item frames");
                            } else {
                                Bukkit.getLogger().info("Not empty item frames");
                            }*/
                            statement.setBytes(9, baosItemFrames.toByteArray());

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
                                byte[] itemFrameBytes = set.getBytes("item_frames");


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
                                ByteArrayInputStream baisPaintings = new ByteArrayInputStream(paintingBytes);
                                List<NBTTagCompound> paintings = deserializeEntities(baisPaintings);
                                for (NBTTagCompound painting : paintings) {
                                    double paintingX = painting.getInt("TileX");
                                    double paintingY = painting.getInt("TileY");
                                    double paintingZ = painting.getInt("TileZ");
                                    Location newLocation = new Location(player.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta);
                                    sendPaintingFromNBT(painting, newLocation, player);
                                }

                                ByteArrayInputStream baisItemFrames = new ByteArrayInputStream(itemFrameBytes);
                                List<NBTTagCompound> itemFrames = deserializeEntities(baisItemFrames);
                                Bukkit.getLogger().info("Item frames size: " + itemFrames.size());
                                for (NBTTagCompound itemFrame : itemFrames) {
                                    NBTTagList nbttaglist = itemFrame.getList("Pos", 6);
                                    double paintingX = nbttaglist.f(0);
                                    double paintingY = nbttaglist.f(1);
                                    double paintingZ = nbttaglist.f(2);
                                    Bukkit.getLogger().info(String.format("X: %s Y: %s Z: %s", paintingX, paintingY, paintingZ));
                                    Location newLocation = new Location(player.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta);
                                    sendItemFrameFromNBT(itemFrame, newLocation, player);
                                    Bukkit.getLogger().info("End of loop");
                                }
                                Bukkit.getLogger().info("After loop");

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

    private void sendItemFrameFromNBT(NBTTagCompound compound, Location location, Player pl) {
        if (entityCount == Integer.MIN_VALUE) {
            entityCount = 0;
        }
        long start = System.nanoTime();
        WrapperPlayServerSpawnEntity wrapperObject = new WrapperPlayServerSpawnEntity();
        int entityID = --entityCount;
        /*EntityItemFrame entity = new EntityItemFrame(((CraftWorld)pl.getWorld()).getHandle());
        entity.f(compound);
        entity.a(compound);
        PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity(entity, 71, EnumDirection.fromType2(compound.getByte("Facing")).get2DRotationValue(), new net.minecraft.server.v1_12_R1.BlockPosition(location.getX(), location.getY(), location.getZ()));
        ((CraftPlayer)pl).getHandle().playerConnection.sendPacket(packet);*/
        wrapperObject.setEntityID(entityID);
        wrapperObject.setType(71);
        wrapperObject.setX(location.getX());
        wrapperObject.setY(location.getY());
        wrapperObject.setZ(location.getZ());
        wrapperObject.setObjectData(EnumDirection.fromType2(compound.getByte("Facing")).get2DRotationValue());

        NBTTagList nbttaglist2 = compound.getList("Rotation", 5);
        wrapperObject.setYaw(nbttaglist2.g(0));
        wrapperObject.setPitch(nbttaglist2.g(1));
        wrapperObject.sendPacket(pl);

        /*EntityItemFrame entity = new EntityItemFrame(((CraftWorld)pl.getWorld()).getHandle());
        entity.f(compound);
        entity.a(compound);*/

        /*NBTTagCompound nbttagcompound = compound.getCompound("Item");
        DataWatcher dataWatcher = new DataWatcher(null);
        dataWatcher.register(new DataWatcherObject<>(6, DataWatcherRegistry.f), new ItemStack(nbttagcompound));
        PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(entityID, dataWatcher, true);
        ((CraftPlayer)pl).getHandle().playerConnection.sendPacket(packet);*/

        WrapperPlayServerEntityMetadata wrapperMetaData = new WrapperPlayServerEntityMetadata();
        wrapperMetaData.setEntityID(entityID);
        NBTTagCompound nbttagcompound = compound.getCompound("Item");
        //List<WrappedWatchableObject> metadata = new ArrayList<>();
        WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
        dataWatcher.setObject(6, WrappedDataWatcher.Registry.getItemStackSerializer(false), new ItemStack(nbttagcompound));
        /*WrappedWatchableObject obj = new WrappedWatchableObject(6, new ItemStack(nbttagcompound));
        obj.setValue(new ItemStack(nbttagcompound));
        Bukkit.getLogger().info("Index: " + obj.getIndex());
        metadata.add(obj);
        metadata.add(new WrappedWatchableObject(0xff, null));*/
        wrapperMetaData.setMetadata(dataWatcher.getWatchableObjects());
        wrapperMetaData.sendPacket(pl);

        Bukkit.getLogger().info("send function end");
        long finish = System.nanoTime();
        Bukkit.getLogger().info("Speed of sending item frame (ms): " + (finish - start));
    }

    private void serializePaintingsFromChunk(World world, int chunkX, int chunkZ, DataOutput destination) throws IOException {
        for (Painting painting : world.getEntitiesByClass(Painting.class)) {
                int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                if (paintingChunkX == chunkX && paintingChunkZ == chunkZ) {
                    NbtTagCompoundSerializer.serialize(destination, painting, Painting.class);
                }
        }
    }

    private void serializeItemFramesFromChunk(World world, int chunkX, int chunkZ, DataOutput destination) throws IOException {
        for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
            int itemFrameChunkX = (int) (itemFrame.getLocation().getX()/CHUNK_WIDTH);
            int itemFrameChunkZ = (int) (itemFrame.getLocation().getZ()/CHUNK_WIDTH);
            if (itemFrameChunkX == chunkX && itemFrameChunkZ == chunkZ) {
                NbtTagCompoundSerializer.serialize(destination, itemFrame, ItemFrame.class);
            }
        }
    }

    private List<NBTTagCompound> deserializeEntities(InputStream source) throws IOException, MojangsonParseException {
        List<NBTTagCompound> entitiesNBT = new ArrayList<>();
        DataInputStream in = new DataInputStream(source);
        while (source.available() > 0) {
            entitiesNBT.add(NbtTagCompoundSerializer.deserialize(in));
        }
        return entitiesNBT;
    }

    public static int getEntityCount() {
        return entityCount;
    }
}
