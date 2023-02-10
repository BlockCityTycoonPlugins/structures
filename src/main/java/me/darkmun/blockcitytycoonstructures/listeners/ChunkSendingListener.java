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
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.ItemFrame;
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

import static net.minecraft.server.v1_12_R1.BlockFurnace.FACING;

public class ChunkSendingListener extends PacketAdapter {

    public static final int CHUNK_WIDTH = 16;
    private static final int FOUNDRY_CHUNK_X = -5;
    private static final int FOUNDRY_CHUNK_Z = 16;
    private static int entityCount = 0;
    private static final FileConfiguration BCTSConfig = BlockCityTycoonStructures.getPlugin().getConfig();
    private static final CustomConfig BCTFPlayersFurnacesDataConfig = BlockCityTycoonStructures.getBCTFPlayersFurnacesDataConfig();
    private static final CustomConfig BCTEPlayersEventsDataConfig = BlockCityTycoonStructures.getBCTEPlayersEventsDataConfig();
    private static final FileConfiguration BCTFConfig = Bukkit.getPluginManager().getPlugin("BlockCityTycoonFoundry").getConfig();
    private static final FileConfiguration BCTEConfig = Bukkit.getPluginManager().getPlugin("BlockCityTycoonEvents").getConfig();
    List<BaseBlockPosition> ritualBlocksCoords = new ArrayList<>();

    public ChunkSendingListener(Plugin plugin, PacketType... types) {
        super(plugin, types);

        Set<String> blockNames = BCTEConfig.getConfigurationSection("rain-event.ritual-blocks-coord").getKeys(false);
        for (String name : blockNames) {
            int x = BCTEConfig.getInt("rain-event.ritual-blocks-coord." + name + ".x");
            int y = BCTEConfig.getInt("rain-event.ritual-blocks-coord." + name + ".y");
            int z = BCTEConfig.getInt("rain-event.ritual-blocks-coord." + name + ".z");
            ritualBlocksCoords.add(new BaseBlockPosition(x, y, z));
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        WrapperPlayServerMapChunk wrapper = new WrapperPlayServerMapChunk(packet);

        Player pl = event.getPlayer();
        int ChunkX = packet.getIntegers().read(0);
        int ChunkZ = packet.getIntegers().read(1);
        fillConfigWithChunkData(wrapper, ChunkX, ChunkZ, pl.getWorld());

        try {
            changeChunkToPlayer(BlockCityTycoonStructures.getPlayerUpgradesConfig(), wrapper, ChunkX, ChunkZ, pl);
        } catch (IOException | NoSuchFieldException | InvocationTargetException | IllegalAccessException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void fillConfigWithChunkData(WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ, World world) {
        Set<String> businessesChunks = BCTSConfig.getKeys(false);
        businessesChunks.remove("enable");
        Set<String> businessChunkUpgrades;
        Set<String> businessChunkUpgradeChunks;
        byte[] data;
        int bitMask;
        boolean groundUp;
        List<NbtBase<?>> tileEntities;

        for (String businessChunk : businessesChunks) {
            businessChunkUpgrades = BCTSConfig.getConfigurationSection(businessChunk).getKeys(false);
            for (String businessChunkUpgrade : businessChunkUpgrades) {
                businessChunkUpgradeChunks = BCTSConfig.getConfigurationSection(businessChunk + "." + businessChunkUpgrade).getKeys(false);
                for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                    int businessUpgradeChunkX = BCTSConfig.getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                    int businessUpgradeChunkZ = BCTSConfig.getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                    if ((businessUpgradeChunkX == ChunkX) && (businessUpgradeChunkZ == ChunkZ) /*&& !chunkDataConfig.getConfig().contains(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk)*/) {
                        data = packet.getData();
                        bitMask = packet.getBitmask();
                        groundUp = packet.getGroundUpContinuous();
                        tileEntities = packet.getTileEntities();

                        try(PreparedStatement statement = BlockCityTycoonStructures.getDatabase().getConnection()
                                .prepareStatement("INSERT INTO chunk_data (structure,upgrade,chunk,data,bit_mask,ground_up_continuous,tile_entities,paintings,item_frames) " +
                                        "SELECT * FROM (SELECT ? AS structure, ? AS upgrade, ? AS chunk, ? AS data, ? AS bit_mask, ? AS ground_up_continuous, ? AS tile_entities, ? AS paintings, ? AS item_frames) AS temp " +
                                        "WHERE NOT EXISTS ( " +
                                        "    SELECT structure, upgrade, chunk FROM chunk_data WHERE structure = ? AND upgrade = ? AND chunk = ? " +
                                        ") LIMIT 1")) {
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
                            statement.setBytes(8, baosPaintings.toByteArray());

                            ByteArrayOutputStream baosItemFrames = new ByteArrayOutputStream();
                            DataOutputStream outItemFrames = new DataOutputStream(baosItemFrames);
                            serializeItemFramesFromChunk(world, businessUpgradeChunkX, businessUpgradeChunkZ, outItemFrames);
                            statement.setBytes(9, baosItemFrames.toByteArray());

                            statement.setString(10, businessChunk);
                            statement.setString(11, businessChunkUpgrade);
                            statement.setString(12, businessChunkUpgradeChunk);

                            statement.executeUpdate();
                        } catch (SQLException | IOException ex) {
                            ex.printStackTrace();
                        }
                        return;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void changeChunkToPlayer(CustomConfig playerUpgradesConfig, WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ, Player player) throws IOException, NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Set<String> businesses = BCTSConfig.getKeys(false);
        Set<String> businessChunkUpgradeChunks;
        String businessValue;
        String plUUID = player.getUniqueId().toString();

        byte[] data;
        int bitMask;
        boolean groundUp;

        int pasteChunkX;
        int pasteChunkZ;

        BaseBlockPosition ritualBlockPositionInChunk = ritualBlocksCoords.stream().filter(blockPos ->
                (blockPos.getX() < 0 ? (blockPos.getX() + 1)/CHUNK_WIDTH - 1 : blockPos.getX()/CHUNK_WIDTH) == ChunkX
                        && (blockPos.getZ() < 0 ? (blockPos.getZ() + 1)/CHUNK_WIDTH - 1 : blockPos.getZ()/CHUNK_WIDTH) == ChunkZ)
                .findAny().orElse(null);
        if (ritualBlockPositionInChunk != null) {
            Chunk chunk;
            if (packet.getGroundUpContinuous()) {
                chunk = new Chunk(((CraftPlayer)player).getHandle().getWorld(), ChunkX, ChunkZ);
            } else {
                chunk = ((CraftPlayer)player).getHandle().getWorld().getChunkAt(ChunkX, ChunkZ);
            }
            ChunkDataSerializer chunkDataSerializer = new ChunkDataSerializer();
            chunkDataSerializer.ReadChunkColumn(chunk, packet.getBitmask(), Unpooled.wrappedBuffer(packet.getData()));

            setPlayerRitualBlockToChunk(plUUID, ritualBlockPositionInChunk, chunk);
            PacketPlayOutMapChunk packetMapChunk = new PacketPlayOutMapChunk(chunk, 65535);

            byte[] bytes;
            Field dataField;
            try {
                dataField = packetMapChunk.getClass().getDeclaredField("d");
                dataField.setAccessible(true);
                bytes = (byte[])dataField.get(packetMapChunk);
                dataField.setAccessible(false);

                /*WrapperPlayServerUnloadChunk wrapperUnloadChunk = new WrapperPlayServerUnloadChunk();
                wrapperUnloadChunk.setChunkX(ChunkX);
                wrapperUnloadChunk.setChunkZ(ChunkZ);
                wrapperUnloadChunk.sendPacket(player);*/

                packet.setData(bytes);
            } catch (NoSuchFieldException | SecurityException | IllegalAccessException e){
                e.printStackTrace();
            }
        } else {
            for (String business : businesses) {
                if (playerUpgradesConfig.getConfig().contains(plUUID + "." + business)) {
                    businessValue = playerUpgradesConfig.getConfig().getString(plUUID + "." + business);
                    //if (chunkDataConfig.getConfig().contains(business + "." + businessValue)) {
                    businessChunkUpgradeChunks = BCTSConfig.getConfigurationSection(business + "." + businessValue).getKeys(false);
                    for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                        pasteChunkX = BCTSConfig.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                        pasteChunkZ = BCTSConfig.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                        if ((ChunkX == pasteChunkX) && (ChunkZ == pasteChunkZ)) {
                            try (PreparedStatement statement = BlockCityTycoonStructures.getDatabase().getConnection().prepareStatement("SELECT * FROM chunk_data WHERE structure = ? AND upgrade = ? AND chunk = ?")) {
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

                                    int copyChunkX = BCTSConfig.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                                    int copyChunkZ = BCTSConfig.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                                    int chunkXDelta = pasteChunkX - copyChunkX;
                                    int chunkZDelta = pasteChunkZ - copyChunkZ;

                                    for (NbtBase<?> tileEntity : tileEntities) {
                                        Map<String, NbtBase<?>> map = (Map<String, NbtBase<?>>) tileEntity.getValue();
                                        int x = (int) map.get("x").getValue();
                                        int z = (int) map.get("z").getValue();
                                        map.put("x", NbtFactory.of("x", x + CHUNK_WIDTH * chunkXDelta));
                                        map.put("z", NbtFactory.of("z", z + CHUNK_WIDTH * chunkZDelta));

                                    }

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
                                    for (NBTTagCompound itemFrame : itemFrames) {
                                        NBTTagList nbttaglist = itemFrame.getList("Pos", 6);
                                        double paintingX = nbttaglist.f(0);
                                        double paintingY = nbttaglist.f(1);
                                        double paintingZ = nbttaglist.f(2);
                                        Location newLocation = new Location(player.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta);
                                        sendItemFrameFromNBT(itemFrame, newLocation, player);
                                    }

                                    Chunk chunk;
                                    if (groundUp) {
                                        chunk = new Chunk(((CraftPlayer)player).getHandle().getWorld(), ChunkX, ChunkZ);
                                    } else {
                                        chunk = ((CraftPlayer)player).getHandle().getWorld().getChunkAt(ChunkX, ChunkZ);
                                    }
                                    ChunkDataSerializer chunkDataSerializer = new ChunkDataSerializer();
                                    chunkDataSerializer.ReadChunkColumn(chunk, bitMask, Unpooled.wrappedBuffer(data));
                                    if (ChunkX == FOUNDRY_CHUNK_X && ChunkZ == FOUNDRY_CHUNK_Z) {
                                        setPlayersFoundryFurnacesToChunk(plUUID, chunk);
                                    }

                                    PacketPlayOutMapChunk packetMapChunk = new PacketPlayOutMapChunk(chunk, 65535);

                                    byte[] bytes;
                                    Field dataField;
                                    try {
                                        dataField = packetMapChunk.getClass().getDeclaredField("d");
                                        dataField.setAccessible(true);
                                        bytes = (byte[])dataField.get(packetMapChunk);
                                        dataField.setAccessible(false);

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

                            return;
                        }
                    }
                }
            }
        }
    }

    private void sendPaintingFromNBT(NBTTagCompound compound, Location location, Player pl) {
        WrapperPlayServerSpawnEntityPainting wrapper = new WrapperPlayServerSpawnEntityPainting();
        if (entityCount == Integer.MIN_VALUE) {
            entityCount = 0;
        }
        wrapper.setEntityID(--entityCount);
        wrapper.getHandle().getStrings().write(0, compound.getString("Motive"));
        wrapper.setDirection(EnumWrappers.Direction.valueOf(EnumDirection.fromType2(compound.getByte("Facing")).toString().toUpperCase()));
        wrapper.setLocation(new BlockPosition(location.toVector()));
        wrapper.sendPacket(pl);
    }

    private void sendItemFrameFromNBT(NBTTagCompound compound, Location location, Player pl) {
        if (entityCount == Integer.MIN_VALUE) {
            entityCount = 0;
        }
        int entityID = --entityCount;

        //Здесь создаем рамку
        NBTTagList rotationNBT = compound.getList("Rotation", 5);

        WrapperPlayServerSpawnEntity wrapperObject = new WrapperPlayServerSpawnEntity();
        wrapperObject.setEntityID(entityID);
        wrapperObject.setType(71);
        wrapperObject.setX(location.getX());
        wrapperObject.setY(location.getY());
        wrapperObject.setZ(location.getZ());
        wrapperObject.setObjectData(EnumDirection.fromType2(compound.getByte("Facing")).get2DRotationValue());
        wrapperObject.setYaw(rotationNBT.g(0));
        wrapperObject.setPitch(rotationNBT.g(1));
        wrapperObject.sendPacket(pl);

        //А здесь "кладем" в рамку предмет
        NBTTagCompound itemNBT = compound.getCompound("Item");

        WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
        dataWatcher.setObject(6, WrappedDataWatcher.Registry.getItemStackSerializer(false), new ItemStack(itemNBT));
        List<WrappedWatchableObject> metadata = dataWatcher.getWatchableObjects();

        WrapperPlayServerEntityMetadata wrapperMetaData = new WrapperPlayServerEntityMetadata();
        wrapperMetaData.setEntityID(entityID);
        wrapperMetaData.setMetadata(metadata);
        wrapperMetaData.sendPacket(pl);
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

    private void setPlayersFoundryFurnacesToChunk(String plUUID, Chunk chunk) {
        BCTFPlayersFurnacesDataConfig.reloadConfig();
        if (BCTFPlayersFurnacesDataConfig.getConfig().contains(plUUID)) {
            Set<String> playersFurnaces = BCTFPlayersFurnacesDataConfig.getConfig().getConfigurationSection(String.format("%s.furnaces", plUUID)).getKeys(false);
            for (String onceBoughtFurnace : playersFurnaces) {
                String state = BCTFPlayersFurnacesDataConfig.getConfig().getString(String.format("%s.furnaces.%s.state", plUUID, onceBoughtFurnace));
                int globalX = BCTFConfig.getInt(String.format("furnaces.%s.x", onceBoughtFurnace));
                int globalY = BCTFConfig.getInt(String.format("furnaces.%s.y", onceBoughtFurnace));
                int globalZ = BCTFConfig.getInt(String.format("furnaces.%s.z", onceBoughtFurnace));
                String facing = BCTFConfig.getString(String.format("furnaces.%s.facing", onceBoughtFurnace)).toUpperCase();

                IBlockData blockData;
                if (state.equals("PLACED_EMPTY") || state.equals("PLACED_MELTED") || state.equals("PLACED_MELTING")) {
                    blockData = Blocks.FURNACE.getBlockData().set(FACING, EnumDirection.valueOf(facing));
                } /*else if (state.equals("PLACED_MELTING")) {
                    blockData = Blocks.LIT_FURNACE.getBlockData().set(FACING, EnumDirection.valueOf(facing));
                }*/ else {
                    blockData = Blocks.AIR.getBlockData();
                }

                int chunkSectionNum = globalY/16;
                int x = getXInChunk(globalX);
                int y = getYInChunk(globalY);
                int z = getZInChunk(globalZ);
                chunk.getSections()[chunkSectionNum].setType(x, y, z, blockData);
            }
        }
    }

    private void setPlayerRitualBlockToChunk(String plUUID, BaseBlockPosition ritualBlockPos, Chunk chunk) {
        BCTEPlayersEventsDataConfig.reloadConfig();
        //CustomConfig BCTEPlayersEventsDataConfig = BlockCityTycoonStructures.getBCTEPlayersEventsDataConfig();
        if (BCTEPlayersEventsDataConfig.getConfig().contains(String.format("%s.rain-event.ritual-blocks", plUUID))) {
            Set<String> playersRitualBlocks = BCTEPlayersEventsDataConfig.getConfig().getConfigurationSection(String.format("%s.rain-event.ritual-blocks", plUUID)).getKeys(false);
            for (String ritualBlock : playersRitualBlocks) {
                if (BCTEPlayersEventsDataConfig.getConfig().getBoolean(String.format("%s.rain-event.ritual-blocks.%s.placed", plUUID, ritualBlock))) {
                    int globalX = BCTEConfig.getInt("rain-event.ritual-blocks-coord." + ritualBlock + ".x");
                    int globalY = BCTEConfig.getInt("rain-event.ritual-blocks-coord." + ritualBlock + ".y");
                    int globalZ = BCTEConfig.getInt("rain-event.ritual-blocks-coord." + ritualBlock + ".z");
                    if (ritualBlockPos.equals(new BaseBlockPosition(globalX, globalY, globalZ))) {
                        IBlockData blockData = Block.getById(239).getBlockData();
                        int chunkSectionNum = globalY/CHUNK_WIDTH;
                        int x = getXInChunk(globalX);
                        int y = getYInChunk(globalY);
                        int z = getZInChunk(globalZ);
                        chunk.getSections()[chunkSectionNum].setType(x, y, z, blockData);
                        return;
                    }
                }
            }
        }
    }

    public static int getXInChunk(int x) {
        if (x < 0) {
            return CHUNK_WIDTH + x % CHUNK_WIDTH;
        } else {
            return x % CHUNK_WIDTH;
        }
    }
    public static int getYInChunk(int y) {
        return y % CHUNK_WIDTH;
    }
    public static int getZInChunk(int z) {
        if (z < 0) {
            return CHUNK_WIDTH + z % CHUNK_WIDTH;
        } else {
            return z % CHUNK_WIDTH;
        }
    }

    @SuppressWarnings("unused")
    public static int getEntityCount() {
        return entityCount;
    }
}
